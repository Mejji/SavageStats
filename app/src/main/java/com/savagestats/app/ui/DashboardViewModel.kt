package com.savagestats.app.ui

import androidx.activity.result.ActivityResultLauncher
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.savagestats.app.ai.LlmInferenceManager
import com.savagestats.app.ai.NutritionCalculator
import com.savagestats.app.data.CustomFoodDao
import com.savagestats.app.data.CustomFoodItem
import com.savagestats.app.data.toFoodItem
import com.savagestats.app.data.DailyLog
import com.savagestats.app.data.HealthConnectManager
import com.savagestats.app.data.LogRepository
import com.savagestats.app.data.UserProfile
import com.savagestats.app.data.UserProfileManager
import com.savagestats.app.data.nutrition.FoodDao
import com.savagestats.app.data.nutrition.FoodItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(FlowPreview::class)
class DashboardViewModel(
    private val repository: LogRepository,
    private val healthConnectManager: HealthConnectManager,
    private val profileManager: UserProfileManager,
    private val llmManager: LlmInferenceManager,
    private val foodDao: FoodDao,
    private val customFoodDao: CustomFoodDao
) : ViewModel() {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Input state
    private val _proteinInput = MutableStateFlow("0")
    val proteinInput: StateFlow<String> = _proteinInput.asStateFlow()

    private val _targetProtein = MutableStateFlow("0")
    val targetProtein: StateFlow<String> = _targetProtein.asStateFlow()

    private val _durationInput = MutableStateFlow("")
    val durationInput: StateFlow<String> = _durationInput.asStateFlow()

    private val _carbsInput = MutableStateFlow("0")
    val carbsInput: StateFlow<String> = _carbsInput.asStateFlow()

    private val _targetCarbs = MutableStateFlow("0")
    val targetCarbs: StateFlow<String> = _targetCarbs.asStateFlow()

    private val _fatsInput = MutableStateFlow("0")
    val fatsInput: StateFlow<String> = _fatsInput.asStateFlow()

    private val _targetFats = MutableStateFlow("0")
    val targetFats: StateFlow<String> = _targetFats.asStateFlow()

    private val _fiberInput = MutableStateFlow("0")
    val fiberInput: StateFlow<String> = _fiberInput.asStateFlow()

    private val _targetFiber = MutableStateFlow("0")
    val targetFiber: StateFlow<String> = _targetFiber.asStateFlow()

    private val _sodiumInput = MutableStateFlow("0")
    val sodiumInput: StateFlow<String> = _sodiumInput.asStateFlow()

    private val _baseProtein = MutableStateFlow(0f)
    val baseProtein: StateFlow<Float> = _baseProtein.asStateFlow()

    private val _baseCarbs = MutableStateFlow(0f)
    val baseCarbs: StateFlow<Float> = _baseCarbs.asStateFlow()

    private val _baseFats = MutableStateFlow(0f)
    val baseFats: StateFlow<Float> = _baseFats.asStateFlow()

    private val _baseFiber = MutableStateFlow(0f)
    val baseFiber: StateFlow<Float> = _baseFiber.asStateFlow()

    private val _baseSodium = MutableStateFlow(0f)
    val baseSodium: StateFlow<Float> = _baseSodium.asStateFlow()

    private val _baseCalories = MutableStateFlow(0f)
    val baseCalories: StateFlow<Float> = _baseCalories.asStateFlow()

    private val _caloriesInput = MutableStateFlow("0")
    val caloriesInput: StateFlow<String> = _caloriesInput.asStateFlow()

    private val _portionSizeGrams = MutableStateFlow(100f)
    val portionSizeGrams: StateFlow<Float> = _portionSizeGrams.asStateFlow()

    private val _portionSizeInput = MutableStateFlow("100")
    val portionSizeInput: StateFlow<String> = _portionSizeInput.asStateFlow()

    private val _hasBaseMacroEstimate = MutableStateFlow(false)

    private val _targetSodium = MutableStateFlow("0")
    val targetSodium: StateFlow<String> = _targetSodium.asStateFlow()

    private val _sleepInput = MutableStateFlow("")
    val sleepInput: StateFlow<String> = _sleepInput.asStateFlow()

    private val _foodNameInput = MutableStateFlow("")
    val foodNameInput: StateFlow<String> = _foodNameInput.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()

    /** True when user picked from the dropdown — suppresses the next search trigger. */
    private var suppressNextSearch = false

    private val _customActivityNameInput = MutableStateFlow("")
    val customActivityNameInput: StateFlow<String> = _customActivityNameInput.asStateFlow()

    private val _selectedActivity = MutableStateFlow("Basketball")
    val selectedActivity: StateFlow<String> = _selectedActivity.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _selectedDateLog = MutableStateFlow<DailyLog?>(null)
    val selectedDateLog: StateFlow<DailyLog?> = _selectedDateLog.asStateFlow()

    private val _weeklyLogs = MutableStateFlow<List<DailyLog>>(emptyList())
    val weeklyLogs: StateFlow<List<DailyLog>> = _weeklyLogs.asStateFlow()

    private val _rollingSevenDayLogs = MutableStateFlow<List<DailyLog>>(emptyList())
    val rollingSevenDayLogs: StateFlow<List<DailyLog>> = _rollingSevenDayLogs.asStateFlow()

    private val _isEstimatingMacros = MutableStateFlow(false)
    val isEstimatingMacros: StateFlow<Boolean> = _isEstimatingMacros.asStateFlow()

    // Coach reaction states for RAG-injected prompts
    sealed class CoachReaction {
        data object Idle : CoachReaction()
        data object Loading : CoachReaction()
        data class Success(
            val verdict: String,
            val strategy: String,
            val workout: String
        ) : CoachReaction()
        data class Error(val message: String) : CoachReaction()
    }

    private val _coachReaction = MutableStateFlow<CoachReaction>(CoachReaction.Idle)
    val coachReaction: StateFlow<CoachReaction> = _coachReaction.asStateFlow()

    private val _dailySteps = MutableStateFlow(0L)
    val dailySteps: StateFlow<Long> = _dailySteps.asStateFlow()

    private val _dailyActiveCalories = MutableStateFlow(0f)
    val dailyActiveCalories: StateFlow<Float> = _dailyActiveCalories.asStateFlow()

    private var lastDeletedLog: DailyLog? = null

    val userProfile: StateFlow<UserProfile> = profileManager.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val selectedWeekRange: StateFlow<Pair<LocalDate, LocalDate>> = selectedDate
        .map { selected ->
            val weekStart = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            weekStart to weekStart.plusDays(6)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .let { it to it.plusDays(6) }
        )

    val analyticsUiState: StateFlow<AnalyticsUiState> = combine(
        selectedDateLog,
        userProfile,
        weeklyLogs,
        rollingSevenDayLogs
    ) { todayLog, profile, weekLogs, recentLogs ->
        val bmr = NutritionCalculator.calculateBMR(profile.weight, profile.height, profile.age)
        val targetCalories = NutritionCalculator.calculateDailyCalories(bmr, goal = profile.goal)
        val macros = NutritionCalculator.calculateTargetMacros(targetCalories, profile.weight)
        val weeklyConsistency = calculateWeeklyConsistency(recentLogs, macros.protein)
        val weeklyBars = buildWeeklyActivityBars(weekLogs)

        val todayProtein = todayLog?.protein ?: 0f
        val todayCarbs   = todayLog?.carbs   ?: 0f
        val todayFats    = todayLog?.fats    ?: 0f
        val todayCalories = (todayProtein * 4f) + (todayCarbs * 4f) + (todayFats * 9f)

        AnalyticsUiState(
            todayProtein = todayProtein,
            todayCarbs = todayCarbs,
            todayFats = todayFats,
            todayCalories = todayCalories,
            proteinTarget = macros.protein,
            carbsTarget = macros.carbs,
            fatsTarget = macros.fats,
            targetCalories = targetCalories,
            consistencyScorePercent = weeklyConsistency,
            weeklyActivityMinutes = weeklyBars
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    val healthPermissions: Set<String> = healthConnectManager.requiredPermissions

    val activityOptions = listOf(
        "Basketball",
        "Running",
        "Lifting",
        "Swimming",
        "Cycling",
        "Yoga",
        "Boxing",
        "Other"
    )

    init {
        // Log database status on startup for debugging
        viewModelScope.launch {
            try {
                val totalFoods = foodDao.getFoodCount()
                android.util.Log.d("SavageStats", "NutritionDB ready: $totalFoods foods available")
            } catch (e: Exception) {
                android.util.Log.e("SavageStats", "NutritionDB init failed: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            selectedDate.collectLatest { date ->
                repository
                    .getLogForDate(date.format(isoDateFormatter))
                    .collectLatest { log ->
                        _selectedDateLog.value = log
                    }
            }
        }

        viewModelScope.launch {
            selectedWeekRange.collectLatest { (start, end) ->
                repository.getLogsBetweenDates(
                    startDate = start.format(isoDateFormatter),
                    endDate = end.format(isoDateFormatter)
                ).collectLatest { logs ->
                    _weeklyLogs.value = logs
                }
            }
        }

        viewModelScope.launch {
            selectedDate.collectLatest { endDate ->
                val startDate = endDate.minusDays(6)
                repository.getLogsBetweenDates(
                    startDate = startDate.format(isoDateFormatter),
                    endDate = endDate.format(isoDateFormatter)
                ).collectLatest { logs ->
                    _rollingSevenDayLogs.value = logs
                }
            }
        }

        // Debounced food search — fires 300ms after the user stops typing
        viewModelScope.launch {
            _foodNameInput
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (suppressNextSearch) {
                        suppressNextSearch = false
                        return@collectLatest
                    }
                    
                    // 1. Sanitize: Remove characters that break SQLite FTS (keep only letters/numbers/spaces)
                    val sanitizedQuery = query.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                    
                    if (sanitizedQuery.length >= 2) {
                        try {
                            // 2. Tokenize: "fried chicken" -> "fried* chicken*"
                            // This makes FTS4 find rows containing ALL words (in any order)
                            val ftsQuery = sanitizedQuery.split("\\s+".toRegex())
                                .filter { it.isNotEmpty() }
                                .joinToString(" ") { "$it*" }
                            
                            if (ftsQuery.isNotBlank()) {
                                // Search USDA database (FTS)
                                val usdaResults = foodDao.searchFoods(ftsQuery)
                                // Search custom foods (LIKE)
                                val customResults = customFoodDao.searchFoods("%$sanitizedQuery%")
                                // Merge: custom foods first, then USDA
                                val merged = customResults.map { it.toFoodItem() } + usdaResults
                                android.util.Log.d("SavageStats", "Food search '$ftsQuery': ${usdaResults.size} USDA + ${customResults.size} custom")
                                _searchResults.value = merged
                                
                                // Provide feedback if no matches found
                                if (merged.isEmpty()) {
                                    _syncMessage.value = "No foods match '$sanitizedQuery'. Try: CHICKEN, BEEF, RICE..."
                                }
                            } else {
                                _searchResults.value = emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SavageStats", "Food search failed: ${e.message}", e)
                            _searchResults.value = emptyList()
                        }
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun onProteinChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _hasBaseMacroEstimate.value = false
            _proteinInput.value = value
            _targetProtein.value = value
        }
    }

    fun onDurationChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _durationInput.value = value
        }
    }

    fun onCarbsChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _hasBaseMacroEstimate.value = false
            _carbsInput.value = value
            _targetCarbs.value = value
        }
    }

    fun onFatsChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _hasBaseMacroEstimate.value = false
            _fatsInput.value = value
            _targetFats.value = value
        }
    }

    fun onFiberChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _hasBaseMacroEstimate.value = false
            _fiberInput.value = value
            _targetFiber.value = value
        }
    }

    fun onSodiumChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _hasBaseMacroEstimate.value = false
            _sodiumInput.value = value
            _targetSodium.value = value
        }
    }

    fun onCaloriesChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _caloriesInput.value = value
        }
    }

    fun onPortionSizeChanged(value: String) {
        if (!value.matches(Regex("\\d*\\.?\\d*"))) return
        _portionSizeInput.value = value

        val parsed = value.toFloatOrNull()
        _portionSizeGrams.value = (parsed ?: 0f).coerceAtLeast(0f)

        if (_hasBaseMacroEstimate.value) {
            applyScaledMacroInputs()
        }
    }

    fun onSleepChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _sleepInput.value = value
        }
    }

    fun onFoodNameChanged(value: String) {
        _foodNameInput.value = value
        if (value.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    /**
     * Called when the user taps a food item from the FTS search dropdown.
     * Populates verified macro values from the USDA database directly,
     * bypassing the LLM estimation entirely.
     * Then triggers AI Coach reaction with verified facts.
     */
    fun onFoodSelected(food: FoodItem) {
        suppressNextSearch = true
        _foodNameInput.value = food.name
        _searchResults.value = emptyList()

        // Populate macros from verified DB data (per 100g base)
        _baseProtein.value = food.protein
        _baseCarbs.value = food.carbs
        _baseFats.value = food.fat
        _baseFiber.value = food.fiber
        _baseSodium.value = food.sodium
        _baseCalories.value = food.calories
        _portionSizeGrams.value = 100f
        _portionSizeInput.value = "100"
        _hasBaseMacroEstimate.value = true
        applyScaledMacroInputs()

        val cals = food.calories.toInt()
        _syncMessage.value = "Verified: ${food.name.uppercase()} ($cals kcal/100g)"

        // Trigger AI Coach reaction with verified facts
        triggerCoachReaction(food)
    }

    /**
     * Triggers the AI Coach to react to a verified food selection.
     * Uses RAG-injected prompt with FACTS from the database.
     */
    private fun triggerCoachReaction(food: FoodItem) {
        viewModelScope.launch {
            // Skip if model not ready
            if (llmManager.loadStatus.value !is LlmInferenceManager.ModelLoadStatus.Ready) {
                return@launch
            }

            _coachReaction.value = CoachReaction.Loading

            try {
                // Get current daily progress from today's log
                val todayLog = _selectedDateLog.value
                val analytics = analyticsUiState.value
                
                val currentCals = analytics.todayCalories
                val targetCals = analytics.targetCalories
                val currentProtein = analytics.todayProtein
                val targetProtein = analytics.proteinTarget

                // Build the context-injected prompt
                val prompt = buildSavagePrompt(
                    selectedFood = food,
                    currentCals = currentCals,
                    targetCals = targetCals,
                    currentProtein = currentProtein,
                    targetProtein = targetProtein
                )

                // Generate response via LLM
                val response = llmManager.generateSavageResponse(prompt)
                android.util.Log.d("SavageStats", "Coach RAG response: $response")

                // Parse the template tags
                val parsed = parseCoachReactionResponse(response)
                _coachReaction.value = parsed

            } catch (e: Exception) {
                android.util.Log.e("SavageStats", "Coach reaction failed", e)
                _coachReaction.value = CoachReaction.Error("Coach is currently lifting. Try again.")
            }
        }
    }

    /**
     * Builds a context-injected prompt with verified food data and user's daily progress.
     * The LLM acts ONLY as a coach, not a calculator - all facts are provided.
     */
    private fun buildSavagePrompt(
        selectedFood: FoodItem,
        currentCals: Float,
        targetCals: Float,
        currentProtein: Float,
        targetProtein: Float
    ): String {
        val caloriesRemaining = (targetCals - currentCals).toInt()
        val proteinRemaining = (targetProtein - currentProtein).toInt()
        val isOverCalories = currentCals + selectedFood.calories > targetCals
        val isHighProtein = selectedFood.protein >= 20f

        return """
            FACTS (DO NOT INVENT NUMBERS):
            Food logged: ${selectedFood.name}
            Macros: ${selectedFood.calories.toInt()} kcal, ${selectedFood.protein.toInt()}g Protein, ${selectedFood.carbs.toInt()}g Carbs, ${selectedFood.fat.toInt()}g Fat.
            
            USER'S DAILY PROGRESS:
            Calories: ${currentCals.toInt()} / ${targetCals.toInt()} kcal (${caloriesRemaining} remaining)
            Protein: ${currentProtein.toInt()} / ${targetProtein.toInt()} g (${proteinRemaining}g remaining)
            
            TASK:
            React to this food choice. ${if (isOverCalories) "This DESTROYS their calorie limit!" else ""} ${if (isHighProtein) "This is a protein win." else ""}
            
            Respond with EXACTLY this format:
            [VERDICT] Your 1-2 sentence reaction to this food choice.
            [STRATEGY] One sentence of real advice.
            [WORKOUT] A quick punishment or reward exercise (e.g., "50 burpees" or "Rest day earned").
        """.trimIndent()
    }

    /**
     * Parses the AI response looking for [VERDICT], [STRATEGY], and [WORKOUT] tags.
     */
    private fun parseCoachReactionResponse(response: String): CoachReaction {
        val verdictRegex = Regex("""\[VERDICT\]\s*(.+?)(?=\[STRATEGY\]|\[WORKOUT\]|$)""", RegexOption.DOT_MATCHES_ALL)
        val strategyRegex = Regex("""\[STRATEGY\]\s*(.+?)(?=\[WORKOUT\]|$)""", RegexOption.DOT_MATCHES_ALL)
        val workoutRegex = Regex("""\[WORKOUT\]\s*(.+?)$""", RegexOption.DOT_MATCHES_ALL)

        val verdict = verdictRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
        val strategy = strategyRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
        val workout = workoutRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""

        // If no tags found, use the entire response as verdict
        return if (verdict.isBlank() && strategy.isBlank() && workout.isBlank()) {
            val cleanResponse = response.replace(Regex("\\*\\*"), "").trim()
            if (cleanResponse.isNotBlank()) {
                CoachReaction.Success(
                    verdict = cleanResponse.take(200),
                    strategy = "",
                    workout = ""
                )
            } else {
                CoachReaction.Error("Coach couldn't formulate a response.")
            }
        } else {
            CoachReaction.Success(
                verdict = verdict.replace(Regex("\\*\\*"), "").trim(),
                strategy = strategy.replace(Regex("\\*\\*"), "").trim(),
                workout = workout.replace(Regex("\\*\\*"), "").trim()
            )
        }
    }

    fun dismissCoachReaction() {
        _coachReaction.value = CoachReaction.Idle
    }

    /**
     * Manually triggers a DB food search (no debounce), so the user can
     * open the dropdown on demand even if autocomplete didn't fire.
     * If the input is blank, loads a sample of popular items to browse.
     */
    fun searchFoodManually() {
        viewModelScope.launch {
            val raw = _foodNameInput.value
            val sanitized = raw.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()

            try {
                if (sanitized.length >= 2) {
                    val ftsQuery = sanitized.split("\\s+".toRegex())
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { "$it*" }
                    // Search USDA database (FTS)
                    val usdaResults = foodDao.searchFoods(ftsQuery)
                    // Search custom foods (LIKE)
                    val customResults = customFoodDao.searchFoods("%$sanitized%")
                    // Merge: custom foods first, then USDA
                    val merged = customResults.map { it.toFoodItem() } + usdaResults
                    _searchResults.value = merged
                    if (merged.isEmpty()) {
                        _syncMessage.value = "No foods match '$sanitized'. Try: CHICKEN, BEEF, RICE..."
                    }
                } else {
                    // Input too short — show sample foods so user can browse
                    val samples = foodDao.getSampleFoods()
                    _searchResults.value = samples
                    if (samples.isEmpty()) {
                        _syncMessage.value = "Food database is empty. Reinstall to restore."
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SavageStats", "Manual food search failed: ${e.message}", e)
                _syncMessage.value = "Search failed: ${e.message}"
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Saves a user-created custom food to the local database and
     * immediately selects it so that the main form's calorie/macro
     * fields are auto-populated — ready to log.
     */
    fun saveCustomFood(
        name: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        sodium: Float
    ) {
        if (name.isBlank()) {
            _syncMessage.value = "Enter a food name before saving."
            return
        }

        viewModelScope.launch {
            try {
                customFoodDao.insertFood(
                    CustomFoodItem(
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        fiber = fiber,
                        sodium = sodium
                    )
                )
                _syncMessage.value = "Custom food '${name.uppercase()}' saved."

                // Auto-select the saved food so calories/macros fill the form
                val asFoodItem = FoodItem(
                    id = -1,  // Temporary — not from USDA
                    name = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    sodium = sodium
                )
                onFoodSelected(asFoodItem)
            } catch (e: Exception) {
                android.util.Log.e("SavageStats", "Save custom food failed: ${e.message}", e)
                _syncMessage.value = "Failed to save custom food: ${e.message}"
            }
        }
    }

    fun onCustomActivityNameChanged(value: String) {
        _customActivityNameInput.value = value
    }

    fun onActivitySelected(activity: String) {
        _selectedActivity.value = activity
        if (activity != "Other") {
            _customActivityNameInput.value = ""
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveLog() {
        val protein = _proteinInput.value.toFloatOrNull() ?: 0f
        val carbs = _carbsInput.value.toFloatOrNull() ?: 0f
        val fats = _fatsInput.value.toFloatOrNull() ?: 0f
        val fiber = _fiberInput.value.toFloatOrNull() ?: 0f
        val sodium = _sodiumInput.value.toFloatOrNull() ?: 0f
        val duration = _durationInput.value.toFloatOrNull() ?: 0f
        val sleep = _sleepInput.value.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            repository.insertLog(
                date = _selectedDate.value.format(isoDateFormatter),
                protein = protein,
                carbs = carbs,
                fats = fats,
                fiber = fiber,
                sodium = sodium,
                foodName = _foodNameInput.value.trim(),
                activityDurationMinutes = duration,
                activityType = resolveActivityName(),
                sleepHours = sleep,
                dailySteps = _dailySteps.value,
                activeCalories = _dailyActiveCalories.value,
            )
            _saveSuccess.value = true
            // Reset inputs after save
            _proteinInput.value = "0"
            _durationInput.value = ""
            _carbsInput.value = "0"
            _fatsInput.value = "0"
            _fiberInput.value = "0"
            _sodiumInput.value = "0"
            _caloriesInput.value = "0"
            _sleepInput.value = ""
            _foodNameInput.value = ""
        }
    }

    fun addMealToDailyLog() {
        val portionMultiplier = (_portionSizeGrams.value / 100f).coerceAtLeast(0f)
        val proteinToAdd = if (_hasBaseMacroEstimate.value) {
            (_baseProtein.value * portionMultiplier).coerceAtLeast(0f)
        } else {
            _proteinInput.value.toFloatOrNull() ?: 0f
        }
        val carbsToAdd = if (_hasBaseMacroEstimate.value) {
            (_baseCarbs.value * portionMultiplier).coerceAtLeast(0f)
        } else {
            _carbsInput.value.toFloatOrNull() ?: 0f
        }
        val fatsToAdd = if (_hasBaseMacroEstimate.value) {
            (_baseFats.value * portionMultiplier).coerceAtLeast(0f)
        } else {
            _fatsInput.value.toFloatOrNull() ?: 0f
        }
        val fiberToAdd = if (_hasBaseMacroEstimate.value) {
            (_baseFiber.value * portionMultiplier).coerceAtLeast(0f)
        } else {
            _fiberInput.value.toFloatOrNull() ?: 0f
        }
        val sodiumToAdd = if (_hasBaseMacroEstimate.value) {
            (_baseSodium.value * portionMultiplier).coerceAtLeast(0f)
        } else {
            _sodiumInput.value.toFloatOrNull() ?: 0f
        }

        viewModelScope.launch {
            val date = _selectedDate.value.format(isoDateFormatter)
            val existingLog = repository.getLogForDate(date).first() ?: emptyDailyLog(date)

            repository.insertLog(
                existingLog.copy(
                    protein = (existingLog.protein + proteinToAdd).coerceAtLeast(0f),
                    carbs = (existingLog.carbs + carbsToAdd).coerceAtLeast(0f),
                    fats = (existingLog.fats + fatsToAdd).coerceAtLeast(0f),
                    fiber = (existingLog.fiber + fiberToAdd).coerceAtLeast(0f),
                    sodium = (existingLog.sodium + sodiumToAdd).coerceAtLeast(0f),
                    foodName = _foodNameInput.value.trim(),
                )
            )

            _saveSuccess.value = true
            _proteinInput.value = "0"
            _carbsInput.value = "0"
            _fatsInput.value = "0"
            _fiberInput.value = "0"
            _sodiumInput.value = "0"
            _caloriesInput.value = "0"
            _foodNameInput.value = ""
            _hasBaseMacroEstimate.value = false
            _portionSizeGrams.value = 100f
            _portionSizeInput.value = "100"
        }
    }

    fun logActivityAndSleep() {
        val activityDuration = _durationInput.value.toFloatOrNull() ?: 0f
        val sleepHours = _sleepInput.value.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            val date = _selectedDate.value.format(isoDateFormatter)
            val existingLog = repository.getLogForDate(date).first() ?: emptyDailyLog(date)

            repository.insertLog(
                existingLog.copy(
                    activityDurationMinutes = activityDuration.coerceAtLeast(0f),
                    activityType = resolveActivityName(),
                    sleepHours = sleepHours.coerceAtLeast(0f),
                )
            )

            _saveSuccess.value = true
            _durationInput.value = ""
            _sleepInput.value = ""
        }
    }

    /**
     * Called when the camera/gallery scan returns ML Kit tag(s).
     * Routes each tag through the FTS food database, most-specific first.
     * Fills the search bar with the best-matching tag and opens the dropdown
     * so the user picks a verified FoodItem instead of going to the LLM.
     *
     * Tag cluster format: "Food, Fruit, Apple, Red" (comma-separated).
     * Generic labels like "Food", "Dish", "Cuisine" are skipped.
     */
    fun onCameraTagDetected(tagCluster: String) {
        if (tagCluster.isBlank()) return

        viewModelScope.launch {
            val genericLabels = setOf(
                "food", "dish", "cuisine", "meal", "ingredient",
                "plant", "produce", "natural foods", "recipe",
                "tableware", "plate", "bowl", "fast food"
            )

            // Split comma-separated tags, try most-specific first (later = more specific from ML Kit)
            val tags = tagCluster.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.lowercase() !in genericLabels }
                .reversed()  // ML Kit puts generic labels first, specific last

            var bestTag = tags.firstOrNull() ?: tagCluster.split(",").first().trim()
            var bestResults = emptyList<FoodItem>()

            for (tag in tags) {
                val sanitized = tag.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                if (sanitized.length < 2) continue

                val ftsQuery = sanitized.split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }
                    .joinToString(" ") { "$it*" }

                try {
                    val results = foodDao.searchFoods(ftsQuery)
                    if (results.isNotEmpty()) {
                        bestTag = tag
                        bestResults = results
                        break  // Found DB matches — use this tag
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SavageStats", "Camera tag search failed for '$tag': ${e.message}")
                }
            }

            // Fill the search bar and show results
            _foodNameInput.value = bestTag
            _searchResults.value = bestResults

            if (bestResults.isEmpty()) {
                // No DB match for any tag — show message, user can refine manually
                _syncMessage.value = "No exact match for '$bestTag'. Refine or pick from suggestions."
                // Try a broader search with the original cluster's first meaningful tag
                searchFoodManually()
            }
        }
    }

    fun estimateMacrosFromScan(foodLabel: String) {
        if (foodLabel.isBlank() || _isEstimatingMacros.value) return

        _foodNameInput.value = foodLabel

        viewModelScope.launch {
            _isEstimatingMacros.value = true
            try {
                // ── DB-first: check if the food exists in our verified nutrition DB ──
                val sanitized = foodLabel.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                if (sanitized.length >= 2) {
                    val ftsQuery = sanitized.split("\\s+".toRegex())
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { "$it*" }
                    val dbResults = foodDao.searchFoods(ftsQuery)
                    if (dbResults.isNotEmpty()) {
                        // Use the top DB match — verified data, no AI needed
                        val topMatch = dbResults.first()
                        android.util.Log.d("SavageStats", "DB-first match: ${topMatch.name}")
                        onFoodSelected(topMatch)
                        _syncMessage.value = "Found in DB: ${topMatch.name.uppercase()} (${topMatch.calories.toInt()} kcal/100g)"
                        return@launch
                    }
                }

                // ── Fallback: LLM estimation when no DB match ──
                // Completion-style prompt: we start the answer ourselves with "["
                // so Gemma's only job is to finish the number sequence.
                // This is far more reliable than instruction-following on a 2B model.
                val prompt = "Nutrition per 100g of $foodLabel — " +
                    "protein(g), carbs(g), fat(g), fiber(g), sodium(mg): ["
                val rawResponse = llmManager.generateSavageResponse(prompt)
                // Reconstruct the full bracket so the parser can find it
                val response = "[$rawResponse"
                android.util.Log.d("SavageStats", "Gemma raw: $rawResponse")
                android.util.Log.d("SavageStats", "Gemma reconstructed: $response")

                // Only bail out on actual LlmInferenceManager error strings
                if (
                    response.contains("model not loaded", ignoreCase = true) ||
                    response.contains("inference failed", ignoreCase = true)
                ) {
                    _syncMessage.value = response.trim()
                    return@launch
                }

                val estimate = parseMacroEstimate(response)

                if (estimate != null) {
                    _baseProtein.value = estimate.protein
                    _baseCarbs.value = estimate.carbs
                    _baseFats.value = estimate.fats
                    _baseFiber.value = estimate.fiber
                    _baseSodium.value = estimate.sodium
                    _portionSizeGrams.value = 100f
                    _portionSizeInput.value = "100"
                    _hasBaseMacroEstimate.value = true
                    applyScaledMacroInputs()

                    _syncMessage.value = "AI estimated macros for ${foodLabel.uppercase()}."
                } else {
                    // AI failed — show dropdown results so user can pick manually
                    searchFoodManually()
                    _syncMessage.value = "AI couldn't parse macros. Use the search dropdown or enter manually."
                }
            } catch (e: Exception) {
                _syncMessage.value = "Macro estimate failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isEstimatingMacros.value = false
            }
        }
    }

    fun checkPermissionsAndRunSync(permissionLauncher: ActivityResultLauncher<Set<String>>) {
        viewModelScope.launch {
            healthConnectManager.checkPermissionsAndRun(permissionLauncher) {
                syncDailyMetricsInternal()
            }
        }
    }

    /** Called from the composable after permissions are granted via the launcher. */
    fun triggerSyncAfterPermission() {
        viewModelScope.launch { syncDailyMetricsInternal() }
    }

    /** True only if Health Connect is available AND all required permissions are held. */
    suspend fun hasAllPermissions(): Boolean = healthConnectManager.hasAllPermissions()

    @Suppress("UNUSED_PARAMETER")
    fun onSyncDevicesClicked(context: Context) {
        viewModelScope.launch {
            if (!healthConnectManager.isHealthConnectAvailable()) {
                onHealthConnectUnavailable()
                return@launch
            }
            syncDailyMetricsInternal()
        }
    }

    private suspend fun syncDailyMetricsInternal() {
        if (_isSyncing.value) return
        _isSyncing.value = true

        try {
            val metrics = healthConnectManager.syncDailyMetrics()
            _dailySteps.value = metrics.totalSteps
            _dailyActiveCalories.value = metrics.activeCalories

            val todayDate = _selectedDate.value.format(isoDateFormatter)
            val existingLog = repository.getLogForDate(todayDate).first() ?: emptyDailyLog(todayDate)

            repository.insertLog(
                existingLog.copy(
                    activityDurationMinutes = metrics.activeCalories.coerceAtLeast(0f),
                    dailySteps = metrics.totalSteps,
                    activeCalories = metrics.activeCalories.coerceAtLeast(0f),
                )
            )

            _syncMessage.value = "Data extracted. The numbers don't lie."
        } catch (e: Exception) {
            _syncMessage.value = "Health sync failed: ${e.message ?: "Unknown error"}"
        } finally {
            _isSyncing.value = false
        }
    }

    fun isHealthConnectAvailable(): Boolean = healthConnectManager.isHealthConnectAvailable()

    fun onHealthConnectUnavailable() {
        if (healthConnectManager.isHealthConnectUpdateRequired()) {
            _syncMessage.value = "Health Connect needs to be updated. Please update it in the Play Store."
        } else {
            _syncMessage.value = "Health Connect is not available on this device."
        }
    }

    fun syncWithHealthConnect() {
        viewModelScope.launch {
            syncWithHealthConnectInternal()
        }
    }

    fun onHealthPermissionsDenied() {
        // "Needs Updating" badge in HC silently blocks the permission sheet on some devices.
        // Direct the user to grant manually from HC settings — this bypasses the badge.
        _syncMessage.value = "Permission denied. Go to Settings → Health Connect → App Permissions → SavageStats to grant access manually."
    }

    private suspend fun syncWithHealthConnectInternal() {
        if (_isSyncing.value) return

        _isSyncing.value = true
        try {
            val importedLog = healthConnectManager.fetchYesterdayData()
            if (importedLog != null) {
                repository.insertLog(importedLog)
                _selectedDate.value = LocalDate.parse(importedLog.date, isoDateFormatter)
                _syncMessage.value = "Synced yesterday's workout & sleep from Health Connect."
            } else {
                _syncMessage.value = "No Health Connect data found for yesterday."
            }
        } catch (e: Exception) {
            _syncMessage.value = "Health sync failed: ${e.message ?: "Unknown error"}"
        } finally {
            _isSyncing.value = false
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun updateSelectedDayLogFromInputs(isNutritionTab: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value.format(isoDateFormatter)
            val existingLog = repository.getLogForDate(date).first() ?: emptyDailyLog(date)

            if (isNutritionTab) {
                // Only update nutrition fields, preserve activity data
                val protein = _proteinInput.value.toFloatOrNull() ?: 0f
                val carbs = _carbsInput.value.toFloatOrNull() ?: 0f
                val fats = _fatsInput.value.toFloatOrNull() ?: 0f
                val fiber = _fiberInput.value.toFloatOrNull() ?: 0f
                val sodium = _sodiumInput.value.toFloatOrNull() ?: 0f

                repository.updateLog(
                    existingLog.copy(
                        protein = protein.coerceAtLeast(0f),
                        carbs = carbs.coerceAtLeast(0f),
                        fats = fats.coerceAtLeast(0f),
                        fiber = fiber.coerceAtLeast(0f),
                        sodium = sodium.coerceAtLeast(0f),
                        foodName = _foodNameInput.value.trim(),
                        // Preserve activity data
                        activityDurationMinutes = existingLog.activityDurationMinutes,
                        activityType = existingLog.activityType,
                        sleepHours = existingLog.sleepHours,
                    )
                )
            } else {
                // Only update activity fields, preserve nutrition data
                val duration = _durationInput.value.toFloatOrNull() ?: 0f
                val sleep = _sleepInput.value.toFloatOrNull() ?: 0f

                repository.updateLog(
                    existingLog.copy(
                        // Preserve nutrition data
                        protein = existingLog.protein,
                        carbs = existingLog.carbs,
                        fats = existingLog.fats,
                        fiber = existingLog.fiber,
                        sodium = existingLog.sodium,
                        foodName = existingLog.foodName,
                        // Update activity data
                        activityDurationMinutes = duration.coerceAtLeast(0f),
                        activityType = resolveActivityName(),
                        sleepHours = sleep.coerceAtLeast(0f),
                    )
                )
            }

            _saveSuccess.value = true
        }
    }

    fun deleteSelectedDayLog() {
        viewModelScope.launch {
            val date = _selectedDate.value.format(isoDateFormatter)
            val existingLog = repository.getLogForDate(date).first()

            if (existingLog == null) {
                _syncMessage.value = "Nothing to delete for this day."
                return@launch
            }

            lastDeletedLog = existingLog
            repository.deleteLogByDate(date)
            _syncMessage.value = "Log deleted."
        }
    }

    fun undoDeleteLog() {
        val deletedLog = lastDeletedLog ?: return

        viewModelScope.launch {
            repository.insertLog(deletedLog)
            _syncMessage.value = "Delete undone. Back to the grind."
            lastDeletedLog = null
        }
    }

    private fun resolveActivityName(): String {
        return if (_selectedActivity.value == "Other") {
            _customActivityNameInput.value.trim().ifBlank { "Other" }
        } else {
            _selectedActivity.value
        }
    }

    data class AnalyticsUiState(
        val todayProtein: Float = 0f,
        val todayCarbs: Float = 0f,
        val todayFats: Float = 0f,
        val todayCalories: Float = 0f,
        val proteinTarget: Float = 1f,
        val carbsTarget: Float = 1f,
        val fatsTarget: Float = 1f,
        val targetCalories: Float = 0f,
        val consistencyScorePercent: Int = 0,
        val weeklyActivityMinutes: List<Float> = List(7) { 0f },
    )

    private fun calculateWeeklyConsistency(logs: List<DailyLog>, proteinTarget: Float): Int {
        val proteinMinimum = proteinTarget * 0.8f
        val successfulDays = logs.count { log -> log.protein >= proteinMinimum }
        return ((successfulDays / 7f) * 100f).toInt().coerceIn(0, 100)
    }

    private fun buildWeeklyActivityBars(logs: List<DailyLog>): List<Float> {
        val weekStart = selectedWeekRange.value.first
        val grouped = logs.associateBy { it.date }
        return (0..6).map { dayOffset ->
            val day = weekStart.plusDays(dayOffset.toLong()).format(isoDateFormatter)
            (grouped[day]?.activityDurationMinutes ?: 0f).coerceAtLeast(0f)
        }
    }

    private data class MacroEstimate(
        val protein: Float,
        val carbs: Float,
        val fats: Float,
        val fiber: Float,
        val sodium: Float,
    )

    private fun parseMacroEstimate(response: String): MacroEstimate? {
        // Strip common unit suffixes so "25g" or "120mg" become "25" / "120"
        val cleaned = response.replace(Regex("(\\d)\\s*(?:g|mg|kcal|cal)\\b", RegexOption.IGNORE_CASE)) { match ->
            match.groupValues[1]
        }
        val numberRegex = Regex("-?\\d+(?:\\.\\d+)?")

        // Strategy 1: find the first [n, n, n, ...] bracket in the response.
        // This is the intended output format and avoids picking up numbers from
        // any preamble or explanation Gemma adds before the bracket.
        val bracketRegex = Regex("\\[([^\\]]+)\\]")
        val bracketMatch = bracketRegex.find(cleaned)
        if (bracketMatch != null) {
            val nums = numberRegex.findAll(bracketMatch.groupValues[1])
                .mapNotNull { it.value.toFloatOrNull() }
                .toList()
            if (nums.size >= 3) {
                android.util.Log.d("SavageStats", "parseMacroEstimate (bracket): $nums | raw: $response")
                return MacroEstimate(
                    protein = nums[0],
                    carbs = nums[1],
                    fats = nums[2],
                    fiber = nums.getOrElse(3) { 0f },
                    sodium = nums.getOrElse(4) { 0f }
                )
            }
        }

        // Strategy 2: fallback — scan the entire response for the first 3+ numbers.
        // Handles prose-style output like "Protein: 25g, Carbs: 0g ...".
        val allNums = numberRegex.findAll(cleaned)
            .mapNotNull { it.value.toFloatOrNull() }
            .toList()

        if (allNums.size < 3) {
            android.util.Log.e("SavageStats", "parseMacroEstimate: need 3+ numbers, got ${allNums.size}. Raw: $response")
            return null
        }

        android.util.Log.d("SavageStats", "parseMacroEstimate (fallback): $allNums | raw: $response")
        return MacroEstimate(
            protein = allNums[0],
            carbs = allNums[1],
            fats = allNums[2],
            fiber = allNums.getOrElse(3) { 0f },
            sodium = allNums.getOrElse(4) { 0f }
        )
    }

    private fun formatNumericInput(value: Float): String {
        val rounded = kotlin.math.round(value * 10f) / 10f
        return if (rounded % 1f == 0f) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    private fun applyScaledMacroInputs() {
        val multiplier = (_portionSizeGrams.value / 100f).coerceAtLeast(0f)

        val proteinValue = formatNumericInput(_baseProtein.value * multiplier)
        val carbsValue = formatNumericInput(_baseCarbs.value * multiplier)
        val fatsValue = formatNumericInput(_baseFats.value * multiplier)
        val fiberValue = formatNumericInput(_baseFiber.value * multiplier)
        val sodiumValue = formatNumericInput(_baseSodium.value * multiplier)
        val caloriesValue = formatNumericInput(_baseCalories.value * multiplier)

        _proteinInput.value = proteinValue
        _carbsInput.value = carbsValue
        _fatsInput.value = fatsValue
        _fiberInput.value = fiberValue
        _sodiumInput.value = sodiumValue
        _caloriesInput.value = caloriesValue

        _targetProtein.value = proteinValue
        _targetCarbs.value = carbsValue
        _targetFats.value = fatsValue
        _targetFiber.value = fiberValue
        _targetSodium.value = sodiumValue
    }

    private fun emptyDailyLog(date: String): DailyLog = DailyLog(
        date = date,
        protein = 0f,
            carbs = 0f,
            fats = 0f,
            fiber = 0f,
            sodium = 0f,
            foodName = "",
            activityDurationMinutes = 0f,
            activityType = resolveActivityName(),
            sleepHours = 0f,
            dailySteps = 0L,
            activeCalories = 0f,
        )

    class Factory(
        private val repository: LogRepository,
        private val healthConnectManager: HealthConnectManager,
        private val profileManager: UserProfileManager,
        private val llmManager: LlmInferenceManager,
        private val foodDao: FoodDao,
        private val customFoodDao: CustomFoodDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository, healthConnectManager, profileManager, llmManager, foodDao, customFoodDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
