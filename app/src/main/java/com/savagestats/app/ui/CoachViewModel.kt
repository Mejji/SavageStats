package com.savagestats.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.savagestats.app.ai.LlmInferenceManager
import com.savagestats.app.data.DailyLog
import com.savagestats.app.data.LogRepository
import com.savagestats.app.data.Mission
import com.savagestats.app.data.MissionTask
import com.savagestats.app.data.UserProfile
import com.savagestats.app.data.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoachViewModel(
    private val repository: LogRepository,
    private val profileManager: UserProfileManager,
    private val llmManager: LlmInferenceManager
) : ViewModel() {

    val recentLogs: StateFlow<List<DailyLog>> = repository
        .getLastSevenDaysLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userProfile: StateFlow<UserProfile> = profileManager.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val modelStatus: StateFlow<LlmInferenceManager.ModelLoadStatus> = llmManager.loadStatus

    val failedMissionsCount: StateFlow<Int> = repository
        .getFailedMissionsCount(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isGeneratingWorkout = MutableStateFlow(false)
    val isGeneratingWorkout: StateFlow<Boolean> = _isGeneratingWorkout.asStateFlow()

    private val _coachResponse = MutableStateFlow<String?>(null)
    val coachResponse: StateFlow<String?> = _coachResponse.asStateFlow()

    // Parsed coach verdict sections
    private val _verdictText = MutableStateFlow("")
    val verdictText: StateFlow<String> = _verdictText.asStateFlow()

    private val _strategyText = MutableStateFlow("")
    val strategyText: StateFlow<String> = _strategyText.asStateFlow()

    private val _workoutText = MutableStateFlow("")
    val workoutText: StateFlow<String> = _workoutText.asStateFlow()

    private val _workoutSuggestion = MutableStateFlow<String?>(null)
    val workoutSuggestion: StateFlow<String?> = _workoutSuggestion.asStateFlow()

    private val _workoutSuggestionItems = MutableStateFlow<List<String>>(emptyList())
    val workoutSuggestionItems: StateFlow<List<String>> = _workoutSuggestionItems.asStateFlow()

    private val _missionAccepted = MutableStateFlow(false)
    val missionAccepted: StateFlow<Boolean> = _missionAccepted.asStateFlow()

    private val _showModelCard = MutableStateFlow(true)
    val showModelCard: StateFlow<Boolean> = _showModelCard.asStateFlow()

    // Stored so parseCoachResponse can prepend it to the LLM continuation
    private var _lastOpeningSentence = ""

    fun roastMyWeek() {
        val logs = recentLogs.value
        if (logs.isEmpty()) {
            _verdictText.value = "No logs found. You can't even be roasted — you've done NOTHING."
            _strategyText.value = ""
            _workoutText.value = ""
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _coachResponse.value = null
            _verdictText.value = ""
            _strategyText.value = ""
            _workoutText.value = ""

            val prompt = formatPrompt(logs, userProfile.value)
            // Extract the data-driven opening sentence we wrote so we can
            // prepend it to Gemma's continuation (which omits it).
            _lastOpeningSentence = prompt.substringBefore(" Week:")
            val response = llmManager.generateSavageResponse(prompt)
            parseCoachResponse(response)
            _isGenerating.value = false
        }
    }

    private fun parseCoachResponse(rawResponse: String) {
        val cleaned = cleanResponse(rawResponse)
            .trimStart('"', ' ')
            .trimEnd('"', ' ')

        // Combine our data-driven opening + Gemma's savage continuation
        val fullRoast = if (cleaned.isNotBlank()) {
            "$_lastOpeningSentence $cleaned"
        } else {
            _lastOpeningSentence.ifBlank {
                "Your data is so boring even the AI had nothing to say. Log something worth roasting."
            }
        }

        _verdictText.value = fullRoast.trim()
        _strategyText.value = ""
        _workoutText.value = ""
    }

    fun generateWorkoutSuggestion() {
        val logs = recentLogs.value

        viewModelScope.launch {
            _isGeneratingWorkout.value = true
            _workoutSuggestion.value = null
            _workoutSuggestionItems.value = emptyList()
            _missionAccepted.value = false

            val prompt = formatWorkoutPrompt(
                logs = logs.take(3),
                profile = userProfile.value
            )
            val rawResponse = llmManager.generateSavageResponse(prompt)
            val cleaned = cleanResponse(rawResponse)
            _workoutSuggestion.value = cleaned
            _workoutSuggestionItems.value = parseWorkoutItems(cleaned)
            _isGeneratingWorkout.value = false
        }
    }

    fun acceptMission() {
        val items = _workoutSuggestionItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            val tasks = items.map { MissionTask(name = it) }
            val mission = Mission(
                title = "Savage Mission",
                tasks = tasks,
                expiresAt = System.currentTimeMillis() + 86_400_000L, // 24 hours
            )
            repository.insertMission(mission)
            _missionAccepted.value = true
            _workoutSuggestion.value = null
            _workoutSuggestionItems.value = emptyList()
        }
    }

    fun clearMissionAccepted() {
        _missionAccepted.value = false
    }

    fun clearWorkoutSuggestion() {
        _workoutSuggestion.value = null
        _workoutSuggestionItems.value = emptyList()
    }

    fun hideModelCard() {
        _showModelCard.value = false
    }

    fun reinitializeModel(context: android.content.Context) {
        llmManager.initialize(context)
    }

    private fun formatPrompt(logs: List<DailyLog>, profile: UserProfile): String {
        val todayLog = logs.first()
        val bmr = com.savagestats.app.ai.NutritionCalculator.calculateBMR(
            profile.weight, profile.height, profile.age
        )
        val targetCals    = com.savagestats.app.ai.NutritionCalculator
            .calculateDailyCalories(bmr, goal = profile.goal).toInt()
        val targetProtein = (profile.weight * 2.2f).toInt()
        val currentCals   = ((todayLog.protein * 4f) + (todayLog.carbs * 4f) + (todayLog.fats * 9f)).toInt()
        val currentProtein = todayLog.protein.toInt()
        val proteinDelta  = currentProtein - targetProtein
        val calDelta      = currentCals - targetCals

        // Weekly summary for context
        val weekSummary = logs.take(7).joinToString("; ") { l ->
            "${l.date}: ${l.protein.toInt()}g P, ${l.activityType} ${l.activityDurationMinutes}min"
        }

        // ── Build the first sentence ourselves based on the data ────────────
        // Gemma will CONTINUE from this sentence — no room to preamble.
        val openingSentence = when {
            proteinDelta < -30 && calDelta > 200 ->
                "You ate ${currentCals}kcal but only got ${currentProtein}g protein out of ${targetProtein}g — all carbs and fat, zero discipline."
            proteinDelta < -30 ->
                "You missed your protein by ${-proteinDelta}g — your muscles are literally starving while your goal says ${profile.goal}."
            calDelta > 300 ->
                "You went ${calDelta}kcal over your limit eating ${currentCals}kcal — that's not ${profile.goal}, that's a cheat day disguised as effort."
            calDelta < -400 ->
                "You only ate ${currentCals} out of ${targetCals}kcal — undereating won't get you to ${profile.goal}, it'll get you to the hospital."
            proteinDelta >= 0 && kotlin.math.abs(calDelta) <= 200 ->
                "Okay, ${currentProtein}g protein and ${currentCals}kcal is actually respectable for ${profile.goal} — don't let it go to your head."
            else ->
                "You logged ${currentCals}kcal and ${currentProtein}g protein against targets of ${targetCals}/${targetProtein}g for ${profile.goal}."
        }

        // Gemma continues from our sentence: adds 1-3 more savage lines + advice.
        return "$openingSentence Week: $weekSummary. Continue roasting in 1-3 more short sentences, then give one sentence of real advice. Do not repeat what was already said.\n"
    }

    private fun formatWorkoutPrompt(logs: List<DailyLog>, profile: UserProfile): String {
        val todayLog = logs.firstOrNull()
        val goal = profile.goal.lowercase()
        val bmr = com.savagestats.app.ai.NutritionCalculator.calculateBMR(
            profile.weight, profile.height, profile.age
        )
        val targetCals = com.savagestats.app.ai.NutritionCalculator
            .calculateDailyCalories(bmr, goal = profile.goal)
        val targetProtein = profile.weight * 2.2f
        val todayCals = todayLog?.let { (it.protein * 4f) + (it.carbs * 4f) + (it.fats * 9f) } ?: 0f
        val todayProtein = todayLog?.protein ?: 0f
        val wellFuelled = (targetCals > 0f && todayCals / targetCals >= 0.8f) &&
                          (targetProtein > 0f && todayProtein / targetProtein >= 0.8f)
        val lastActivity = todayLog?.activityType?.lowercase() ?: "rest"

        // Intensity: well-fuelled → heavy compound sets, under-fuelled → scaled back
        val intensity = if (wellFuelled) "heavy" else "moderate"
        val lastWeek   = logs.take(7).joinToString(", ") { "${it.date}: ${it.activityType}" }

        // Explicit format instruction - no preamble, just numbered exercises
        return """
            Generate exactly 4 workout exercises for a ${profile.age} year old, ${profile.weight}kg athlete with goal: ${profile.goal}.
            Intensity level: $intensity.
            Recent activities: $lastWeek.
            
            Format MUST be:
            1. ExerciseName: 3 sets of 10 reps
            2. ExerciseName: 4 sets of 8 reps
            3. ExerciseName: 3 sets of 12 reps
            4. ExerciseName: 5 sets of 5 reps
            
            Start now with "1. " and nothing else.
        """.trimIndent()
    }

    /** Strip model filler/disclaimer text and keep direct output only. */
    private fun cleanResponse(rawAiResponse: String): String {
        val raw = rawAiResponse.trim().replace("**", "")
        val introLineRegex = Regex(
            pattern = "(?i)^\\s*(here(?:'|')??s your (?:roast|prompt|workout|plan|verdict)|today(?:'|')??s (?:orders|workout|plan)|i(?:'|')??ll reply as|i will (?:reply as|act as)|sure,? i can|okay,? i can|understood\\.?|system:|workout plan:)"
        )

        val lines = raw.lines().toMutableList()
        var dropped = 0
        while (lines.isNotEmpty() && dropped < 2) {
            if (introLineRegex.containsMatchIn(lines.first())) {
                lines.removeAt(0)
                dropped++
            } else {
                break
            }
        }

        val cleaned = lines.joinToString("\n").trim()
            .replaceFirst(Regex("^\\s*(Sure,? I can[^.!?]*[.!?]?\\s*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^\\s*(Okay,? I can[^.!?]*[.!?]?\\s*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^\\s*(Here is your (?:roast|workout|prompt)[:\\-]?\\s*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^\\s*(I(?:'|')??ll reply as[^.!?]*[.!?]?\\s*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^\\s*(I will act as[^.!?]*[.!?]?\\s*)", RegexOption.IGNORE_CASE), "")
            // Strip "Sure, here's your workout:" style headers that have 1. on the same line
            .replaceFirst(
                Regex("(?i)^\\s*(sure[, ]?(?:here'?s?|here is|below)[^:\\d]*:\\s*)", RegexOption.IGNORE_CASE), ""
            )
            // If the first remaining line starts with a number but has preamble text before it, strip the preamble
            .replaceFirst(
                Regex("(?i)^\\s*((?:here'?s|here is|sure|okay|below)[^\\d]*)\\s*(\\d+\\.)", RegexOption.IGNORE_CASE),
                "$2"
            )
            .trim()

        return if (cleaned.length >= 6) cleaned else raw
    }

    private fun parseWorkoutItems(raw: String): List<String> {
        val results = mutableListOf<String>()
        val lines = raw.lines()
        
        // Try multiple parsing strategies
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            // Strategy 1: Match numbered pattern "1. Exercise Name: X sets of Y reps"
            val numberedMatch = Regex("^\\s*(\\d+)[.)\\-\\:]\\s*(.+)$").find(trimmed)
            if (numberedMatch != null) {
                val exercise = numberedMatch.groupValues[2].trim()
                if (exercise.isNotEmpty() && !results.contains(exercise)) {
                    results.add(exercise)
                }
                if (results.size >= 4) break
                continue
            }
            
            // Strategy 2: Match colon-separated "Exercise Name: X sets of Y reps"
            val colonMatch = Regex("^([A-Za-z][A-Za-z\\s]+):\\s*(.+)$").find(trimmed)
            if (colonMatch != null && results.size < 4) {
                val exercise = trimmed
                if (exercise.isNotEmpty() && !results.contains(exercise)) {
                    results.add(exercise)
                }
                if (results.size >= 4) break
            }
        }
        
        // Strategy 3: If we got partial results, try to extract any exercise patterns
        if (results.size < 2) {
            val exercisePatterns = listOf(
                Regex("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s*:\\s*(\\d+)\\s*(?:x|×|sets?)\\s*(\\d+)", RegexOption.IGNORE_CASE),
                Regex("(\\d+)\\s*(?:x|×)\\s*(\\d+)\\s*(?:reps?|sets?)\\s*[-–]\\s*([A-Za-z][A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
            )
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                
                for (pattern in exercisePatterns) {
                    val match = pattern.find(trimmed)
                    if (match != null) {
                        val exercise = match.groupValues[0].trim()
                        if (exercise.length > 5 && !results.contains(exercise)) {
                            results.add(exercise)
                        }
                        if (results.size >= 4) break
                    }
                }
                if (results.size >= 4) break
            }
        }
        
        // If still no results, just return the cleaned lines as-is (up to 4)
        if (results.isEmpty()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && trimmed.length > 5) {
                    results.add(trimmed)
                }
                if (results.size >= 4) break
            }
        }
        
        return results.take(4)
    }

    private fun calculateRank(currentWeight: Float, targetWeight: Float, xp: Int): String {
        val weightGoalMet = targetWeight > 0f && currentWeight > 0f &&
                kotlin.math.abs(currentWeight - targetWeight) <= 2f

        return when {
            weightGoalMet && xp >= 1000 -> "Savage God"
            xp >= 600 -> "Local Threat"
            xp >= 300 -> "Iron Novice"
            xp >= 100 -> "Couch Predator"
            else -> "Uncooked Noodle"
        }
    }

    class Factory(
        private val repository: LogRepository,
        private val profileManager: UserProfileManager,
        private val llmManager: LlmInferenceManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CoachViewModel::class.java)) {
                return CoachViewModel(repository, profileManager, llmManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private val DailyLog.activityDuration: Float
    get() = activityDurationMinutes
