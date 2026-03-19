package com.savagestats.app.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.savagestats.app.ai.ModelDownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SetupViewModel(private val applicationContext: Context) : ViewModel() {

    sealed class DownloadStatus {
        data object Idle : DownloadStatus()
        data object ModelAlreadyExists : DownloadStatus()
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

    private val modelFileName = "gemma3-1b-it-int4.task"
    private val modelFile: File
        get() = File(applicationContext.filesDir, modelFileName)



    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val serviceEventsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ModelDownloadService.ACTION_DOWNLOAD_PROGRESS -> {
                    val progress = intent.getIntExtra(ModelDownloadService.EXTRA_PROGRESS, 0)
                    val downloadedBytes = intent.getLongExtra(ModelDownloadService.EXTRA_DOWNLOADED_BYTES, 0L)
                    val totalBytes = intent.getLongExtra(ModelDownloadService.EXTRA_TOTAL_BYTES, 0L)

                    _downloadStatus.value = DownloadStatus.Downloading(
                        progress = progress,
                        downloadedMb = downloadedBytes / BYTES_IN_MB,
                        totalMb = totalBytes / BYTES_IN_MB
                    )
                }

                ModelDownloadService.ACTION_DOWNLOAD_COMPLETE -> {
                    val fileBytes = intent.getLongExtra(ModelDownloadService.EXTRA_FILE_BYTES, modelFile.length())
                    // Save completion flag to SharedPreferences
                    prefs.edit().putBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, true).apply()
                    _downloadStatus.value = DownloadStatus.Success(fileBytes / BYTES_IN_MB)
                }

                ModelDownloadService.ACTION_DOWNLOAD_ERROR -> {
                    _downloadStatus.value = DownloadStatus.Failed(
                        intent.getStringExtra(ModelDownloadService.EXTRA_ERROR_MESSAGE) ?: "Download failed"
                    )
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ModelDownloadService.ACTION_DOWNLOAD_PROGRESS)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_COMPLETE)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_ERROR)
        }

        ContextCompat.registerReceiver(
            applicationContext,
            serviceEventsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Check if model already exists on startup - this is the key fix!
        checkModelStatus()
    }

    private fun checkModelStatus() {
        when {
            // First priority: Check SharedPreferences flag (most reliable)
            prefs.getBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, false) && isValidModelFile(modelFile) -> {
                _downloadStatus.value = DownloadStatus.ModelAlreadyExists
            }
            // Second: Check if file exists and is valid
            isValidModelFile(modelFile) -> {
                // File exists but no flag - mark as complete anyway
                prefs.edit().putBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, true).apply()
                _downloadStatus.value = DownloadStatus.ModelAlreadyExists
            }
            // File exists but invalid - delete it
            modelFile.exists() -> {
                modelFile.delete()
                _downloadStatus.value = DownloadStatus.Idle
            }
            else -> {
                _downloadStatus.value = DownloadStatus.Idle
            }
        }
    }

    fun startDownload() {
        if (_downloadStatus.value is DownloadStatus.Downloading || isServiceRunning()) return

        // Re-check file status before starting download
        if (isValidModelFile(modelFile)) {
            prefs.edit().putBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, true).apply()
            val sizeMb = modelFile.length() / BYTES_IN_MB
            _downloadStatus.value = DownloadStatus.Success(sizeMb)
            return
        }

        if (modelFile.exists()) {
            modelFile.delete()
        }

        // Clear the completion flag before starting new download
        prefs.edit().putBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, false).apply()

        _downloadStatus.value = DownloadStatus.Downloading(
            progress = 0,
            downloadedMb = 0,
            totalMb = 0
        )

        startForegroundService(applicationContext, ModelDownloadService.createStartIntent(applicationContext))
    }

    fun modelExists(): Boolean {
        return isValidModelFile(modelFile) && prefs.getBoolean(KEY_MODEL_DOWNLOAD_COMPLETE, false)
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() && file.length() >= MIN_VALID_MODEL_BYTES
    }

    private fun isServiceRunning(): Boolean {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == ModelDownloadService::class.java.name }
    }

    companion object {
        private const val BYTES_IN_MB = 1024L * 1024L
        // Gemma 3B 1B INT4 = 528MB exactly. 300MB floor catches partial/corrupt downloads.
        private const val MIN_VALID_MODEL_BYTES = 300_000_000L
        private const val PREFS_NAME = "savage_setup_prefs"
        private const val KEY_MODEL_DOWNLOAD_COMPLETE = "model_download_complete"
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

    override fun onCleared() {
        applicationContext.unregisterReceiver(serviceEventsReceiver)
        super.onCleared()
    }
}
