package com.example.savagestats.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.app.NotificationCompat
import com.example.savagestats.MainActivity
import com.example.savagestats.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class SetupViewModel(private val applicationContext: Context) : ViewModel() {

    sealed class DownloadStatus {
        data object Idle : DownloadStatus()
        data class Downloading(
            val progress: Int,
            val downloadedMb: Long,
            val totalMb: Long
        ) : DownloadStatus()
        data class Success(val fileSizeMb: Long) : DownloadStatus()
        data class Failed(val message: String) : DownloadStatus()
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val modelUrl =
        "https://huggingface.co/Mejji16/savagestats-gemma/resolve/main/gemma-2b.bin.bin?download=true"

    private val modelFileName = "gemma-2b.bin"
    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val modelFile: File
        get() = File(applicationContext.filesDir, modelFileName)

    fun startDownload() {
        if (_downloadStatus.value is DownloadStatus.Downloading) return

        createNotificationChannelIfNeeded()

        if (isValidModelFile(modelFile)) {
            val sizeMb = modelFile.length() / BYTES_IN_MB
            _downloadStatus.value = DownloadStatus.Success(sizeMb)
            showCompletionNotification(sizeMb)
            return
        }

        if (modelFile.exists()) {
            modelFile.delete()
        }

        viewModelScope.launch {
            _downloadStatus.value = DownloadStatus.Downloading(
                progress = 0,
                downloadedMb = 0,
                totalMb = 0
            )
            updateProgressNotification(progress = 0, downloadedMb = 0, totalMb = 0)

            try {
                downloadModel()

                if (!isValidModelFile(modelFile)) {
                    throw IllegalStateException("Downloaded file is incomplete or corrupted")
                }

                val sizeMb = modelFile.length() / BYTES_IN_MB
                _downloadStatus.value = DownloadStatus.Success(sizeMb)
                showCompletionNotification(sizeMb)

                delay(250)
            } catch (e: Exception) {
                // Clean up partial file on failure
                if (modelFile.exists()) modelFile.delete()

                _downloadStatus.value = DownloadStatus.Failed(
                    e.message ?: "Download failed"
                )
                showFailedNotification(e.message ?: "Download failed")
            }
        }
    }

    private suspend fun downloadModel() = withContext(Dispatchers.IO) {
        val destinationFile = modelFile
        val url = URL(modelUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP $responseCode")
            }

            val contentLength = connection.contentLengthLong
            var totalBytesRead = 0L

            connection.inputStream.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = if (contentLength > 0) {
                            (totalBytesRead * 100 / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        val downloadedMb = totalBytesRead / BYTES_IN_MB
                        val totalMb = if (contentLength > 0) contentLength / BYTES_IN_MB else 0

                        _downloadStatus.value = DownloadStatus.Downloading(
                            progress = progress,
                            downloadedMb = downloadedMb,
                            totalMb = totalMb
                        )
                        updateProgressNotification(
                            progress = progress,
                            downloadedMb = downloadedMb,
                            totalMb = totalMb
                        )
                    }
                    outputStream.flush()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun modelExists(): Boolean {
        return isValidModelFile(modelFile)
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() && file.length() >= MIN_VALID_MODEL_BYTES
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks AI model download progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateProgressNotification(progress: Int, downloadedMb: Long, totalMb: Long) {
        val progressText = if (totalMb > 0) {
            "$downloadedMb MB / $totalMb MB"
        } else {
            "$downloadedMb MB downloaded"
        }

        notificationManager.notify(
            DOWNLOAD_NOTIFICATION_ID,
            baseNotificationBuilder()
                .setContentTitle("Downloading Savage Coach Model")
                .setContentText("$progress% • $progressText")
                .setProgress(100, progress, totalMb == 0L)
                .setOngoing(true)
                .build()
        )
    }

    private fun showCompletionNotification(fileSizeMb: Long) {
        notificationManager.notify(
            DOWNLOAD_NOTIFICATION_ID,
            baseNotificationBuilder()
                .setContentTitle("Model Download Complete")
                .setContentText("$fileSizeMb MB verified. Tap to open SavageStats")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun showFailedNotification(message: String) {
        notificationManager.notify(
            DOWNLOAD_NOTIFICATION_ID,
            baseNotificationBuilder()
                .setContentTitle("Model Download Failed")
                .setContentText(message)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun baseNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .setContentIntent(createMainActivityPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_PROGRESS)
    }

    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val BYTES_IN_MB = 1024L * 1024L
        private const val MIN_VALID_MODEL_BYTES = 1_300_000_000L
        private const val DOWNLOAD_CHANNEL_ID = "savagestats_model_download"
        private const val DOWNLOAD_NOTIFICATION_ID = 10011
    }

    class Factory(private val applicationContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
                return SetupViewModel(applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
