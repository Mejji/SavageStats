package com.savagestats.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.savagestats.app.data.DailyLog
import com.savagestats.app.data.LogRepository
import com.savagestats.app.data.Mission
import com.savagestats.app.data.MissionTask
import com.savagestats.app.data.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MissionsViewModel(
    private val repository: LogRepository,
    private val profileManager: UserProfileManager,
) : ViewModel() {

    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val activeMissions: StateFlow<List<Mission>> = repository
        .getActiveMissions(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedMissions: StateFlow<List<Mission>> = repository
        .getCompletedMissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _missionCompleteEvent = MutableStateFlow<Mission?>(null)
    val missionCompleteEvent: StateFlow<Mission?> = _missionCompleteEvent.asStateFlow()

    fun onTaskChecked(mission: Mission, taskIndex: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val updatedTasks = mission.tasks.mapIndexed { index, task ->
                if (index == taskIndex) task.copy(isDone = isChecked) else task
            }

            val allDone = updatedTasks.all { it.isDone }
            val updatedMission = mission.copy(
                tasks = updatedTasks,
                isCompleted = allDone,
            )

            repository.updateMission(updatedMission)

            if (allDone) {
                onMissionCompleted(updatedMission)
            }
        }
    }

    private suspend fun onMissionCompleted(mission: Mission) {
        // Award 50 XP
        profileManager.addXp(50)

        // Auto-log a 60-minute "Savage Mission" activity for today
        val todayDate = LocalDate.now().format(isoDateFormatter)
        val existingLog = repository.getLogForDate(todayDate).first()

        if (existingLog != null) {
            repository.insertLog(
                existingLog.copy(
                    activityDurationMinutes = (existingLog.activityDurationMinutes + 60f).coerceAtLeast(0f),
                    activityType = "Savage Mission",
                )
            )
        } else {
            repository.insertLog(
                DailyLog(
                    date = todayDate,
                    protein = 0f,
                    carbs = 0f,
                    fats = 0f,
                    fiber = 0f,
                    sodium = 0f,
                    foodName = "",
                    activityDurationMinutes = 60f,
                    activityType = "Savage Mission",
                    sleepHours = 0f,
                )
            )
        }

        _missionCompleteEvent.value = mission
    }

    fun clearMissionCompleteEvent() {
        _missionCompleteEvent.value = null
    }

    class Factory(
        private val repository: LogRepository,
        private val profileManager: UserProfileManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MissionsViewModel::class.java)) {
                return MissionsViewModel(repository, profileManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
