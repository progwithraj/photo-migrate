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

enum class SortBy {
    NEWEST, OLDEST, SIZE_DESC, SIZE_ASC, NAME_AZ, NAME_ZA
}

data class MediaItem(
    val id: String = "",
    val filename: String = "",
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long = 0L,
    val baseUrl: String = "",
    val creationTime: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val accountId: String = "",
    val thumbnailUrl: String? = null,
    val sha256Hash: String? = null,
    var status: SyncStatus = SyncStatus.IDLE,
    var errorMessage: String? = null,
    var isSelected: Boolean = false
) {
    val safeMimeType: String
        get() = mimeType.orEmpty()

    val safeFilename: String
        get() = filename.orEmpty()

    val safeCreationTime: String
        get() = creationTime.orEmpty()

    val safeBaseUrl: String
        get() = baseUrl.orEmpty()

    val isVideo: Boolean
        get() = safeMimeType.startsWith("video/")
}
