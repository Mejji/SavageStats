package com.example.savagestats.data

import kotlinx.coroutines.flow.Flow

class LogRepository(private val dailyLogDao: DailyLogDao) {

    fun getLogForDate(date: String): Flow<DailyLog?> =
        dailyLogDao.getLogForDate(date)

    suspend fun insertLog(log: DailyLog) =
        dailyLogDao.insertLog(log)

    fun getLastSevenDaysLogs(): Flow<List<DailyLog>> =
        dailyLogDao.getLastSevenDaysLogs()
}
