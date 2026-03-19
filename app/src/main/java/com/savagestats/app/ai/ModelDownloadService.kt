package com.savagestats.app.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.savagestats.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ModelDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isDownloading = false

    override fun onCreate() {
        super.onCreate()
        ensureChannelExists()
        startForeground(NOTIFICATION_ID, createNotification("Downloading AI Brain...", 0, true))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isDownloading) return START_STICKY

        isDownloading = true
        serviceScope.launch {
            runCatching { downloadModelWithProgress() }
                .onSuccess { fileBytes ->
                    broadcastCompleted(fileBytes)
                    updateNotification("SYSTEM READY", 100, false)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
                .onFailure { throwable ->
                    broadcastError(throwable.message ?: "Download failed")
                    updateNotification("Download failed", 0, false)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            isDownloading = false
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureChannelExists() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Savage System",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(message: String, progress: Int, ongoing: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(message)
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(message: String, progress: Int, ongoing: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(message, progress, ongoing))
    }

    private fun downloadModelWithProgress(): Long {
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val request = Request.Builder()
            .url(MODEL_URL)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download file: ${response.code}")
            }

            val body = response.body ?: throw IOException("Null body")
            val totalBytes = body.contentLength().coerceAtLeast(1L)
            val finalFile = File(filesDir, MODEL_FILE_NAME)
            val tempFile = File(filesDir, "$MODEL_FILE_NAME.part")

            if (tempFile.exists()) tempFile.delete()

            var bytesCopied = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesCopied += read

                        val progress = ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        updateNotification("Downloading AI Brain...", progress, true)
                        broadcastProgress(progress, bytesCopied, totalBytes)
                    }
                    output.flush()
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                throw IOException("Failed to finalize downloaded model file")
            }

            Log.d(TAG, "Download complete. File size: ${finalFile.length() / (1024 * 1024)} MB")
            return finalFile.length()
        }
    }

    private fun broadcastProgress(progress: Int, downloadedBytes: Long, totalBytes: Long) {
        val intent = Intent(ACTION_DOWNLOAD_PROGRESS).apply {
            setPackage(packageName)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_DOWNLOADED_BYTES, downloadedBytes)
            putExtra(EXTRA_TOTAL_BYTES, totalBytes)
        }
        sendBroadcast(intent)
    }

    private fun broadcastCompleted(fileBytes: Long) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE).apply {
            setPackage(packageName)
            putExtra(EXTRA_FILE_BYTES, fileBytes)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(message: String) {
        val intent = Intent(ACTION_DOWNLOAD_ERROR).apply {
            setPackage(packageName)
            putExtra(EXTRA_ERROR_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_DOWNLOAD_PROGRESS = "com.savagestats.app.action.DOWNLOAD_PROGRESS"
        const val ACTION_DOWNLOAD_COMPLETE = "com.savagestats.app.action.DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_ERROR = "com.savagestats.app.action.DOWNLOAD_ERROR"

        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_DOWNLOADED_BYTES = "extra_downloaded_bytes"
        const val EXTRA_TOTAL_BYTES = "extra_total_bytes"
        const val EXTRA_FILE_BYTES = "extra_file_bytes"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"

        private const val CHANNEL_ID = "savagestats_model_download_service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "ModelDownloadService"
        private const val MODEL_FILE_NAME = "gemma3-1b-it-int4.task"
        private const val BUFFER_SIZE = 64 * 1024  // 64KB buffer for hyper-speed downloads
        private const val MODEL_URL = "https://huggingface.co/Mejji16/SavageStats-Brain-gemma3b/resolve/main/gemma3-1b-it-int4.task"

        fun createStartIntent(context: Context): Intent = Intent(context, ModelDownloadService::class.java)
    }
}
