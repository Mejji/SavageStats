package com.example.savagestats.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.savagestats.data.DailyLog
import com.example.savagestats.data.HealthConnectManager
import com.example.savagestats.data.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val repository: LogRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Input state
    private val _proteinInput = MutableStateFlow("")
    val proteinInput: StateFlow<String> = _proteinInput.asStateFlow()

    private val _durationInput = MutableStateFlow("")
    val durationInput: StateFlow<String> = _durationInput.asStateFlow()

    private val _sleepInput = MutableStateFlow("")
    val sleepInput: StateFlow<String> = _sleepInput.asStateFlow()

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
        viewModelScope.launch {
            selectedDate.collectLatest { date ->
                repository
                    .getLogForDate(date.format(isoDateFormatter))
                    .collectLatest { log ->
                        _selectedDateLog.value = log
                    }
            }
        }
    }

    fun onProteinChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _proteinInput.value = value
        }
    }

    fun onDurationChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _durationInput.value = value
        }
    }

    fun onSleepChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
            _sleepInput.value = value
        }
    }

    fun onActivitySelected(activity: String) {
        _selectedActivity.value = activity
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveLog() {
        val protein = _proteinInput.value.toFloatOrNull() ?: 0f
        val duration = _durationInput.value.toFloatOrNull() ?: 0f
        val sleep = _sleepInput.value.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            repository.insertLog(
                DailyLog(
                    date = _selectedDate.value.format(isoDateFormatter),
                    proteinGrams = protein,
                    activityDurationMinutes = duration,
                    activityType = _selectedActivity.value,
                    sleepHours = sleep
                )
            )
            _saveSuccess.value = true
            // Reset inputs after save
            _proteinInput.value = ""
            _durationInput.value = ""
            _sleepInput.value = ""
        }
    }

    fun checkPermissionsAndRunSync(permissionLauncher: ActivityResultLauncher<Set<String>>) {
        viewModelScope.launch {
            healthConnectManager.checkPermissionsAndRun(permissionLauncher) {
                syncWithHealthConnectInternal()
            }
        }
    }

    fun syncWithHealthConnect() {
        viewModelScope.launch {
            syncWithHealthConnectInternal()
        }
    }

    fun onHealthPermissionsDenied() {
        _syncMessage.value = "Health Connect permission denied."
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

    class Factory(
        private val repository: LogRepository,
        private val healthConnectManager: HealthConnectManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository, healthConnectManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
