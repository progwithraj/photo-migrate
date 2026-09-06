package com.photomigrate.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.photomigrate.app.data.api.TelegramService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class TelegramPartMessage(
    val messageId: Long,
    val fileId: String,
    val fileName: String,
    val partIndex: Int,
    val totalParts: Int,
    val fileSize: Long
)

class TelegramStitcher(private val context: Context) {

    private val telegramService = TelegramService()
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Scans recent bot messages to discover unmerged split parts in a Telegram chat.
     */
    suspend fun discoverParts(botToken: String, chatId: String): Map<String, List<TelegramPartMessage>> = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/getUpdates?limit=100"
        val request = Request.Builder().url(url).get().build()
        val resultMap = mutableMapOf<String, MutableList<TelegramPartMessage>>()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyMap()

            val json = gson.fromJson(response.body?.string(), Map::class.java)
            val updates = json["result"] as? List<Map<*, *>> ?: emptyList()

            for (update in updates) {
                val message = update["message"] as? Map<*, *> ?: continue
                val msgChat = message["chat"] as? Map<*, *>
                val msgChatId = when (val cId = msgChat?.get("id")) {
                    is Double -> cId.toLong().toString()
                    is String -> cId
                    else -> continue
                }

                if (msgChatId != chatId) continue

                val msgId = (message["message_id"] as? Double)?.toLong() ?: continue
                val document = message["document"] as? Map<*, *> ?: message["video"] as? Map<*, *> ?: continue
                
                val fileId = document["file_id"] as? String ?: continue
                val fileName = document["file_name"] as? String ?: "file.part001"
                val fileSize = (document["file_size"] as? Double)?.toLong() ?: 0L
                val caption = message["caption"] as? String ?: ""

                // Check for .part file extension or Part X of Y caption
                var baseName = ""
                var partIdx = 1
                var totalParts = 1

                val captionMatch = Regex("""Part (\d+) of (\d+)""").find(caption)
                val partMatch = Regex("""(.+)\.part(\d+)$""").find(fileName)

                if (captionMatch != null) {
                    partIdx = captionMatch.groupValues[1].toIntOrNull() ?: 1
                    totalParts = captionMatch.groupValues[2].toIntOrNull() ?: 1
                    baseName = caption.substringBefore("\nPart").replace("Migrated: ", "").replace("Migrated (MTProto Pro): ", "").trim()
                } else if (partMatch != null) {
                    baseName = partMatch.groupValues[1]
                    partIdx = partMatch.groupValues[2].toIntOrNull() ?: 1
                } else {
                    continue // Not a split part
                }

                if (baseName.isEmpty()) baseName = fileName.substringBefore(".part")

                val partMsg = TelegramPartMessage(msgId, fileId, fileName, partIdx, totalParts, fileSize)
                resultMap.getOrPut(baseName) { mutableListOf() }.add(partMsg)
            }
        } catch (e: Exception) {
            Log.e("TelegramStitcher", "Error scanning updates: ${e.message}")
        }

        resultMap
    }

    /**
     * Stitches split parts into a single video file in the phone's Download/PhotoMigrate folder.
     */
    suspend fun stitchAndMerge(
        botToken: String,
        chatId: String,
        baseName: String,
        parts: List<TelegramPartMessage>,
        deleteFromTelegram: Boolean = true,
        onLog: (String) -> Unit,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val sortedParts = parts.sortedBy { it.partIndex }
        val expectedTotal = sortedParts.firstOrNull()?.totalParts ?: sortedParts.size

        // SAFETY CHECK: Ensure ALL parts exist
        if (sortedParts.size < expectedTotal) {
            onLog("⚠️ STITCH HALTED: Received ${sortedParts.size} of $expectedTotal parts for '$baseName'. Waiting for remaining parts.")
            return@withContext false
        }

        onLog("⚡ SAFETY VERIFIED: All $expectedTotal parts present for '$baseName'. Starting download...")

        val tempDir = File(context.cacheDir, "stitch_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val downloadedPartFiles = mutableListOf<File>()

        try {
            // 1. Download each part
            for ((idx, part) in sortedParts.withIndex()) {
                onLog("⏬ Downloading part ${idx + 1}/$expectedTotal (${part.fileName})...")
                val filePath = telegramService.getFilePath(botToken, part.fileId)
                if (filePath == null) {
                    onLog("❌ Error: Could not resolve download URL for part ${part.partIndex}.")
                    tempDir.deleteRecursively()
                    return@withContext false
                }

                val tempPartFile = File(tempDir, part.fileName)
                val downloaded = telegramService.downloadTelegramFile(botToken, filePath, tempPartFile) { read, total ->
                    val partProgress = (idx.toFloat() + (read.toFloat() / total.toFloat())) / expectedTotal
                    onProgress(partProgress)
                }

                if (!downloaded) {
                    onLog("❌ Error: Failed downloading part ${part.partIndex}.")
                    tempDir.deleteRecursively()
                    return@withContext false
                }
                downloadedPartFiles.add(tempPartFile)
            }

            // 2. Stitch parts into phone's Download/PhotoMigrate folder
            val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PhotoMigrate").apply {
                if (!exists()) mkdirs()
            }
            val finalOutputFile = File(outputDir, baseName)
            if (finalOutputFile.exists()) finalOutputFile.delete()

            onLog("🧩 Stream-concatenating $expectedTotal parts into '${finalOutputFile.name}'...")

            val buffer = ByteArray(65536)
            FileOutputStream(finalOutputFile).use { output ->
                for (partFile in downloadedPartFiles) {
                    FileInputStream(partFile).use { input ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                output.flush()
            }

            val finalSizeMb = String.format(java.util.Locale.US, "%.1f", finalOutputFile.length() / 1024.0 / 1024.0)
            onLog("✅ SUCCESS: Saved unified '$baseName' ($finalSizeMb MB) to Downloads/PhotoMigrate!")

            // 3. Delete original .part messages from Telegram if requested
            if (deleteFromTelegram) {
                onLog("🧹 Purging $expectedTotal original .part messages from Telegram chat...")
                for (part in sortedParts) {
                    telegramService.deleteMessage(botToken, chatId, part.messageId)
                }
                onLog("✨ Telegram chat cleaned!")
            }

            // 4. Clean up local temp files
            tempDir.deleteRecursively()
            onProgress(1f)
            true
        } catch (e: Exception) {
            onLog("❌ Critical Stitch Error: ${e.message}")
            tempDir.deleteRecursively()
            false
        }
    }
}
