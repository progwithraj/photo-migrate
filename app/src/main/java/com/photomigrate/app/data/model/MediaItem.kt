package com.photomigrate.app.data.model

enum class SyncStatus {
    IDLE,
    DOWNLOADING,
    UPLOADING,
    VERIFYING,
    COMPLETED,
    TRASHED_FROM_SOURCE,
    FAILED
}

data class MediaItem(
    val id: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val baseUrl: String,
    val creationTime: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val accountId: String,
    val thumbnailUrl: String? = null,
    val sha256Hash: String? = null,
    var status: SyncStatus = SyncStatus.IDLE,
    var errorMessage: String? = null,
    var isSelected: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}
