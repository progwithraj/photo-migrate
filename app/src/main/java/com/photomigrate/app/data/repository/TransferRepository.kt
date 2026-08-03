package com.photomigrate.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.photomigrate.app.data.api.GooglePhotosService
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.db.TransferDatabase
import com.photomigrate.app.data.db.TransferredFile
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.SyncStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferLog
import com.photomigrate.app.data.model.TransferMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class TransferRepository(private val context: Context) {

    companion object {
        @Volatile
        private var instance: TransferRepository? = null

        fun getInstance(context: Context): TransferRepository {
            return instance ?: synchronized(this) {
                instance ?: TransferRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val apiService = GooglePhotosService(context)
    private val oauthManager = OAuthManager(context)
    private val db = TransferDatabase.getDatabase(context)
    private val gson = Gson()
    private val hashPrefs = context.getSharedPreferences("photo_migrate_hash_db", Context.MODE_PRIVATE)

    private val _currentJob = MutableStateFlow<TransferJob?>(null)
    val currentJob: StateFlow<TransferJob?> = _currentJob.asStateFlow()

    private val _sourceMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val sourceMediaList: StateFlow<List<MediaItem>> = _sourceMediaList.asStateFlow()

    private val _isLoadingMedia = MutableStateFlow(false)
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia.asStateFlow()

    /**
     * Checks if a photo with sha256 hash has already been transferred to destination account.
     */
    suspend fun isDuplicateHash(destinationAccountId: String, sha256Hash: String): Boolean {
        return db.transferDao().findByHash(destinationAccountId, sha256Hash) != null
    }

    private suspend fun markTransferred(mediaId: String, destinationAccountId: String, sha256Hash: String) {
        db.transferDao().insert(TransferredFile(mediaId, destinationAccountId, sha256Hash))
    }

    /**
     * Load media items from source account. Fetches all pages and filters out already transferred items.
     */
    suspend fun loadSourceMedia(sourceAccount: GoogleAccount, destinationAccount: GoogleAccount? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        _isLoadingMedia.value = true
        try {
            val validAccount = oauthManager.refreshTokenIfNeededSuspend(sourceAccount) ?: sourceAccount
            val transferredIds = if (destinationAccount != null) {
                db.transferDao().getTransferredMediaIds(destinationAccount.id).toSet()
            } else emptySet()

            val allItems = mutableListOf<MediaItem>()
            var nextToken: String? = null
            
            do {
                val (items, token) = apiService.listMediaItems(validAccount, pageSize = 100, pageToken = nextToken)
                // Filter out items that are already transferred
                val filteredItems = items.filter { it.id !in transferredIds }
                allItems.addAll(filteredItems)
                nextToken = token
                
                // Update UI incrementally
                _sourceMediaList.value = allItems.toList()
                
                // Stop after 10,000 items for stability, or when finished
                if (allItems.size >= 10000) break
            } while (nextToken != null)
            
            Log.d("TransferRepository", "Finished loading media. Total items found: ${allItems.size}")
            allItems
        } catch (e: Exception) {
            Log.e("TransferRepository", "Error loading media: ${e.message}", e)
            emptyList()
        } finally {
            _isLoadingMedia.value = false
        }
    }

    /**
     * Starts a new transfer/migration job.
     */
    fun createAndStartJob(
        sourceAccount: GoogleAccount,
        destinationAccount: GoogleAccount,
        mode: TransferMode,
        selectedItems: List<MediaItem>
    ): TransferJob {
        val job = TransferJob(
            sourceAccountId = sourceAccount.id,
            destinationAccountId = destinationAccount.id,
            mode = mode,
            selectedMediaIds = selectedItems.map { it.id },
            totalItems = selectedItems.size,
            totalBytes = selectedItems.sumOf { it.sizeBytes },
            status = JobStatus.RUNNING,
            logs = listOf(TransferLog(message = "Job initialized: ${selectedItems.size} items queued for ${mode.name} mode."))
        )
        _currentJob.value = job
        return job
    }

    private fun updateJob(reducer: (TransferJob) -> TransferJob) {
        val current = _currentJob.value ?: return
        _currentJob.value = reducer(current)
    }

    private fun addLog(message: String, isError: Boolean = false) {
        updateJob { it.copy(logs = it.logs + TransferLog(message = message, isError = isError)) }
    }

    /**
     * Executes the transfer loop item by item.
     */
    suspend fun processNextMediaItem(
        sourceAccount: GoogleAccount,
        destinationAccount: GoogleAccount,
        item: MediaItem,
        mode: TransferMode,
        onProgressUpdate: (TransferJob) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Step 1: Ensure OAuth Tokens are valid
        val validSource = oauthManager.refreshTokenIfNeededSuspend(sourceAccount) ?: sourceAccount
        val validDest = oauthManager.refreshTokenIfNeededSuspend(destinationAccount) ?: destinationAccount

        val startTime = System.currentTimeMillis()
        addLog("Downloading '${item.filename}'...")
        _currentJob.value?.let { onProgressUpdate(it) }

        // Step 2: Download file to temp cache & compute SHA-256 hash
        val downloadResult = apiService.downloadToTempFile(validSource, item) { readBytes, totalBytes ->
            // Progress callback during download
        }

        if (downloadResult == null) {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to download media file from source account."
            updateJob { it.copy(failedItems = it.failedItems + 1) }
            addLog("ERROR: Failed to download '${item.filename}'", isError = true)
            _currentJob.value?.let { onProgressUpdate(it) }
            return@withContext
        }

        val (tempFile, hash) = downloadResult

        // Step 3: Check Deduplication
        if (isDuplicateHash(validDest.id, hash)) {
            item.status = SyncStatus.COMPLETED
            markTransferred(item.id, validDest.id, hash)
            updateJob { it.copy(completedItems = it.completedItems + 1) }
            addLog("SKIPPED: '${item.filename}' (Duplicate photo already exists in destination account).")
            tempFile.delete()
            _currentJob.value?.let { onProgressUpdate(it) }
            return@withContext
        }

        // Step 4: Upload to Destination
        addLog("Uploading '${item.filename}' to destination account...")
        _currentJob.value?.let { onProgressUpdate(it) }

        val uploadResult = apiService.uploadMediaToDestination(
            destinationAccount = validDest,
            file = tempFile,
            filename = item.filename,
            mimeType = item.mimeType
        ) { uploadedBytes, totalBytes ->
            // Progress callback during upload
        }

        tempFile.delete() // Clean up cache file

        if (uploadResult != null) {
            markTransferred(item.id, validDest.id, hash)
            item.status = SyncStatus.COMPLETED
            
            val fileSize = if (tempFile.length() > 0) tempFile.length() else 1024L * 1024L
            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
            val speed = fileSize / elapsedSec

            updateJob { 
                it.copy(
                    completedItems = it.completedItems + 1,
                    transferredBytes = it.transferredBytes + fileSize,
                    speedBytesPerSec = speed
                )
            }

            addLog("SUCCESS: Uploaded '${item.filename}' to destination account.")

            // Step 5: If MOVE mode, trash from source account to free space
            if (mode == TransferMode.MOVE) {
                addLog("MOVE MODE: Trashing '${item.filename}' from source account to free storage...")
                val deleted = apiService.deleteFromSourceAccount(validSource, item.id)
                if (deleted) {
                    item.status = SyncStatus.TRASHED_FROM_SOURCE
                    addLog("FREED STORAGE: '${item.filename}' moved to Trash in source account.")
                } else {
                    addLog("Notice: Transferred to destination, but could not delete from source account.", isError = true)
                }
            }
        } else {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to upload to destination account."
            updateJob { it.copy(failedItems = it.failedItems + 1) }
            addLog("ERROR: Upload failed for '${item.filename}'", isError = true)
        }

        _currentJob.value?.let { onProgressUpdate(it) }
    }

    fun pauseJob() {
        updateJob { 
            it.copy(
                status = JobStatus.PAUSED,
                logs = it.logs + TransferLog(message = "Transfer job paused by user.")
            )
        }
    }

    fun resumeJob() {
        updateJob { 
            it.copy(
                status = JobStatus.RUNNING,
                logs = it.logs + TransferLog(message = "Transfer job resumed.")
            )
        }
    }

    fun updateJobStatus(status: JobStatus) {
        updateJob { it.copy(status = status) }
    }

    fun forceLog(message: String, isError: Boolean = false) {
        addLog(message, isError)
    }

    suspend fun fetchItemsForWorker(account: GoogleAccount, ids: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        apiService.fetchMediaItemsByIds(account, ids)
    }
}
