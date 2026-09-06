package com.photomigrate.app.data.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GooglePhotosService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    private val gson = Gson()

    companion object {
        const val PHOTOS_BASE_URL = "https://photoslibrary.googleapis.com/v1"
        const val DRIVE_BASE_URL = "https://www.googleapis.com/drive/v3"
    }

    /**
     * Lists media items (photos & videos) from Google Photos library.
     */
    fun listMediaItems(account: GoogleAccount, pageSize: Int = 100, pageToken: String? = null): Pair<List<MediaItem>, String?> {
        Log.d("GooglePhotosService", "Listing media items for ${account.email}")
        
        val isDriveToken = pageToken?.startsWith("DRIVE|") == true
        val actualToken = when {
            pageToken == null -> null
            isDriveToken -> pageToken.removePrefix("DRIVE|").let { if (it == "START") null else it }
            pageToken.startsWith("PHOTOS|") -> pageToken.removePrefix("PHOTOS|")
            else -> pageToken // Legacy support
        }
        
        if (isDriveToken) {
            val (items, nextToken) = listDrivePhotos(account, pageSize, actualToken)
            return Pair(items, nextToken?.let { "DRIVE|$it" })
        }

        val urlBuilder = StringBuilder("$PHOTOS_BASE_URL/mediaItems?pageSize=$pageSize")
        if (!actualToken.isNullOrEmpty()) {
            urlBuilder.append("&pageToken=$actualToken")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("GooglePhotosService", "Photos API failed (${response.code}): $responseBody")
                
                if (response.code == 400 && responseBody.contains("Invalid resume token")) {
                    Log.e("GooglePhotosService", "Detected stale resume token. Stopping Photos API paging.")
                    return Pair(emptyList(), null)
                }

                return if (pageToken == null) {
                    val (items, nextToken) = listDrivePhotos(account, pageSize, null)
                    Pair(items, nextToken?.let { "DRIVE|$it" })
                } else {
                    Pair(emptyList(), null)
                }
            }

            val json = gson.fromJson(responseBody, Map::class.java)
            val rawItems = json["mediaItems"] as? List<Map<*, *>> ?: emptyList()
            val photosNextToken = json["nextPageToken"] as? String

            val mediaItems = rawItems.mapNotNull { itemMap ->
                val id = itemMap["id"] as? String ?: return@mapNotNull null
                val filename = itemMap["filename"] as? String ?: "photo_$id.jpg"
                val mimeType = itemMap["mimeType"] as? String ?: "image/jpeg"
                val baseUrl = itemMap["baseUrl"] as? String ?: ""
                val mediaMetadata = itemMap["mediaMetadata"] as? Map<*, *>
                val creationTime = mediaMetadata?.get("creationTime") as? String ?: ""

                MediaItem(
                    id = id,
                    filename = filename,
                    mimeType = mimeType,
                    sizeBytes = 0L, 
                    baseUrl = baseUrl,
                    thumbnailUrl = if (baseUrl.isNotEmpty()) "$baseUrl=w256-h256" else "",
                    creationTime = creationTime,
                    accountId = account.id
                )
            }

            // If Photos API reached the end, switch to Drive API to get everything
            val finalToken = if (photosNextToken != null) {
                "PHOTOS|$photosNextToken"
            } else {
                "DRIVE|START"
            }

            return Pair(mediaItems, finalToken)
        } catch (e: Exception) {
            return Pair(emptyList(), null)
        }
    }

    /**
     * Lists media items in a specific album.
     */
    fun listMediaItemsInAlbum(account: GoogleAccount, albumId: String, pageSize: Int = 100, pageToken: String? = null): Pair<List<MediaItem>, String?> {
        val url = "$PHOTOS_BASE_URL/mediaItems:search"
        val payload = mutableMapOf<String, Any>(
            "albumId" to albumId,
            "pageSize" to pageSize
        )
        if (!pageToken.isNullOrEmpty()) {
            payload["pageToken"] = pageToken
        }
        
        val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return Pair(emptyList(), null)

            val json = gson.fromJson(responseBody, Map::class.java)
            val rawItems = json["mediaItems"] as? List<Map<*, *>> ?: emptyList()
            val nextToken = json["nextPageToken"] as? String

            val mediaItems = rawItems.mapNotNull { itemMap ->
                val id = itemMap["id"] as? String ?: return@mapNotNull null
                val filename = itemMap["filename"] as? String ?: "photo_$id.jpg"
                val mimeType = itemMap["mimeType"] as? String ?: "image/jpeg"
                val baseUrl = itemMap["baseUrl"] as? String ?: ""
                val mediaMetadata = itemMap["mediaMetadata"] as? Map<*, *>
                val creationTime = mediaMetadata?.get("creationTime") as? String ?: ""

                MediaItem(
                    id = id,
                    filename = filename,
                    mimeType = mimeType,
                    sizeBytes = 0L, 
                    baseUrl = baseUrl,
                    thumbnailUrl = if (baseUrl.isNotEmpty()) "$baseUrl=w256-h256" else "",
                    creationTime = creationTime,
                    accountId = account.id
                )
            }
            Pair(mediaItems, nextToken)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }

    /**
     * Fetches metadata for specific media items by their IDs.
     */
    suspend fun fetchMediaItemsByIds(account: GoogleAccount, ids: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        
        Log.d("GooglePhotosService", "Fetching metadata for ${ids.size} items...")
        val results = mutableListOf<MediaItem>()
        val chunks = ids.chunked(50)
        
        for (chunk in chunks) {
            val urlBuilder = StringBuilder("$PHOTOS_BASE_URL/mediaItems:batchGet?")
            chunk.forEach { id -> urlBuilder.append("mediaItemIds=$id&") }
            
            val request = Request.Builder()
                .url(urlBuilder.toString().removeSuffix("&"))
                .addHeader("Authorization", "Bearer ${account.accessToken}")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val rawResults = json["mediaItemResults"] as? List<Map<*, *>> ?: emptyList()
                    
                    rawResults.forEach { res ->
                        val itemMap = res["mediaItem"] as? Map<*, *> ?: return@forEach
                        val id = itemMap["id"] as? String ?: return@forEach
                        val filename = itemMap["filename"] as? String ?: "photo_$id.jpg"
                        val mimeType = itemMap["mimeType"] as? String ?: "image/jpeg"
                        val baseUrl = itemMap["baseUrl"] as? String ?: ""
                        val mediaMetadata = itemMap["mediaMetadata"] as? Map<*, *>
                        val creationTime = mediaMetadata?.get("creationTime") as? String ?: ""

                        results.add(MediaItem(
                            id = id,
                            filename = filename,
                            mimeType = mimeType,
                            sizeBytes = 0L,
                            baseUrl = baseUrl,
                            creationTime = creationTime,
                            accountId = account.id
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e("GooglePhotosService", "Error in batchGet: ${e.message}")
            }
        }
        
        if (results.size < ids.size) {
            val foundIds = results.map { it.id }.toSet()
            val remainingIds = ids.filter { it !in foundIds }
            val deferreds = remainingIds.map { id ->
                async { fetchSingleDriveItem(account, id) }
            }
            results.addAll(deferreds.awaitAll().filterNotNull())
        }
        
        results
    }

    private fun fetchSingleDriveItem(account: GoogleAccount, id: String): MediaItem? {
        val url = "$DRIVE_BASE_URL/files/$id?fields=id,name,mimeType,size,createdTime,thumbnailLink"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .get()
            .build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                val file = gson.fromJson(body, Map::class.java)
                MediaItem(
                    id = file["id"] as String,
                    filename = file["name"] as? String ?: "photo.jpg",
                    mimeType = file["mimeType"] as? String ?: "image/jpeg",
                    sizeBytes = (file["size"] as? String)?.toLongOrNull() ?: 0L,
                    baseUrl = "$DRIVE_BASE_URL/files/${file["id"]}?alt=media",
                    thumbnailUrl = file["thumbnailLink"] as? String,
                    creationTime = file["createdTime"] as? String ?: "",
                    accountId = account.id
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun listDrivePhotos(account: GoogleAccount, pageSize: Int, actualToken: String?): Pair<List<MediaItem>, String?> {
        val query = "mimeType contains 'image/' or mimeType contains 'video/'"
        val urlBuilder = StringBuilder("$DRIVE_BASE_URL/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&pageSize=$pageSize&fields=nextPageToken,files(id,name,mimeType,size,createdTime,thumbnailLink)")
        
        if (!actualToken.isNullOrEmpty()) {
            urlBuilder.append("&pageToken=$actualToken")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return Pair(emptyList(), null)

            val json = gson.fromJson(response.body?.string(), Map::class.java)
            val rawFiles = json["files"] as? List<Map<*, *>> ?: emptyList()
            val nextPageToken = json["nextPageToken"] as? String

            val items = rawFiles.mapNotNull { file ->
                val id = file["id"] as? String ?: return@mapNotNull null
                val name = file["name"] as? String ?: "photo_$id.jpg"
                val mimeType = file["mimeType"] as? String ?: "image/jpeg"
                val size = (file["size"] as? String)?.toLongOrNull() ?: 0L
                val createdTime = file["createdTime"] as? String ?: ""
                val thumbnailLink = file["thumbnailLink"] as? String

                MediaItem(
                    id = id,
                    filename = name,
                    mimeType = mimeType,
                    sizeBytes = size,
                    baseUrl = "$DRIVE_BASE_URL/files/$id?alt=media",
                    thumbnailUrl = thumbnailLink,
                    creationTime = createdTime,
                    accountId = account.id
                )
            }

            return Pair(items, nextPageToken)
        } catch (e: Exception) {
            return Pair(emptyList(), null)
        }
    }

    fun downloadToTempFile(account: GoogleAccount, item: MediaItem, onProgress: (Long, Long) -> Unit): Pair<File, String>? {
        val tempDir = File(context.cacheDir, "transfer_cache")
        if (!tempDir.exists()) tempDir.mkdirs()

        val tempFile = File(tempDir, "temp_${System.currentTimeMillis()}_${item.filename}")
        val downloadUrl = if (item.baseUrl.contains("googleusercontent.com") && !item.baseUrl.contains("drive.google.com")) {
            "${item.baseUrl}=d" 
        } else {
            item.baseUrl
        }

        val requestBuilder = Request.Builder().url(downloadUrl)
        if (!downloadUrl.contains("googleusercontent.com") || downloadUrl.contains("googleapis.com")) {
            requestBuilder.addHeader("Authorization", "Bearer ${account.accessToken}")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return null

            val body = response.body ?: return null
            val contentLength = body.contentLength().let { if (it > 0) it else item.sizeBytes }

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)
            val digest = MessageDigest.getInstance("SHA-256")

            val buffer = ByteArray(65536) // Optimized 64KB buffer
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (contentLength > 0) {
                    onProgress(totalRead, contentLength)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            return Pair(tempFile, sha256)
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            return null
        }
    }

    fun uploadMediaToDestination(
        destinationAccount: GoogleAccount,
        file: File,
        filename: String,
        mimeType: String,
        albumId: String? = null,
        onProgress: (Long, Long) -> Unit
    ): String? {
        val uploadUrl = "$PHOTOS_BASE_URL/uploads"
        val mediaType = mimeType.toMediaType()
        
        val requestBody = object : okhttp3.RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength() = file.length()
            override fun writeTo(sink: okio.BufferedSink) {
                file.inputStream().source().use { source ->
                    var totalRead = 0L
                    val buffer = okio.Buffer()
                    var read: Long
                    while (source.read(buffer, 65536L).also { read = it } != -1L) { // Optimized 64KB
                        sink.write(buffer, read)
                        totalRead += read
                        onProgress(totalRead, file.length())
                    }
                }
            }
        }

        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer ${destinationAccount.accessToken}")
            .addHeader("Content-type", "application/octet-stream")
            .addHeader("X-Goog-Upload-File-Name", filename)
            .addHeader("X-Goog-Upload-Protocol", "raw")
            .post(requestBody)
            .build()

        try {
            val uploadResp = client.newCall(uploadRequest).execute()
            val responseBody = uploadResp.body?.string() ?: ""
            val uploadToken = responseBody.trim()

            if (!uploadResp.isSuccessful || uploadToken.isEmpty()) {
                return uploadToDrive(destinationAccount, file, filename, mimeType)
            }

            val batchUrl = "$PHOTOS_BASE_URL/mediaItems:batchCreate"
            val newMediaItem = mapOf(
                "description" to "Migrated via PhotoMigrate App",
                "simpleMediaItem" to mapOf("fileName" to filename, "uploadToken" to uploadToken)
            )
            val payload = mutableMapOf<String, Any>("newMediaItems" to listOf(newMediaItem))
            if (!albumId.isNullOrEmpty()) payload["albumId"] = albumId

            val batchRequestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
            val batchRequest = Request.Builder()
                .url(batchUrl)
                .addHeader("Authorization", "Bearer ${destinationAccount.accessToken}")
                .post(batchRequestBody)
                .build()

            val batchResp = client.newCall(batchRequest).execute()
            if (batchResp.isSuccessful) return uploadToken
            return uploadToDrive(destinationAccount, file, filename, mimeType)
        } catch (e: Exception) {
            return uploadToDrive(destinationAccount, file, filename, mimeType)
        }
    }

    private fun uploadToDrive(destinationAccount: GoogleAccount, file: File, filename: String, mimeType: String): String? {
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val requestBody = file.asRequestBody(mimeType.toMediaType())
        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer ${destinationAccount.accessToken}")
            .post(requestBody)
            .build()
        return try {
            val resp = client.newCall(request).execute()
            if (resp.isSuccessful) "drive_upload_success" else null
        } catch (e: Exception) {
            null
        }
    }

    fun createAlbum(account: GoogleAccount, title: String): String? {
        val url = "$PHOTOS_BASE_URL/albums"
        val payload = mapOf("album" to mapOf("title" to title))
        val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer ${account.accessToken}").post(body).build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                json["id"] as? String
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun listAlbums(account: GoogleAccount): List<Pair<String, String>> {
        val url = "$PHOTOS_BASE_URL/albums?pageSize=50"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer ${account.accessToken}").get().build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                val rawAlbums = json["albums"] as? List<Map<*, *>> ?: emptyList()
                rawAlbums.mapNotNull { 
                    val id = it["id"] as? String
                    val title = it["title"] as? String
                    if (id != null && title != null) id to title else null
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteFromSourceAccount(account: GoogleAccount, itemId: String, filename: String? = null): Boolean {
        if (tryTrash(account, itemId)) return true
        if (!filename.isNullOrEmpty()) {
            val resolvedId = findFileIdByName(account, filename)
            if (resolvedId != null && resolvedId != itemId) return tryTrash(account, resolvedId)
        }
        return false
    }

    private fun tryTrash(account: GoogleAccount, id: String): Boolean {
        val url = "$DRIVE_BASE_URL/files/$id?supportsAllDrives=true"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .patch(gson.toJson(mapOf("trashed" to true)).toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun findFileIdByName(account: GoogleAccount, filename: String): String? {
        val query = "name = '${filename.replace("'", "\\'")}' and trashed = false"
        val url = "$DRIVE_BASE_URL/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&pageSize=1&fields=files(id)"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer ${account.accessToken}").get().build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), Map::class.java)
                val files = json["files"] as? List<Map<*, *>>
                files?.firstOrNull()?.get("id") as? String
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
