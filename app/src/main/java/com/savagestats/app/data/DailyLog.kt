package com.savagestats.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float,
    val sodium: Float,
    val foodName: String = "",
    val activityDurationMinutes: Float,
    val activityType: String,
    val sleepHours: Float = 0f,
    val dailySteps: Long = 0L,
    val activeCalories: Float = 0f,
)
