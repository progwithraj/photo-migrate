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
