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
        const val KEY_SOURCE_ACCOUNT_ID = "source_account_id"
        const val KEY_DEST_ACCOUNT_ID = "dest_account_id"
        const val KEY_MODE = "transfer_mode"
        const val KEY_SELECTED_IDS = "selected_ids"
    }

    override suspend fun doWork(): Result {
        val sourceId = inputData.getString(KEY_SOURCE_ACCOUNT_ID) ?: return Result.failure()
        val destId = inputData.getString(KEY_DEST_ACCOUNT_ID) ?: return Result.failure()
        val selectedIds = inputData.getStringArray(KEY_SELECTED_IDS) ?: emptyArray()
        val modeName = inputData.getString(KEY_MODE) ?: TransferMode.COPY.name
        val mode = TransferMode.valueOf(modeName)

        val accounts = oauthManager.getSavedAccounts()
        val sourceAccount = accounts.find { it.id == sourceId } ?: return Result.failure()
        val destAccount = accounts.find { it.id == destId } ?: return Result.failure()

        // Create foreground notification
        createNotificationChannel()
        setForeground(createForegroundInfo("Preparing photo transfer...", 0, 100))

        // INITIALIZE UI IMMEDIATELY
        // We create a dummy job so the UI shows "Preparing..." while we fetch metadata
        val initialJob = repository.createAndStartJob(sourceAccount, destAccount, mode, emptyList())
        var currentJobId = initialJob.id
        repository.forceLog(currentJobId, "Preparing metadata for ${selectedIds.size} items...")

        // EFFICIENT FETCH: Instead of loadSourceMedia (which scans everything), 
        // fetch only the metadata for the specific IDs selected.
        val selectedItems = repository.fetchItemsForWorker(sourceAccount, selectedIds.toList())
        Log.d("TransferWorker", "Fetched ${selectedItems.size} items for processing. Expected: ${selectedIds.size}")
        
        if (selectedItems.isEmpty()) {
            Log.w("TransferWorker", "No media items found to process or fetched 0 items.")
            repository.updateJobStatus(JobStatus.FAILED)
            repository.forceLog(currentJobId, "Error: Could not retrieve metadata for selected items.", true)
            return Result.success()
        }

        // ALWAYS initialize a fresh job for the new selection to ensure progress starts at 0%
        // and totalItems matches the actual batch size.
        val actualJob = repository.createAndStartJob(sourceAccount, destAccount, mode, selectedItems)
        currentJobId = actualJob.id // UPDATE to the real job ID being tracked
        
        Log.d("TransferWorker", "Job ready. Starting loop...")

        try {
            for ((index, item) in selectedItems.withIndex()) {
                val currentJob = repository.currentJob.value
                if (currentJob?.id != currentJobId) {
                    Log.d("TransferWorker", "Job ID mismatch (New job started?). Aborting old worker.")
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

                repository.processNextMediaItem(
                    sourceAccount = sourceAccount,
                    destinationAccount = destAccount,
                    item = item,
                    mode = mode,
                    jobId = currentJobId
                ) { updatedJob ->
                    // Update live job state
                }
            }
            
            // Only mark as COMPLETED if we actually reached the end of the loop 
            // and the job ID still matches.
            if (repository.currentJob.value?.id == currentJobId) {
                repository.updateJobStatus(JobStatus.COMPLETED)
            }
        } catch (e: Exception) {
            Log.e("TransferWorker", "Fatal error during loop: ${e.message}", e)
            repository.updateJobStatus(JobStatus.FAILED)
            repository.forceLog(currentJobId, "CRITICAL ERROR: ${e.message}", true)
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
