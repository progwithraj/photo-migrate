package com.photomigrate.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object MediaCompressor {

    /**
     * Compresses an image file to save storage.
     * Resizes if dimensions are larger than 3840px (4K).
     * Saves as WebP (lossy) at 80% quality.
     */
    fun compressImage(context: Context, inputFile: File, mimeType: String): File? {
        if (!mimeType.startsWith("image/")) return inputFile

        val outputDir = File(context.cacheDir, "compressed_cache")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val outputFile = File(outputDir, "compressed_${inputFile.name}")
        
        try {
            // 1. Decode dimensions first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputFile.absolutePath, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) return inputFile

            // 2. Determine scaling factor if larger than 4K
            val maxDimension = 3840
            var sampleSize = 1
            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                val halfWidth = originalWidth / 2
                val halfHeight = originalHeight / 2
                while (halfWidth / sampleSize >= maxDimension || halfHeight / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // 3. Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions) ?: return inputFile

            // 4. Correct orientation from EXIF
            val rotatedBitmap = rotateBitmapIfNeeded(inputFile, bitmap)

            // 5. Save to WebP
            val out = FileOutputStream(outputFile)
            val success = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                rotatedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
            } else {
                @Suppress("DEPRECATION")
                rotatedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            }
            out.flush()
            out.close()
            
            if (bitmap != rotatedBitmap) rotatedBitmap.recycle()
            bitmap.recycle()

            return if (success) outputFile else inputFile
        } catch (e: Exception) {
            return inputFile
        }
    }

    private fun rotateBitmapIfNeeded(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
