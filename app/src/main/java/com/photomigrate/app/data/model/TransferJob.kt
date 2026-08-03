package com.photomigrate.app.data.model

import java.util.UUID

enum class TransferMode {
    COPY, // Keep originals in source account
    MOVE  // Copy to destination, then delete from source account to free storage
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
    val mode: TransferMode = TransferMode.COPY,
    val selectedMediaIds: List<String> = emptyList(),
    var totalItems: Int = 0,
    var completedItems: Int = 0,
    var failedItems: Int = 0,
    var totalBytes: Long = 0L,
    var transferredBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var status: JobStatus = JobStatus.IDLE,
    val logs: MutableList<TransferLog> = mutableListOf()
) {
    val progress: Float
        get() = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f

    val bytesProgress: Float
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
