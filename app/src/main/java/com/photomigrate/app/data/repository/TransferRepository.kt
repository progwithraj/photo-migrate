package com.photomigrate.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.photomigrate.app.data.api.GooglePhotosService
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.db.QueuedItem
import com.photomigrate.app.data.db.TransferDatabase
import com.photomigrate.app.data.db.TransferredFile
import com.photomigrate.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    private val albumCache = mutableMapOf<String, String>() // Title to ID map

    private val _currentJob = MutableStateFlow<TransferJob?>(null)
    val currentJob: StateFlow<TransferJob?> = _currentJob.asStateFlow()

    private val _sourceMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val sourceMediaList: StateFlow<List<MediaItem>> = _sourceMediaList.asStateFlow()

    private val _isLoadingMedia = MutableStateFlow(false)
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia.asStateFlow()

    private var loadMediaJob: Job? = null

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
        // Cancel any existing load to prevent mixing results from different accounts
        loadMediaJob?.cancel()
        loadMediaJob = coroutineContext[Job]
        
        _isLoadingMedia.value = true
        _sourceMediaList.value = emptyList() // Clear previous results immediately
        
        try {
            val validAccount = oauthManager.refreshTokenIfNeededSuspend(sourceAccount) ?: sourceAccount
            val transferredIds = if (destinationAccount != null) {
                db.transferDao().getTransferredMediaIds(destinationAccount.id).toSet()
            } else emptySet()

            val allItems = mutableListOf<MediaItem>()
            var nextToken: String? = null
            
            do {
                // Ensure we haven't been cancelled by a new request
                yield() 

                val (items, token) = apiService.listMediaItems(validAccount, pageSize = 100, pageToken = nextToken)
                
                if (items.isEmpty() && token != null) {
                    Log.w("TransferRepository", "Received empty page but has nextToken. Retrying with token.")
                }

                // Filter out items that are already transferred
                val filteredItems = items.filter { it.id !in transferredIds }
                allItems.addAll(filteredItems)
                nextToken = token
                
                // Update UI incrementally
                _sourceMediaList.value = allItems.toList()
                
                // Stop after 50,000 items for stability, or when finished
                if (allItems.size >= 50000) break
            } while (nextToken != null)
            
            Log.d("TransferRepository", "Finished loading media. Total items found: ${allItems.size}")
            
            // Background Task: Index destination account to detect duplicates already present
            if (destinationAccount != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    indexDestinationAccount(destinationAccount)
                }
            }
            
            allItems
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.e("TransferRepository", "Error loading media: ${e.message}", e)
            }
            emptyList()
        } finally {
            _isLoadingMedia.value = false
        }
    }

    /**
     * Starts a new transfer/migration job and persists selected IDs to the database.
     */
    suspend fun createAndStartJob(
        sourceAccount: GoogleAccount,
        destinationAccount: GoogleAccount,
        mode: TransferMode,
        isCompressed: Boolean,
        orgMode: OrganizationMode,
        selectedItems: List<MediaItem>
    ): TransferJob = withContext(Dispatchers.IO) {
        val job = TransferJob(
            sourceAccountId = sourceAccount.id,
            destinationAccountId = destinationAccount.id,
            mode = mode,
            orgMode = orgMode,
            batchAlbumName = null,
            isCompressionEnabled = isCompressed,
            selectedMediaIds = selectedItems.map { it.id },
            totalItems = selectedItems.size,
            totalBytes = selectedItems.sumOf { it.sizeBytes },
            status = JobStatus.RUNNING,
            logs = listOf(TransferLog(message = "Job initialized: ${selectedItems.size} items queued for ${mode.name} mode (Storage Saver: $isCompressed, AI: ${orgMode != OrganizationMode.NONE})."))
        )
        
        // Clear album cache for new job
        albumCache.clear()
        
        // Persist queue to database to bypass WorkManager data limits
        val queuedItems = selectedItems.map { QueuedItem(job.id, it.id) }
        db.transferDao().insertQueuedItems(queuedItems)
        
        // Persist job to history
        db.transferDao().insertJob(job.toEntity())
        job.logs.forEach { db.transferDao().insertLog(it.toEntity(job.id)) }
        
        _currentJob.value = job
        job
    }

    suspend fun getHistory(): List<TransferJob> = withContext(Dispatchers.IO) {
        db.transferDao().getAllJobs().map { it.toModel() }
    }

    suspend fun getJobLogs(jobId: String): List<TransferLog> = withContext(Dispatchers.IO) {
        db.transferDao().getLogsForJob(jobId).map { it.toModel() }
    }

    suspend fun getQueuedMediaIds(jobId: String): List<String> = withContext(Dispatchers.IO) {
        db.transferDao().getQueuedMediaIds(jobId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        db.transferDao().clearHistory()
    }
    
    suspend fun getTotalTransferredBytes(): Long = withContext(Dispatchers.IO) {
        db.transferDao().getTotalTransferredBytes() ?: 0L
    }

    private fun updateJob(jobId: String? = null, reducer: (TransferJob) -> TransferJob) {
        val current = _currentJob.value ?: return
        if (jobId != null && current.id != jobId) return 
        val updated = reducer(current)
        _currentJob.value = updated
        
        // Persist update to DB (async)
        CoroutineScope(Dispatchers.IO).launch {
            db.transferDao().updateJob(updated.toEntity())
        }
    }

    private fun addLog(jobId: String? = null, message: String, isError: Boolean = false) {
        val log = TransferLog(message = message, isError = isError)
        updateJob(jobId) { it.copy(logs = it.logs + log) }
        
        // Persist log to DB
        jobId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                db.transferDao().insertLog(log.toEntity(id))
            }
        }
    }

    private fun TransferJob.toEntity() = com.photomigrate.app.data.db.TransferJobEntity(
        id = id,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        mode = mode.name,
        orgMode = orgMode.name,
        batchAlbumName = batchAlbumName,
        totalItems = totalItems,
        completedItems = completedItems,
        failedItems = failedItems,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        status = status.name,
        startTime = startTime,
        endTime = endTime
    )

    private fun com.photomigrate.app.data.db.TransferJobEntity.toModel() = TransferJob(
        id = id,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        mode = TransferMode.valueOf(mode),
        orgMode = OrganizationMode.valueOf(orgMode),
        batchAlbumName = batchAlbumName,
        totalItems = totalItems,
        completedItems = completedItems,
        failedItems = failedItems,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        status = JobStatus.valueOf(status),
        startTime = startTime,
        endTime = endTime
    )

    private fun TransferLog.toEntity(jobId: String) = com.photomigrate.app.data.db.JobLogEntity(
        jobId = jobId,
        timestamp = timestamp,
        message = message,
        isError = isError
    )

    private fun com.photomigrate.app.data.db.JobLogEntity.toModel() = TransferLog(
        timestamp = timestamp,
        message = message,
        isError = isError
    )

    /**
     * Executes the transfer loop item by item.
     */
    suspend fun processNextMediaItem(
        sourceAccount: GoogleAccount,
        destinationAccount: GoogleAccount,
        item: MediaItem,
        mode: TransferMode,
        jobId: String,
        isCompressed: Boolean = false,
        orgMode: OrganizationMode = OrganizationMode.NONE,
        onProgressUpdate: (TransferJob) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Step 1: Ensure OAuth Tokens are valid
        val validSource = oauthManager.refreshTokenIfNeededSuspend(sourceAccount) ?: sourceAccount
        val validDest = oauthManager.refreshTokenIfNeededSuspend(destinationAccount) ?: destinationAccount

        val startTime = System.currentTimeMillis()
        
        // Smart Duplicate Detection Step 0: Check metadata before downloading
        if (isSmartDuplicate(destinationAccount.id, item)) {
            addLog(jobId, "SMART SKIP: '${item.filename}' already exists in destination (Matched via Metadata).")
            item.status = SyncStatus.COMPLETED
            updateJob(jobId) { it.copy(completedItems = it.completedItems + 1) }
            _currentJob.value?.let { onProgressUpdate(it) }
            return@withContext
        }

        addLog(jobId, "Downloading '${item.filename}'...")
        _currentJob.value?.let { onProgressUpdate(it) }

        var lastUpdate = 0L
        // Step 2: Download file to temp cache & compute SHA-256 hash
        val downloadResult = apiService.downloadToTempFile(validSource, item) { readBytes, totalBytes ->
            val now = System.currentTimeMillis()
            // Throttle UI updates to every 300ms to prevent UI freezing
            if (now - lastUpdate > 300) {
                val elapsed = (now - startTime) / 1000L
                if (elapsed > 0) {
                    val currentSpeed = (readBytes / elapsed)
                    updateJob(jobId) { 
                        val newHistory = (it.speedHistory + currentSpeed).takeLast(200)
                        it.copy(
                            speedBytesPerSec = currentSpeed,
                            speedHistory = newHistory
                        )
                    }
                    _currentJob.value?.let { onProgressUpdate(it) }
                }
                lastUpdate = now
            }
        }

        if (downloadResult == null) {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to download media file from source account."
            updateJob(jobId) { it.copy(failedItems = it.failedItems + 1) }
            addLog(jobId, "ERROR: Failed to download '${item.filename}'", isError = true)
            _currentJob.value?.let { onProgressUpdate(it) }
            return@withContext
        }

        val (origFile, hash) = downloadResult
        
        // Storage Saver Optimization: Compress image before upload
        val fileToUpload = if (isCompressed && item.mimeType.startsWith("image/")) {
            addLog(jobId, "Optimizing '${item.filename}' (Storage Saver)...")
            com.photomigrate.app.util.MediaCompressor.compressImage(context, origFile, item.mimeType) ?: origFile
        } else {
            origFile
        }

        val actualFileSize = fileToUpload.length()

        // Step 3: Check Deduplication (Always use original file hash for duplicate detection)
        if (isDuplicateHash(validDest.id, hash)) {
            item.status = SyncStatus.COMPLETED
            markTransferred(item.id, validDest.id, hash)
            updateJob(jobId) { 
                it.copy(
                    completedItems = it.completedItems + 1,
                    transferredBytes = it.transferredBytes + actualFileSize 
                ) 
            }
            addLog(jobId, "SKIPPED: '${item.filename}' (Already exists in destination).")
            origFile.delete()
            if (fileToUpload != origFile) fileToUpload.delete()
            _currentJob.value?.let { onProgressUpdate(it) }
            return@withContext
        }

        // Step 4: Upload to Destination
        val sizeMb = String.format(java.util.Locale.US, "%.2f", actualFileSize / 1024.0 / 1024.0)
        addLog(jobId, "Uploading '${item.filename}' ($sizeMb MB)...")
        _currentJob.value?.let { onProgressUpdate(it) }

        lastUpdate = 0L
        
        // Smart AI Organization Step: Determine target album
        var targetAlbumId: String? = null
        if (orgMode != OrganizationMode.NONE && item.mimeType.startsWith("image/")) {
            val albumName = when (orgMode) {
                OrganizationMode.BY_DATE -> {
                    // Extract Month Year from creationTime (e.g. 2026-08-16T... -> August 2026)
                    val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(item.creationTime.take(7))
                    date?.let { SimpleDateFormat("MMMM yyyy", Locale.US).format(it) } ?: "Migrated Photos"
                }
                OrganizationMode.BY_CONTENT -> {
                    // Use the consensus batch name decided during pre-analysis
                    _currentJob.value?.batchAlbumName ?: "Other"
                }
                else -> null
            }
            
            if (albumName != null) {
                targetAlbumId = getOrCreateAlbumId(validDest, albumName)
                if (targetAlbumId != null) {
                    addLog(jobId, "Sorting into album: '$albumName'")
                }
            }
        }

        val uploadResult = apiService.uploadMediaToDestination(
            destinationAccount = validDest,
            file = fileToUpload,
            filename = item.filename,
            mimeType = item.mimeType,
            albumId = targetAlbumId
        ) { uploadedBytes, totalBytes ->
            val now = System.currentTimeMillis()
            // Throttle UI updates to every 300ms
            if (now - lastUpdate > 300) {
                val elapsed = (now - startTime) / 1000L
                if (elapsed > 0) {
                    val currentSpeed = (uploadedBytes / elapsed)
                    updateJob(jobId) { 
                        val newHistory = (it.speedHistory + currentSpeed).takeLast(200)
                        it.copy(
                            speedBytesPerSec = currentSpeed,
                            speedHistory = newHistory
                        )
                    }
                    _currentJob.value?.let { onProgressUpdate(it) }
                }
                lastUpdate = now
            }
        }

        origFile.delete() // Clean up original cache file
        if (fileToUpload != origFile) fileToUpload.delete() // Clean up compressed file

        if (uploadResult != null) {
            markTransferred(item.id, validDest.id, hash)
            item.status = SyncStatus.COMPLETED
            
            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
            val speed = actualFileSize / elapsedSec

            updateJob(jobId) { 
                val newHistory = (it.speedHistory + speed).takeLast(50)
                it.copy(
                    completedItems = it.completedItems + 1,
                    transferredBytes = it.transferredBytes + actualFileSize,
                    speedBytesPerSec = speed,
                    speedHistory = newHistory
                )
            }

            addLog(jobId, "SUCCESS: Uploaded '${item.filename}'")

            // Step 5: If MOVE mode, trash from source account to free storage
            if (mode == TransferMode.MOVE) {
                addLog(jobId, "MOVE MODE: Trashing '${item.filename}' from source account...")
                val deleted = apiService.deleteFromSourceAccount(validSource, item.id, item.filename)
                if (deleted) {
                    item.status = SyncStatus.TRASHED_FROM_SOURCE
                    addLog(jobId, "FREED STORAGE: '${item.filename}' moved to Trash in source account.")
                } else {
                    addLog(jobId, "Notice: Transferred to destination, but could not trash from source. Ensure you checked the 'Full Drive' permission box during login.", isError = true)
                }
            }
        } else {
            item.status = SyncStatus.FAILED
            item.errorMessage = "Failed to upload to destination account."
            updateJob(jobId) { it.copy(failedItems = it.failedItems + 1) }
            addLog(jobId, "ERROR: Upload failed for '${item.filename}'", isError = true)
        }

        _currentJob.value?.let { onProgressUpdate(it) }
    }

    /**
     * Set a batch-wide album name for AI grouping.
     */
    fun setBatchAlbumName(jobId: String, name: String) {
        updateJob(jobId) { it.copy(batchAlbumName = name) }
    }

    /**
     * Lightweight download for AI analysis.
     */
    suspend fun downloadTempForAnalysis(account: GoogleAccount, item: MediaItem): File? {
        val result = apiService.downloadToTempFile(account, item) { _, _ -> }
        return result?.first
    }

    /**
     * Finds or creates an album in the destination account by its name.
     */
    private suspend fun getOrCreateAlbumId(account: GoogleAccount, name: String): String? = withContext(Dispatchers.IO) {
        // 1. Check local cache
        albumCache[name]?.let { return@withContext it }
        
        // 2. Search in account
        val albums = apiService.listAlbums(account)
        val existing = albums.find { it.second.equals(name, ignoreCase = true) }
        if (existing != null) {
            albumCache[name] = existing.first
            return@withContext existing.first
        }
        
        // 3. Create new if not found
        val newId = apiService.createAlbum(account, name)
        if (newId != null) {
            albumCache[name] = newId
        }
        newId
    }

    /**
     * Smart Duplicate Detection: Check if a similar file exists in destination account.
     */
    suspend fun isSmartDuplicate(accountId: String, item: MediaItem): Boolean {
        // 1. Exact match by ID (if we moved it ourselves before)
        val transferredIds = db.transferDao().getTransferredMediaIds(accountId)
        if (item.id in transferredIds) return true
        
        // 2. Metadata match: Filename + Size + CreationTime
        // Note: size might be 0 for Photos API, so we only match if size > 0
        val match = db.transferDao().findRemoteMatch(
            accountId = accountId,
            filename = item.filename,
            size = item.sizeBytes,
            time = item.creationTime
        )
        return match != null
    }

    /**
     * Index destination account media to create a lookup for Smart Duplicate Detection.
     */
    suspend fun indexDestinationAccount(account: GoogleAccount) = withContext(Dispatchers.IO) {
        try {
            Log.d("TransferRepository", "Indexing destination account: ${account.email}")
            val validAccount = oauthManager.refreshTokenIfNeededSuspend(account) ?: account
            
            var nextToken: String? = null
            do {
                val (items, token) = apiService.listMediaItems(validAccount, pageSize = 100, pageToken = nextToken)
                
                val metadata = items.map { 
                    com.photomigrate.app.data.db.RemoteMetadata(
                        accountId = account.id,
                        filename = it.filename,
                        sizeBytes = it.sizeBytes,
                        creationTime = it.creationTime
                    )
                }
                
                db.transferDao().insertRemoteMetadata(metadata)
                nextToken = token
                
                // Limit indexing to 10,000 recent items to keep it fast
                // or just continue if needed.
            } while (nextToken != null && !nextToken.startsWith("drive_")) 
            // We mostly care about Google Photos items for indexing as that's where duplicates happen.
            
            Log.d("TransferRepository", "Finished indexing ${account.email}")
        } catch (e: Exception) {
            Log.e("TransferRepository", "Indexing failed: ${e.message}")
        }
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
        updateJob { 
            it.copy(
                status = status,
                endTime = if (status == JobStatus.COMPLETED || status == JobStatus.FAILED || status == JobStatus.CANCELLED) {
                    System.currentTimeMillis()
                } else it.endTime
            )
        }
    }

    fun forceLog(jobId: String? = null, message: String, isError: Boolean = false) {
        addLog(jobId, message, isError)
    }

    suspend fun fetchItemsForWorker(account: GoogleAccount, ids: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        apiService.fetchMediaItemsByIds(account, ids)
    }
}
