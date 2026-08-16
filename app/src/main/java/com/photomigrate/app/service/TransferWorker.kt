package com.photomigrate.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.JobStatus
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.data.repository.TransferRepository
import kotlinx.coroutines.delay

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

        val accounts = oauthManager.getSavedAccounts()
        val sourceAccount = accounts.find { it.id == sourceId } ?: return Result.failure()
        val destAccount = accounts.find { it.id == destId } ?: return Result.failure()

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

        // The job was already created in MainActivity, we just start processing.
        Log.d("TransferWorker", "Job ready. Starting loop...")

        try {
            for ((index, item) in selectedItems.withIndex()) {
                val currentJob = repository.currentJob.value
                // If a new job was started, this worker should stop.
                if (currentJob?.id != jobId) {
                    Log.d("TransferWorker", "Job ID mismatch. Aborting old worker.")
                    return Result.success()
                }

                if (currentJob.status == JobStatus.PAUSED) {
                    while (repository.currentJob.value?.status == JobStatus.PAUSED) {
                        delay(1000)
                    }
                }

                if (repository.currentJob.value?.status == JobStatus.CANCELLED) {
                    break
                }

                val progressPercent = ((index + 1) * 100) / selectedItems.size
                val notificationText = "Syncing ${index + 1}/${selectedItems.size}: ${item.filename}"
                setForeground(createForegroundInfo(notificationText, progressPercent, 100))

                val isCompressed = inputData.getBoolean("is_compressed", false)

                repository.processNextMediaItem(
                    sourceAccount = sourceAccount,
                    destinationAccount = destAccount,
                    item = item,
                    mode = mode,
                    jobId = jobId,
                    isCompressed = isCompressed
                ) { updatedJob ->
                    // Update live job state
                }
            }
            
            if (repository.currentJob.value?.id == jobId) {
                val finalJob = repository.currentJob.value
                val status = when {
                    finalJob == null -> JobStatus.FAILED
                    finalJob.failedItems > 0 && finalJob.completedItems == 0 -> JobStatus.FAILED
                    finalJob.failedItems > 0 -> JobStatus.COMPLETED // Or add a PARTIAL status
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
