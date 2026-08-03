package com.photomigrate.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.photomigrate.app.data.api.GooglePhotosService
import com.photomigrate.app.data.auth.OAuthManager
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
    fun isDuplicateHash(destinationAccountId: String, sha256Hash: String): Boolean {
        val key = "${destinationAccountId}_$sha256Hash"
        return hashPrefs.getBoolean(key, false)
    }

    private fun markHashTransferred(destinationAccountId: String, sha256Hash: String) {
        val key = "${destinationAccountId}_$sha256Hash"
        hashPrefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Load media items from source account.
     */
    suspend fun loadSourceMedia(sourceAccount: GoogleAccount): List<MediaItem> = withContext(Dispatchers.IO) {
        _isLoadingMedia.value = true
        try {
            val (items, _) = apiService.listMediaItems(sourceAccount)
            _sourceMediaList.value = items
            items
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
            status = JobStatus.RUNNING
        )
        job.logs.add(TransferLog(message = "Job initialized: ${selectedItems.size} items queued for ${mode.name} mode."))
        _currentJob.value = job
        return job
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
        val job = _currentJob.value ?: return@withContext

        // Step 1: Ensure OAuth Tokens are valid
        val validSource = oauthManager.refreshTokenIfNeededSuspend(sourceAccount) ?: sourceAccount
        val validDest = oauthManager.refreshTokenIfNeededSuspend(destinationAccount) ?: destinationAccount

        val startTime = System.currentTimeMillis()
        job.logs.add(TransferLog(message = "Downloading '${item.filename}'..."))
        _currentJob.value = job.copy()
        onProgressUpdate(job)

        // Step 2: Download file to temp cache & compute SHA-256 hash
        val downloadResult = apiService.downloadToTempFile(validSource, item) { readBytes, totalBytes ->
            // Progress callback during download
        }

        if (downloadResult == null) {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to download media file from source account."
            job.failedItems++
            job.logs.add(TransferLog(message = "ERROR: Failed to download '${item.filename}'", isError = true))
            _currentJob.value = job.copy()
            onProgressUpdate(job)
            return@withContext
        }

        val (tempFile, hash) = downloadResult

        // Step 3: Check Deduplication
        if (isDuplicateHash(validDest.id, hash)) {
            item.status = SyncStatus.COMPLETED
            job.completedItems++
            job.logs.add(TransferLog(message = "SKIPPED: '${item.filename}' (Duplicate photo already exists in destination account)."))
            tempFile.delete()
            _currentJob.value = job.copy()
            onProgressUpdate(job)
            return@withContext
        }

        // Step 4: Upload to Destination
        job.logs.add(TransferLog(message = "Uploading '${item.filename}' to destination account..."))
        _currentJob.value = job.copy()
        onProgressUpdate(job)

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
            markHashTransferred(validDest.id, hash)
            item.status = SyncStatus.COMPLETED
            job.completedItems++
            val fileSize = if (tempFile.length() > 0) tempFile.length() else 1024L * 1024L
            job.transferredBytes += fileSize
            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
            job.speedBytesPerSec = fileSize / elapsedSec

            job.logs.add(TransferLog(message = "SUCCESS: Uploaded '${item.filename}' to destination account."))

            // Step 5: If MOVE mode, trash from source account to free space
            if (mode == TransferMode.MOVE) {
                job.logs.add(TransferLog(message = "MOVE MODE: Trashing '${item.filename}' from source account to free storage..."))
                val deleted = apiService.deleteFromSourceAccount(validSource, item.id)
                if (deleted) {
                    item.status = SyncStatus.TRASHED_FROM_SOURCE
                    job.logs.add(TransferLog(message = "FREED STORAGE: '${item.filename}' moved to Trash in source account."))
                } else {
                    job.logs.add(TransferLog(message = "Notice: Transferred to destination, but could not delete from source account.", isError = true))
                }
            }
        } else {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to upload to destination account."
            job.failedItems++
            job.logs.add(TransferLog(message = "ERROR: Upload failed for '${item.filename}'", isError = true))
        }

        _currentJob.value = job.copy()
        onProgressUpdate(job)
    }

    fun pauseJob() {
        val job = _currentJob.value ?: return
        _currentJob.value = job.copy(status = JobStatus.PAUSED).also {
            it.logs.add(TransferLog(message = "Transfer job paused by user."))
        }
    }

    fun resumeJob() {
        val job = _currentJob.value ?: return
        _currentJob.value = job.copy(status = JobStatus.RUNNING).also {
            it.logs.add(TransferLog(message = "Transfer job resumed."))
        }
    }

    fun updateJobStatus(status: JobStatus) {
        val job = _currentJob.value ?: return
        _currentJob.value = job.copy(status = status)
    }
}
