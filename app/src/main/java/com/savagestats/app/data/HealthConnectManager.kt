package com.savagestats.app.data

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext

    // Lazy init: only create the client when Health Connect is actually available.
    // Calling getOrCreate() when HC is not installed / needs update throws an exception.
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(appContext)
    }

    private val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    val requiredPermissions: Set<String>
        get() = permissions

    /** Returns the raw SDK status so callers can distinguish "not installed" vs "needs update". */
    fun getSdkStatus(): Int =
        HealthConnectClient.getSdkStatus(appContext, HEALTH_CONNECT_PACKAGE_NAME)

    /**
     * Android 14+ (API 34): Health Connect is a **framework module** baked into the OS.
     * The legacy Play Store package [HEALTH_CONNECT_PACKAGE_NAME] may be absent or stale,
     * causing [getSdkStatus] to return [HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED]
     * even though the framework HC is fully functional. Treating anything except
     * [HealthConnectClient.SDK_UNAVAILABLE] as "available" on API 34+ is the correct behaviour.
     *
     * Android 13 and below: HC is a Play Store APK, so only [HealthConnectClient.SDK_AVAILABLE]
     * means it is truly ready.
     */
    fun isHealthConnectAvailable(): Boolean {
        val status = getSdkStatus()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: SDK_AVAILABLE or SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED → framework is there
            status != HealthConnectClient.SDK_UNAVAILABLE
        } else {
            status == HealthConnectClient.SDK_AVAILABLE
        }
    }

    /** True when Health Connect is present but the provider package needs updating. */
    fun isHealthConnectUpdateRequired(): Boolean =
        getSdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    suspend fun checkPermissionsAndRun(
        permissionLauncher: ActivityResultLauncher<Set<String>>,
        onGranted: suspend () -> Unit
    ) {
        if (!isHealthConnectAvailable()) return
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(permissions)) {
            onGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        if (!isHealthConnectAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun syncDailyMetrics(): DailyMetrics {
        val zoneId = ZoneId.systemDefault()
        val startOfToday = LocalDateTime.now(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
        val now = Instant.now()

        val aggregateRequest = AggregateRequest(
            metrics = setOf(
                StepsRecord.COUNT_TOTAL,
                ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
            ),
            timeRangeFilter = TimeRangeFilter.between(startOfToday, now)
        )

        val response = healthConnectClient.aggregate(aggregateRequest)
        val totalSteps = response[StepsRecord.COUNT_TOTAL]?.toLong() ?: 0L
        val activeCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toFloat() ?: 0f

        return DailyMetrics(
            totalSteps = totalSteps,
            activeCalories = activeCalories,
        )
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
            protein = 0f,
            carbs = 0f,
            fats = 0f,
            fiber = 0f,
            sodium = 0f,
            foodName = "",
            activityDurationMinutes = exerciseMinutes,
            activityType = if (exerciseMinutes > 0f) "Health Connect" else "Rest",
            sleepHours = sleepHours,
            dailySteps = 0L,
            activeCalories = 0f,
        )
    }

    data class DailyMetrics(
        val totalSteps: Long,
        val activeCalories: Float,
    )

    companion object {
        private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
}
