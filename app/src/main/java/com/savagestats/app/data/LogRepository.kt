package com.savagestats.app.data

import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val dailyLogDao: DailyLogDao,
    private val missionDao: MissionDao,
) {

    // ── DailyLog operations ─────────────────────────────────

    fun getLogForDate(date: String): Flow<DailyLog?> =
        dailyLogDao.getLogForDate(date)

    suspend fun insertLog(log: DailyLog) =
        dailyLogDao.insertLog(log)

    suspend fun updateLog(log: DailyLog) =
        dailyLogDao.updateLog(log)

    suspend fun deleteLogByDate(date: String) =
        dailyLogDao.deleteLogByDate(date)

    suspend fun insertLog(
        date: String,
        protein: Float,
        carbs: Float,
        fats: Float,
        fiber: Float,
        sodium: Float,
        foodName: String = "",
        activityDurationMinutes: Float,
        activityType: String,
        sleepHours: Float,
        dailySteps: Long = 0L,
        activeCalories: Float = 0f,
    ) {
        dailyLogDao.insertLog(
            DailyLog(
                date = date,
                protein = protein,
                carbs = carbs,
                fats = fats,
                fiber = fiber,
                sodium = sodium,
                foodName = foodName,
                activityDurationMinutes = activityDurationMinutes,
                activityType = activityType,
                sleepHours = sleepHours,
                dailySteps = dailySteps,
                activeCalories = activeCalories,
            )
        )
    }

    fun getLastSevenDaysLogs(): Flow<List<DailyLog>> =
        dailyLogDao.getLastSevenDaysLogs()

    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>> =
        dailyLogDao.getLogsBetweenDates(startDate, endDate)

    // ── Mission operations ──────────────────────────────────

    suspend fun insertMission(mission: Mission) =
        missionDao.insertMission(mission)

    suspend fun updateMission(mission: Mission) =
        missionDao.updateMission(mission)

    fun getActiveMissions(now: Long): Flow<List<Mission>> =
        missionDao.getActiveMissions(now)

    fun getCompletedMissions(): Flow<List<Mission>> =
        missionDao.getCompletedMissions()

    suspend fun getMissionById(id: Int): Mission? =
        missionDao.getMissionById(id)

    fun getFailedMissionsCount(now: Long): Flow<Int> =
        missionDao.getFailedMissionsCount(now)

    fun getAllMissions(): Flow<List<Mission>> =
        missionDao.getAllMissions()
}
