package com.savagestats.app.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LlmInferenceManager {
    private val TAG = "SavageAI"
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    sealed class ModelLoadStatus {
        data object Uninitialized : ModelLoadStatus()
        data object Loading : ModelLoadStatus()
        data object Ready : ModelLoadStatus()
        data class Error(val message: String) : ModelLoadStatus()
        data object NeedsDownload : ModelLoadStatus()  // New: model file missing
    }

    private val _loadStatus = MutableStateFlow<ModelLoadStatus>(ModelLoadStatus.Uninitialized)
    val loadStatus: StateFlow<ModelLoadStatus> = _loadStatus.asStateFlow()

    private val _engineType = MutableStateFlow<EngineType?>(null)
    val engineType: StateFlow<EngineType?> = _engineType.asStateFlow()

    private var llmInference: LlmInference? = null

    fun initialize(context: Context) {
        if (_loadStatus.value is ModelLoadStatus.Loading ||
            _loadStatus.value is ModelLoadStatus.Ready
        ) return

        managerScope.launch {
            _loadStatus.value = ModelLoadStatus.Loading

            // Step 1: Check hardware support
            val engine = AIEngineRouter.checkHardwareSupport()
            _engineType.value = engine
            Log.d(TAG, "Selected engine: $engine")

            withContext(Dispatchers.IO) {
                try {
                    when (engine) {
                        EngineType.NATIVE_NANO -> {
                            // Future: Use ML Kit Gemini Nano when available
                            Log.d(TAG, "Native Nano not yet implemented, falling back to Gemma")
                            // For now, treat as LOCAL_GEMMA_FALLBACK
                            _engineType.value = EngineType.LOCAL_GEMMA_FALLBACK
                            
                            val modelFile = File(context.filesDir, "gemma3-1b-it-int4.task")
                            if (!modelFile.exists()) {
                                Log.d(TAG, "Model file not found, needs download")
                                withContext(Dispatchers.Main) {
                                    _loadStatus.value = ModelLoadStatus.NeedsDownload
                                }
                                return@withContext
                            }
                            
                            if (modelFile.length() < 300_000_000L) {
                                throw IllegalStateException("Model file is corrupt or incomplete (${modelFile.length() / (1024 * 1024)} MB). Expected 528MB.")
                            }
                            
                            Log.d(TAG, "Loading Gemma 3 from: ${modelFile.absolutePath} (${modelFile.length() / (1024 * 1024)} MB)")

                            val options = LlmInference.LlmInferenceOptions.builder()
                                .setModelPath(modelFile.absolutePath)
                                .setMaxTokens(1024)
                                .build()

                            llmInference = LlmInference.createFromOptions(context, options)
                            Log.d(TAG, "Gemma 3 loaded successfully")

                            withContext(Dispatchers.Main) {
                                _loadStatus.value = ModelLoadStatus.Ready
                            }
                        }
                        EngineType.LOCAL_GEMMA_FALLBACK -> {
                            // Use downloaded Gemma 3 model
                            val modelFile = File(context.filesDir, "gemma3-1b-it-int4.task")
                            
                            // Check if model needs download
                            if (!modelFile.exists()) {
                                Log.d(TAG, "Model file not found, needs download")
                                withContext(Dispatchers.Main) {
                                    _loadStatus.value = ModelLoadStatus.NeedsDownload
                                }
                                return@withContext
                            }
                            
                            if (modelFile.length() < 300_000_000L) {
                                throw IllegalStateException("Model file is corrupt or incomplete (${modelFile.length() / (1024 * 1024)} MB). Expected 528MB.")
                            }
                            
                            Log.d(TAG, "Loading Gemma 3 from: ${modelFile.absolutePath} (${modelFile.length() / (1024 * 1024)} MB)")

                            val options = LlmInference.LlmInferenceOptions.builder()
                                .setModelPath(modelFile.absolutePath)
                                .setMaxTokens(1024)
                                .build()

                            llmInference = LlmInference.createFromOptions(context, options)
                            Log.d(TAG, "Gemma 3 loaded successfully")

                            withContext(Dispatchers.Main) {
                                _loadStatus.value = ModelLoadStatus.Ready
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "CRITICAL: Failed to initialize AI engine", e)
                    withContext(Dispatchers.Main) {
                        _loadStatus.value = ModelLoadStatus.Error(e.message ?: "Unknown error during model loading")
                    }
                }
            }
        }
    }

    suspend fun generateSavageResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                when (_engineType.value) {
                    EngineType.NATIVE_NANO -> {
                        // Future: Use native Gemini Nano when ML Kit API is available
                        // For now, fall through to LOCAL_GEMMA_FALLBACK
                        val inference = llmInference
                            ?: return@withContext "Model not loaded. Status: ${_loadStatus.value}"
                        inference.generateResponse(prompt)
                    }
                    EngineType.LOCAL_GEMMA_FALLBACK -> {
                        val inference = llmInference
                            ?: return@withContext "Gemma model not loaded. Status: ${_loadStatus.value}"
                        
                        inference.generateResponse(prompt)
                    }
                    null -> "Engine not initialized. Status: ${_loadStatus.value}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                "Inference failed: ${e.message}"
            }
        }
    }

    fun isModelDownloaded(context: Context): Boolean {
        val modelFile = File(context.filesDir, "gemma3-1b-it-int4.task")
        return modelFile.exists() && modelFile.length() >= 300_000_000L
    }
}
