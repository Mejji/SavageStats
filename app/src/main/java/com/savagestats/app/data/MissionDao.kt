package com.savagestats.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission)

    @Update
    suspend fun updateMission(mission: Mission)

    @Query("SELECT * FROM missions WHERE isCompleted = 0 AND expiresAt > :now ORDER BY expiresAt ASC")
    fun getActiveMissions(now: Long): Flow<List<Mission>>

    @Query("SELECT * FROM missions WHERE isCompleted = 1 ORDER BY expiresAt DESC")
    fun getCompletedMissions(): Flow<List<Mission>>

    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionById(id: Int): Mission?

    @Query("SELECT COUNT(*) FROM missions WHERE isCompleted = 0 AND expiresAt < :now")
    fun getFailedMissionsCount(now: Long): Flow<Int>

    @Query("SELECT * FROM missions ORDER BY expiresAt DESC")
    fun getAllMissions(): Flow<List<Mission>>
}
