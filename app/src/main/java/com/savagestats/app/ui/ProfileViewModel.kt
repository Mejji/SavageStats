package com.savagestats.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.savagestats.app.data.UserProfile
import com.savagestats.app.data.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileManager: UserProfileManager) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = profileManager.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    // Input state
    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _ageInput = MutableStateFlow("")
    val ageInput: StateFlow<String> = _ageInput.asStateFlow()

    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    private val _heightInput = MutableStateFlow("")
    val heightInput: StateFlow<String> = _heightInput.asStateFlow()

    private val _selectedGoal = MutableStateFlow("")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    private val _customGoalInput = MutableStateFlow("")
    val customGoalInput: StateFlow<String> = _customGoalInput.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    val goalOptions = listOf(
        "Build Muscle",
        "Athletic Conditioning",
        "Lose Fat",
        "Maintenance",
        "Endurance",
        "Custom"
    )

    init {
        // Pre-fill inputs from saved profile
        viewModelScope.launch {
            profileManager.userProfile.collect { profile ->
                if (_nameInput.value.isEmpty() && profile.name.isNotEmpty()) {
                    _nameInput.value = profile.name
                }
                if (_ageInput.value.isEmpty() && profile.age > 0) {
                    _ageInput.value = profile.age.toString()
                }
                if (_weightInput.value.isEmpty() && profile.weight > 0f) {
                    _weightInput.value = profile.weight.toString()
                }
                if (_heightInput.value.isEmpty() && profile.height > 0f) {
                    _heightInput.value = profile.height.toString()
                }
                if (_selectedGoal.value.isEmpty() && profile.goal.isNotEmpty()) {
                    val isPreset = goalOptions.dropLast(1).contains(profile.goal)
                    if (isPreset) {
                        _selectedGoal.value = profile.goal
                    } else {
                        // Previously saved a custom goal — restore it
                        _selectedGoal.value = "Custom"
                        _customGoalInput.value = profile.goal
                    }
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _nameInput.value = value
    }

    fun onAgeChanged(value: String) {
        if (value.all { it.isDigit() }) {
            _ageInput.value = value
        }
    }

    fun onWeightChanged(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _weightInput.value = value
        }
    }

    fun onHeightChanged(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _heightInput.value = value
        }
    }

    fun onGoalSelected(goal: String) {
        _selectedGoal.value = goal
        // Clear custom text when switching away from Custom
        if (goal != "Custom") _customGoalInput.value = ""
    }

    fun onCustomGoalChanged(value: String) {
        _customGoalInput.value = value
    }

    fun saveProfile() {
        val name = _nameInput.value.ifBlank { return }
        val age = _ageInput.value.toIntOrNull() ?: 0
        val weight = _weightInput.value.toFloatOrNull() ?: 0f
        val height = _heightInput.value.toFloatOrNull() ?: 0f

        // If "Custom" is selected, save the typed text instead of the word "Custom".
        // Guard: don't save if custom is selected but the field is blank.
        val goal = if (_selectedGoal.value == "Custom") {
            _customGoalInput.value.trim().ifBlank { return }
        } else {
            _selectedGoal.value.ifEmpty { return }
        }

        viewModelScope.launch {
            val current = userProfile.value
            profileManager.updateProfile(
                UserProfile(
                    name = name,
                    age = age,
                    weight = weight,
                    height = height,
                    goal = goal,
                    targetWeight = current.targetWeight,
                    savageXp = current.savageXp
                )
            )
            _saveSuccess.value = true
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    class Factory(private val profileManager: UserProfileManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(profileManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
