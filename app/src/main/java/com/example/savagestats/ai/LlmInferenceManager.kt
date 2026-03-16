package com.example.savagestats.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LlmInferenceManager {

    sealed class ModelLoadStatus {
        data object Uninitialized : ModelLoadStatus()
        data object Loading : ModelLoadStatus()
        data object Ready : ModelLoadStatus()
        data class Error(val message: String) : ModelLoadStatus()
    }

    private val _loadStatus = MutableStateFlow<ModelLoadStatus>(ModelLoadStatus.Uninitialized)
    val loadStatus: StateFlow<ModelLoadStatus> = _loadStatus.asStateFlow()

    private var llmInference: LlmInference? = null

    fun initialize(context: Context) {
        if (_loadStatus.value is ModelLoadStatus.Loading ||
            _loadStatus.value is ModelLoadStatus.Ready
        ) return

        _loadStatus.value = ModelLoadStatus.Loading

        try {
            val modelPath = context.filesDir.absolutePath + "/gemma-2b.bin"
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            _loadStatus.value = ModelLoadStatus.Ready
        } catch (e: Exception) {
            _loadStatus.value = ModelLoadStatus.Error(e.message ?: "Unknown error during model loading")
        }
    }

    suspend fun generateSavageResponse(prompt: String): String {
        val inference = llmInference
            ?: return "Model not loaded. Current status: ${_loadStatus.value}"

        return withContext(Dispatchers.IO) {
            try {
                inference.generateResponse(prompt)
            } catch (e: Exception) {
                "Inference failed: ${e.message}"
            }
        }
    }
}
