package com.savagestats.app.ai

import android.os.Build
import android.util.Log

/**
 * Routes AI inference to either:
 * - NATIVE_NANO: Gemini Nano via Android AICore (Pixel 8/9, Galaxy S24/S25)
 * - LOCAL_GEMMA_FALLBACK: Downloaded Gemma 3 model via MediaPipe
 * 
 * Future-ready architecture: When ML Kit GenAI Prompt API becomes available,
 * this router will detect hardware support and use native Gemini Nano.
 */
object AIEngineRouter {
    private const val TAG = "AIEngineRouter"

    suspend fun checkHardwareSupport(): EngineType {
        return try {
            // Check if device is a modern flagship that supports AICore
            val isFlagshipDevice = when {
                // Pixel 8/9 series
                Build.MODEL.contains("Pixel 8", ignoreCase = true) ||
                Build.MODEL.contains("Pixel 9", ignoreCase = true) -> true
                
                // Samsung Galaxy S24/S25 series
                Build.MODEL.contains("SM-S92", ignoreCase = true) ||  // S24 series
                Build.MODEL.contains("SM-S93", ignoreCase = true) -> true  // S25 series
                
                else -> false
            }

            if (isFlagshipDevice) {
                Log.d(TAG, "Flagship device detected: ${Build.MODEL}")
                // TODO: When ML Kit GenAI Prompt API is available, check:
                // val generativeModel = Generation.getClient()
                // if (generativeModel.checkStatus() == FeatureStatus.AVAILABLE) {
                //     return EngineType.NATIVE_NANO
                // }
                Log.d(TAG, "AICore not yet available, using local Gemma fallback")
            } else {
                Log.d(TAG, "Non-flagship device: ${Build.MODEL}, using local Gemma")
            }
            
            EngineType.LOCAL_GEMMA_FALLBACK
        } catch (e: Exception) {
            Log.e(TAG, "Error checking hardware support, falling back to Gemma", e)
            EngineType.LOCAL_GEMMA_FALLBACK
        }
    }
}

enum class EngineType {
    NATIVE_NANO,
    LOCAL_GEMMA_FALLBACK
}
