package com.photomigrate.app.util

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

object MediaAnalyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    /**
     * Analyzes an image and returns the most confident label (e.g., "Nature", "Pets").
     * Returns null if no high-confidence label is found.
     */
    suspend fun analyzeImageContent(file: File): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val image = InputImage.fromBitmap(bitmap, 0)
            
            val labels = labeler.process(image).await()
            bitmap.recycle()

            // Map ML Kit generic labels to more useful album names if needed
            val topLabel = labels.maxByOrNull { it.confidence }
            
            topLabel?.text?.let { 
                mapLabelToAlbumName(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLabelToAlbumName(label: String): String {
        return when (label.lowercase()) {
            "cat", "dog", "animal", "pet" -> "Pets"
            "mountain", "tree", "forest", "nature", "landscape" -> "Nature"
            "beach", "sea", "ocean", "water" -> "Beach & Water"
            "city", "building", "architecture", "street" -> "City Life"
            "food", "dish", "cuisine" -> "Food & Dining"
            "person", "human", "people", "face" -> "People"
            else -> label.replaceFirstChar { it.uppercase() }
        }
    }
}
