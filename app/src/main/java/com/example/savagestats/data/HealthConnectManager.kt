package com.example.savagestats.data

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthConnectManager(context: Context) {

    private val healthConnectClient: HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    private val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    val requiredPermissions: Set<String>
        get() = permissions

    suspend fun checkPermissionsAndRun(
        permissionLauncher: ActivityResultLauncher<Set<String>>,
        onGranted: suspend () -> Unit
    ) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(permissions)) {
            onGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    suspend fun fetchYesterdayData(): DailyLog? {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofHours(24))

        val exerciseRecords = healthConnectClient
            .readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            .records

        val sleepRecords = healthConnectClient
            .readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            .records

        if (exerciseRecords.isEmpty() && sleepRecords.isEmpty()) {
            return null
        }

        val exerciseMinutes = exerciseRecords
            .sumOf { record -> Duration.between(record.startTime, record.endTime).toMinutes() }
            .toFloat()

        val sleepHours = sleepRecords
            .sumOf { record -> Duration.between(record.startTime, record.endTime).toMinutes() }
            .toFloat() / 60f

        val yesterday = LocalDate.now(ZoneId.systemDefault())
            .minusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        return DailyLog(
            date = yesterday,
            proteinGrams = 0f,
            activityDurationMinutes = exerciseMinutes,
            activityType = if (exerciseMinutes > 0f) "Health Connect" else "Rest",
            sleepHours = sleepHours
        )
    }
}
