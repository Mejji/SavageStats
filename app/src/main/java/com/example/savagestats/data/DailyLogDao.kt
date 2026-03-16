package com.example.savagestats.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun getLogForDate(date: String): Flow<DailyLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT 7")
    fun getLastSevenDaysLogs(): Flow<List<DailyLog>>
}
