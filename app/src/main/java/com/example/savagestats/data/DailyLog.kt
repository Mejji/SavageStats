package com.example.savagestats.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val proteinGrams: Float,
    val activityDurationMinutes: Float,
    val activityType: String,
    val sleepHours: Float = 0f
)
