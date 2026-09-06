package com.photomigrate.app.data.api

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class TelegramService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Verifies the bot token and returns the bot's username.
     */
    fun verifyBot(token: String): String? {
        val url = "https://api.telegram.org/bot$token/getMe"
        val request = Request.Builder().url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                val ok = json["ok"] as? Boolean ?: false
                if (ok) {
                    val result = json["result"] as? Map<*, *>
                    result?.get("username") as? String
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Polls getUpdates to find the most recent chat ID interacting with the bot.
     */
    fun getLatestChatId(token: String): String? {
        val url = "https://api.telegram.org/bot$token/getUpdates?limit=1&offset=-1"
        val request = Request.Builder().url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                val ok = json["ok"] as? Boolean ?: false
                if (ok) {
                    val resultList = json["result"] as? List<Map<*, *>>
                    if (!resultList.isNullOrEmpty()) {
                        val update = resultList.last()
                        val message = update["message"] as? Map<*, *>
                        val chat = message?.get("chat") as? Map<*, *>
                        val chatId = chat?.get("id")
                        // Chat ID can be Long, Gson parses numbers as Double by default
                        when (chatId) {
                            is Double -> chatId.toLong().toString()
                            is String -> chatId
                            else -> null
                        }
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a specific message from a Telegram chat/channel.
     */
    fun deleteMessage(token: String, chatId: String, messageId: Long): Boolean {
        val url = "https://api.telegram.org/bot$token/deleteMessage?chat_id=$chatId&message_id=$messageId"
        val request = Request.Builder().url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetches file_path for a Telegram file_id.
     */
    fun getFilePath(token: String, fileId: String): String? {
        val url = "https://api.telegram.org/bot$token/getFile?file_id=$fileId"
        val request = Request.Builder().url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                val ok = json["ok"] as? Boolean ?: false
                if (ok) {
                    val result = json["result"] as? Map<*, *>
                    result?.get("file_path") as? String
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads a file directly from Telegram servers to a local destination file.
     */
    fun downloadTelegramFile(
        token: String,
        filePath: String,
        destFile: File,
        onProgress: (Long, Long) -> Unit
    ): Boolean {
        val url = "https://api.telegram.org/file/bot$token/$filePath"
        val request = Request.Builder().url(url).get().build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return false

            val body = response.body ?: return false
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalRead, contentLength)
                        }
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            if (destFile.exists()) destFile.delete()
            false
        }
    }

    /**
     * Uploads media to Telegram using multipart/form-data.
     */
    fun uploadMedia(
        token: String,
        chatId: String,
        file: File,
        mimeType: String,
        caption: String? = null
    ): Boolean {
        val isVideo = mimeType.startsWith("video/")
        val endpoint = if (isVideo) "sendVideo" else "sendPhoto"
        val url = "https://api.telegram.org/bot$token/$endpoint"

        val filePart = file.asRequestBody(mimeType.toMediaType())
        
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(if (isVideo) "video" else "photo", file.name, filePart)

        if (!caption.isNullOrEmpty()) {
            builder.addFormDataPart("caption", caption)
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TelegramService", "Upload failed: ${e.message}")
            false
        }
    }
}
