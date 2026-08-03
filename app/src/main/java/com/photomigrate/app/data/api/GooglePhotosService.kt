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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GooglePhotosService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
        val urlBuilder = StringBuilder("$PHOTOS_BASE_URL/mediaItems?pageSize=$pageSize")
        if (!pageToken.isNullOrEmpty()) {
            urlBuilder.append("&pageToken=$pageToken")
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
                
                // If it's a 400 error with "Invalid resume token", it means the pageToken is stale or invalid.
                // We should stop trying to use this token and fallback to fresh Drive listing if starting a page.
                if (response.code == 400 && responseBody.contains("Invalid resume token")) {
                    Log.e("GooglePhotosService", "Detected stale resume token. Stopping Photos API paging.")
                    return Pair(emptyList(), null)
                }

                // Fallback to Drive files search if photos API requires scope check or fails
                // BUT: Photos tokens are NOT compatible with Drive. If we have a token, we can't fallback 
                // Mid-stream. We only fallback if we are starting fresh or Photos is totally disabled.
                return if (pageToken == null) {
                    listDrivePhotos(account, pageSize, null)
                } else {
                    Pair(emptyList(), null)
                }
            }

            val json = gson.fromJson(responseBody, Map::class.java)
            val rawItems = json["mediaItems"] as? List<Map<*, *>> ?: emptyList()
            Log.d("GooglePhotosService", "Found ${rawItems.size} items in Photos API page. Total JSON keys: ${json.keys}")
            val nextPageToken = json["nextPageToken"] as? String

            val mediaItems = rawItems.mapNotNull { itemMap ->
                val id = itemMap["id"] as? String ?: return@mapNotNull null
                val filename = itemMap["filename"] as? String ?: "photo_$id.jpg"
                val mimeType = itemMap["mimeType"] as? String ?: "image/jpeg"
                val baseUrl = itemMap["baseUrl"] as? String ?: ""
                val mediaMetadata = itemMap["mediaMetadata"] as? Map<*, *>
                val creationTime = mediaMetadata?.get("creationTime") as? String ?: ""
                val width = (mediaMetadata?.get("width") as? String)?.toIntOrNull() ?: 0
                val height = (mediaMetadata?.get("height") as? String)?.toIntOrNull() ?: 0

                MediaItem(
                    id = id,
                    filename = filename,
                    mimeType = mimeType,
                    sizeBytes = 0L, 
                    baseUrl = baseUrl,
                    thumbnailUrl = if (baseUrl.isNotEmpty()) "$baseUrl=w256-h256" else "",
                    creationTime = creationTime,
                    width = width,
                    height = height,
                    accountId = account.id
                )
            }

            return Pair(mediaItems, nextPageToken)
        } catch (e: Exception) {
            return Pair(emptyList(), null)
        }
    }

    fun fetchMediaItemsByIds(account: GoogleAccount, ids: List<String>): List<MediaItem> = runBlocking {
        if (ids.isEmpty()) return@runBlocking emptyList()
        
        Log.d("GooglePhotosService", "Fetching metadata for ${ids.size} items...")
        // Photos API batchGet limit is 50 items per request
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
                    val json = gson.fromJson(response.body?.string(), Map::class.java)
                    val rawResults = json["mediaItemResults"] as? List<Map<*, *>> ?: emptyList()
                    
                    rawResults.forEach { res ->
                        val itemMap = res["mediaItem"] as? Map<*, *> ?: return@forEach
                        val id = itemMap["id"] as? String ?: return@forEach
                        val filename = itemMap["filename"] as? String ?: "photo_$id.jpg"
                        val mimeType = itemMap["mimeType"] as? String ?: "image/jpeg"
                        val baseUrl = itemMap["baseUrl"] as? String ?: ""
                        
                        results.add(MediaItem(
                            id = id,
                            filename = filename,
                            mimeType = mimeType,
                            sizeBytes = 0L,
                            baseUrl = baseUrl,
                            accountId = account.id
                        ))
                    }
                } else {
                    Log.w("GooglePhotosService", "batchGet failed: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("GooglePhotosService", "Error in batchGet: ${e.message}")
            }
        }
        
        // If some items failed (maybe they are from Drive API), 
        // fallback to Drive API search for the missing IDs IN PARALLEL
        if (results.size < ids.size) {
            val foundIds = results.map { it.id }.toSet()
            val remainingIds = ids.filter { it !in foundIds }
            Log.d("GooglePhotosService", "Falling back to Drive API for ${remainingIds.size} items in parallel")
            
            val deferreds = remainingIds.map { id ->
                async(Dispatchers.IO) {
                    fetchSingleDriveItem(account, id)
                }
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
                    accountId = account.id
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback file listing via Google Drive API for images/videos in Google Drive/Photos.
     */
    private fun listDrivePhotos(account: GoogleAccount, pageSize: Int, pageToken: String?): Pair<List<MediaItem>, String?> {
        Log.d("GooglePhotosService", "Listing media items via Drive API fallback")
        val query = "mimeType contains 'image/' or mimeType contains 'video/'"
        val url = "$DRIVE_BASE_URL/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&pageSize=$pageSize&fields=nextPageToken,files(id,name,mimeType,size,createdTime,thumbnailLink)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("GooglePhotosService", "Drive API failed (${response.code})")
                return Pair(emptyList(), null)
            }

            val json = gson.fromJson(response.body?.string(), Map::class.java)
            val rawFiles = json["files"] as? List<Map<*, *>> ?: emptyList()
            Log.d("GooglePhotosService", "Found ${rawFiles.size} items in Drive API")
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

    /**
     * Downloads a media item to temporary local file cache and computes its SHA-256 hash.
     */
    fun downloadToTempFile(account: GoogleAccount, item: MediaItem, onProgress: (Long, Long) -> Unit): Pair<File, String>? {
        val tempDir = File(context.cacheDir, "transfer_cache")
        if (!tempDir.exists()) tempDir.mkdirs()

        val tempFile = File(tempDir, "temp_${System.currentTimeMillis()}_${item.filename}")
        
        // If it's a Photos API item, append =d for full download. 
        // If it's a Drive API item, baseUrl already has alt=media.
        val downloadUrl = if (item.baseUrl.contains("googleusercontent.com") && !item.baseUrl.contains("drive.google.com")) {
            "${item.baseUrl}=d" 
        } else {
            item.baseUrl
        }

        Log.d("GooglePhotosService", "Downloading from: $downloadUrl")
        val requestBuilder = Request.Builder().url(downloadUrl)
        
        // Add auth header for all non-Photos-baseUrl requests (Photos uses token-in-URL usually, but Drive needs header)
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

            val buffer = ByteArray(8192)
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

    /**
     * Uploads media file to destination account's Google Photos library.
     */
    fun uploadMediaToDestination(
        destinationAccount: GoogleAccount,
        file: File,
        filename: String,
        mimeType: String,
        onProgress: (Long, Long) -> Unit
    ): String? {
        // Step 1: Upload raw bytes to uploads endpoint
        val uploadUrl = "$PHOTOS_BASE_URL/uploads"
        val mediaType = mimeType.toMediaType()
        val requestBody = file.asRequestBody(mediaType)

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
            val uploadToken = uploadResp.body?.string()?.trim() ?: ""

            if (!uploadResp.isSuccessful || uploadToken.isEmpty()) {
                // Fallback upload via Drive API if photos upload fails
                return uploadToDrive(destinationAccount, file, filename, mimeType)
            }

            // Step 2: Call batchCreate to attach uploaded bytes to Google Photos library
            val batchUrl = "$PHOTOS_BASE_URL/mediaItems:batchCreate"
            val jsonPayload = mapOf(
                "newMediaItems" to listOf(
                    mapOf(
                        "description" to "Migrated via PhotoMigrate App",
                        "simpleMediaItem" to mapOf(
                            "fileName" to filename,
                            "uploadToken" to uploadToken
                        )
                    )
                )
            )

            val batchRequestBody = gson.toJson(jsonPayload).toRequestBody("application/json".toMediaType())
            val batchRequest = Request.Builder()
                .url(batchUrl)
                .addHeader("Authorization", "Bearer ${destinationAccount.accessToken}")
                .post(batchRequestBody)
                .build()

            val batchResp = client.newCall(batchRequest).execute()
            if (batchResp.isSuccessful) {
                return uploadToken
            }
            return uploadToDrive(destinationAccount, file, filename, mimeType)
        } catch (e: Exception) {
            return uploadToDrive(destinationAccount, file, filename, mimeType)
        }
    }

    /**
     * Fallback Upload via Google Drive API to "PhotoMigrate" Folder
     */
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

    /**
     * Deletes / Trashes item from source account (used in Move Mode to free storage).
     */
    fun deleteFromSourceAccount(account: GoogleAccount, itemId: String): Boolean {
        // Call Google Drive API trash endpoint
        // NOTE: Uses full 'drive' scope for existing files
        val url = "$DRIVE_BASE_URL/files/$itemId"
        val payload = gson.toJson(mapOf("trashed" to true)).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${account.accessToken}")
            .patch(payload)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.e("GooglePhotosService", "Trash failed for $itemId: ${response.code} - ${response.body?.string()}")
            }
            success
        } catch (e: Exception) {
            Log.e("GooglePhotosService", "Trash exception: ${e.message}")
            false
        }
    }
}
