package com.example.savagestats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.savagestats.ai.LlmInferenceManager
import com.example.savagestats.data.DailyLog
import com.example.savagestats.data.LogRepository
import com.example.savagestats.data.UserProfile
import com.example.savagestats.data.UserProfileManager
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

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isGeneratingWorkout = MutableStateFlow(false)
    val isGeneratingWorkout: StateFlow<Boolean> = _isGeneratingWorkout.asStateFlow()

    private val _coachResponse = MutableStateFlow<String?>(null)
    val coachResponse: StateFlow<String?> = _coachResponse.asStateFlow()

    private val _workoutSuggestion = MutableStateFlow<String?>(null)
    val workoutSuggestion: StateFlow<String?> = _workoutSuggestion.asStateFlow()

    fun roastMyWeek() {
        val logs = recentLogs.value
        if (logs.isEmpty()) {
            _coachResponse.value = "No logs found. You can't even be roasted — you've done NOTHING."
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _coachResponse.value = null

            val prompt = formatPrompt(logs, userProfile.value)
            val response = llmManager.generateSavageResponse(prompt)

            _coachResponse.value = cleanResponse(response)
            _isGenerating.value = false
        }
    }

    fun generateWorkoutSuggestion() {
        val logs = recentLogs.value

        viewModelScope.launch {
            _isGeneratingWorkout.value = true
            _workoutSuggestion.value = null

            val prompt = formatWorkoutPrompt(
                logs = logs.take(3),
                profile = userProfile.value
            )
            val response = llmManager.generateSavageResponse(prompt)

            _workoutSuggestion.value = cleanResponse(response)
            _isGeneratingWorkout.value = false
        }
    }

    private fun formatPrompt(logs: List<DailyLog>, profile: UserProfile): String {
        val formattedLogs = logs.joinToString(separator = "\n") { log ->
            "${log.date}: ${log.proteinGrams}g protein, ${log.activityType} for ${log.activityDurationMinutes} min, ${log.sleepHours}h sleep"
        }

        val user = profile
        val prompt = """
            System: You are a savage, uncompromising fitness coach. Provide a single, brutal, 3-sentence performance review directly to the user based on their data.
            User Profile: ${user.age} years old, ${user.weight}kg, Goal: ${user.goal}.
            Rules:
            1. DO NOT ask any questions. DO NOT wait for a reply.
            2. DO NOT write narrative text, actions, or dialogue tags (e.g., do not write "You smile").
            3. Speak directly to the user using "You".
            4. Be internally consistent: If protein is high (over 120g) and they worked out, praise them aggressively. If it's low or they are lazy, roast them without mercy.

            Data to review:
            $formattedLogs

            Coach Verdict:
        """.trimIndent()

        return prompt
    }

    private fun formatWorkoutPrompt(logs: List<DailyLog>, profile: UserProfile): String {
        val formattedLogs = if (logs.isEmpty()) {
            "No recent logs available."
        } else {
            logs.joinToString(separator = "\n") { log ->
                "${log.date}: ${log.proteinGrams}g protein, ${log.activityType} for ${log.activityDurationMinutes} min, ${log.sleepHours}h sleep"
            }
        }

        val user = profile
        return """
            System: You are a tough, expert fitness coach. Based on the user's profile and recent logs, suggest a highly specific, brutal, but effective workout for TODAY.
            User Profile: ${user.age} years old, ${user.weight}kg. Goal: ${user.goal}.
            Rules:
            1. DO NOT ask questions. DO NOT write narrative text.
            2. Give exactly 3 actionable bullet points for the workout (e.g., exercises, sets, reps, or distance).
            3. If they worked out hard yesterday, suggest active recovery or target a different muscle group.
            4. Keep the tone aggressive and motivating.

            Recent Logs:
            $formattedLogs

            Today's Workout Plan:
        """.trimIndent()
    }

    /** Strip any residual markdown bold markers from model output. */
    private fun cleanResponse(raw: String): String =
        raw.replace("**", "").trim()

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
