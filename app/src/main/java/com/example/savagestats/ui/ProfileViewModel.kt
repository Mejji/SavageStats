package com.example.savagestats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.savagestats.data.UserProfile
import com.example.savagestats.data.UserProfileManager
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
    private val _ageInput = MutableStateFlow("")
    val ageInput: StateFlow<String> = _ageInput.asStateFlow()

    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    private val _heightInput = MutableStateFlow("")
    val heightInput: StateFlow<String> = _heightInput.asStateFlow()

    private val _selectedGoal = MutableStateFlow("")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    val goalOptions = listOf(
        "Build Muscle",
        "Athletic Conditioning",
        "Lose Fat",
        "Maintenance",
        "Endurance"
    )

    init {
        // Pre-fill inputs from saved profile
        viewModelScope.launch {
            profileManager.userProfile.collect { profile ->
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
                    _selectedGoal.value = profile.goal
                }
            }
        }
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
    }

    fun saveProfile() {
        val age = _ageInput.value.toIntOrNull() ?: 0
        val weight = _weightInput.value.toFloatOrNull() ?: 0f
        val height = _heightInput.value.toFloatOrNull() ?: 0f
        val goal = _selectedGoal.value.ifEmpty { return }

        viewModelScope.launch {
            profileManager.updateProfile(
                UserProfile(
                    age = age,
                    weight = weight,
                    height = height,
                    goal = goal
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
