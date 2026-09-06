package com.photomigrate.app.data.model

import java.util.UUID

enum class TransferMode {
    COPY, // Keep originals in source account
    MOVE  // Copy to destination, then delete from source account to free storage
}

enum class DestinationType {
    GOOGLE,
    TELEGRAM_BOT,
    TELEGRAM_MTPROTO
}

enum class OrganizationMode {
    NONE,
    BY_DATE,   // e.g. "August 2026"
    BY_CONTENT // Using AI Content Analysis
}

enum class JobStatus {
    IDLE,
    PREPARING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferLog(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

data class TransferJob(
    val id: String = UUID.randomUUID().toString(),
    val sourceAccountId: String,
    val destinationAccountId: String,
    val destinationType: DestinationType = DestinationType.GOOGLE,
    val mode: TransferMode = TransferMode.COPY,
    val orgMode: OrganizationMode = OrganizationMode.NONE,
    val batchAlbumName: String? = null, // Consensus album name for AI grouping
    val isCompressionEnabled: Boolean = false,
    val isResumed: Boolean = false,
    val selectedMediaIds: List<String> = emptyList(),
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val speedHistory: List<Long> = emptyList(), // History for trend line
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: JobStatus = JobStatus.IDLE,
    val logs: List<TransferLog> = emptyList()
) {
    val progress: Float
        get() = if (totalItems > 0) (completedItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingTimeMillis: Long
        get() {
            if (speedBytesPerSec <= 0) return 0L
            val remainingBytes = totalBytes - transferredBytes
            return (remainingBytes * 1000L) / speedBytesPerSec
        }

    val bytesProgress: Float
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
