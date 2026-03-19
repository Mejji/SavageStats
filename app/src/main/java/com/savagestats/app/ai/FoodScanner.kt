package com.savagestats.app.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

class FoodScanner : AutoCloseable {

    private val localModel = LocalModel.Builder()
        .setAssetFilePath("ml/food-v1.tflite")
        .build()

    private val labeler = ImageLabeling.getClient(
        CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.15f) // Low threshold for 2,000-class model (probability spread)
            .setMaxResultCount(5)
            .build()
    )

    /**
     * Synchronously classify a bitmap and return the top 5 food tags as a
     * comma-joined cluster string (e.g. "Hamburger, Cheeseburger, Sandwich").
     *
     * Uses a custom TFLite food classification model (ml/food-v1.tflite) with
     * a 15% confidence threshold (tuned for 2,000-class probability spread).
     * The top tag is routed to the SQLite RAG pipeline for verified macro lookup.
     *
     * MUST be called from a background thread — [Tasks.await] blocks the caller.
     */
    fun scanImage(bitmap: Bitmap): String {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val labels = Tasks.await(labeler.process(inputImage))

        if (labels.isEmpty()) {
            Log.d(TAG, "No labels detected")
            return ""
        }

        // Neural X-Ray: dump the model's raw thinking to Logcat for calibration
        for (label in labels) {
            Log.d(TAG, "AI Saw: ${label.text} (Confidence: ${"%.1f".format(label.confidence * 100)}%)")
        }

        val tagCluster = labels.take(5).joinToString(", ") { it.text }
        Log.d(TAG, "Tag cluster: $tagCluster")
        return tagCluster
    }

    override fun close() {
        labeler.close()
    }

    companion object {
        private const val TAG = "FoodScanner"
    }
}
