package com.photomigrate.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.data.repository.TransferRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class TransferWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = TransferRepository.getInstance(context)
    private val oauthManager = OAuthManager(context)

    companion object {
        const val CHANNEL_ID = "photo_migrate_transfer_channel"
        const val NOTIFICATION_ID = 1001
        const val KEY_JOB_ID = "job_id"
        const val KEY_SOURCE_ACCOUNT_ID = "source_account_id"
        const val KEY_DEST_ACCOUNT_ID = "dest_account_id"
        const val KEY_MODE = "transfer_mode"
        const val KEY_SELECTED_IDS = "selected_ids"
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val sourceId = inputData.getString(KEY_SOURCE_ACCOUNT_ID) ?: return Result.failure()
        val destId = inputData.getString(KEY_DEST_ACCOUNT_ID) ?: return Result.failure()
        
        // Retrieve selected IDs from database instead of inputData to avoid 10KB limit
        val selectedIds = repository.getQueuedMediaIds(jobId)
        
        val modeName = inputData.getString(KEY_MODE) ?: TransferMode.COPY.name
        val mode = TransferMode.valueOf(modeName)

        val orgModeName = inputData.getString("org_mode") ?: com.photomigrate.app.data.model.OrganizationMode.NONE.name
        val orgMode = com.photomigrate.app.data.model.OrganizationMode.valueOf(orgModeName)

        val accounts = oauthManager.getSavedAccounts()
        val sourceAccount = accounts.find { it.id == sourceId } ?: return Result.failure()
        
        val jobFromDb = repository.getHistory().find { it.id == jobId }
        val destinationType = jobFromDb?.destinationType ?: com.photomigrate.app.data.model.DestinationType.GOOGLE

        // Sync repository state if it's null (e.g. process restart)
        if (repository.currentJob.value == null || repository.currentJob.value?.id != jobId) {
            val jobFromDb = repository.getHistory().find { it.id == jobId }
            if (jobFromDb != null) {
                repository.resumeJob(jobFromDb)
            }
        }

        // Create foreground notification
        createNotificationChannel()
        setForeground(createForegroundInfo("Preparing photo transfer...", 0, 100))

        // INITIALIZE UI IMMEDIATELY
        repository.forceLog(jobId, "Preparing metadata for ${selectedIds.size} items...")

        // EFFICIENT FETCH
        val selectedItems = repository.fetchItemsForWorker(sourceAccount, selectedIds)
        Log.d("TransferWorker", "Fetched ${selectedItems.size} items for processing. Expected: ${selectedIds.size}")
        
        if (selectedItems.isEmpty()) {
            Log.w("TransferWorker", "No media items found to process or fetched 0 items.")
            repository.updateJobStatus(JobStatus.FAILED)
            repository.forceLog(jobId, "Error: Could not retrieve metadata for selected items.", true)
            return Result.success()
        }

        // BATCH AI ANALYSIS PHASE
        if (orgMode == com.photomigrate.app.data.model.OrganizationMode.BY_CONTENT) {
            repository.forceLog(jobId, "AI BATCH ANALYSIS: Scanning selected photos for consensus category...")
            
            // Analyze up to 5 items to find dominant category
            val sampleItems = selectedItems.take(5)
            val labels = mutableListOf<String>()
            
            for (item in sampleItems) {
                repository.forceLog(jobId, "AI: Analyzing sample '${item.filename}'...")
                val downloadResult: File? = repository.downloadTempForAnalysis(sourceAccount, item)
                if (downloadResult != null) {
                    val label = com.photomigrate.app.util.MediaAnalyzer.analyzeImageContent(downloadResult)
                    if (label != null) labels.add(label)
                    downloadResult.delete()
                }
            }
            
            val dominantLabel = labels.groupBy { it }
                .maxByOrNull { it.value.size }?.key ?: "Other"
            
            repository.setBatchAlbumName(jobId, dominantLabel)
            repository.forceLog(jobId, "AI BATCH ANALYSIS: Consensus reached. Using album '$dominantLabel' for this batch.")
        }

        // The job was already created in MainActivity, we just start processing.
        val concurrentLimit = oauthManager.getConcurrentLimit()
        Log.d("TransferWorker", "Job ready. Starting parallel loop with limit: $concurrentLimit")

        val semaphore = Semaphore(concurrentLimit) 

        try {
            coroutineScope {
                selectedItems.forEachIndexed { index, item ->
                    launch {
                        semaphore.withPermit {
                            val currentJob = repository.currentJob.value
                            if (currentJob?.id != jobId) return@launch

                            if (currentJob.status == JobStatus.PAUSED) {
                                while (repository.currentJob.value?.status == JobStatus.PAUSED) {
                                    delay(1000)
                                }
                            }

                            if (repository.currentJob.value?.status == JobStatus.CANCELLED) {
                                return@launch
                            }

                            val activeItems = selectedItems.size
                            val progressPercent = ((index + 1) * 100) / activeItems
                            setForeground(createForegroundInfo("Syncing memories in parallel...", progressPercent, 100))

                            val isCompressed = inputData.getBoolean("is_compressed", false)
                            val orgModeName = inputData.getString("org_mode") ?: com.photomigrate.app.data.model.OrganizationMode.NONE.name
                            val orgMode = com.photomigrate.app.data.model.OrganizationMode.valueOf(orgModeName)

                            repository.processNextMediaItem(
                                sourceAccount = sourceAccount,
                                destinationAccountId = destId,
                                destinationType = destinationType,
                                item = item,
                                mode = mode,
                                jobId = jobId,
                                isCompressed = isCompressed,
                                orgMode = orgMode
                            ) { _ -> }
                        }
                    }
                }
            }
            
            if (repository.currentJob.value?.id == jobId) {
                val finalJob = repository.currentJob.value
                val status = when {
                    finalJob == null -> JobStatus.FAILED
                    finalJob.failedItems > 0 -> JobStatus.FAILED
                    else -> JobStatus.COMPLETED
                }
                repository.updateJobStatus(status)
            }
        } catch (e: Exception) {
            Log.e("TransferWorker", "Fatal error during loop: ${e.message}", e)
            repository.updateJobStatus(JobStatus.FAILED)
            repository.forceLog(jobId, "CRITICAL ERROR: ${e.message}", true)
        }

        val finalJob = repository.currentJob.value
        val summaryText = "Completed ${finalJob?.completedItems ?: 0} of ${selectedItems.size} photos."
        showCompletionNotification(summaryText)

        return Result.success()
    }

    private fun createForegroundInfo(contentText: String, progress: Int, maxProgress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PhotoMigrate Transfer Engine")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(maxProgress, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Photo Transfer Complete")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Photo Transfer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress of photo & video synchronization between Google Accounts"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
