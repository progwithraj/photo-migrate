package com.photomigrate.app.data.model

import java.util.UUID

enum class AccountRole {
    SOURCE,
    DESTINATION,
    BOTH
}

data class GoogleAccount(
    val email: String,
    val displayName: String = "",
    val photoUrl: String = "",
    val accessToken: String,
    val refreshToken: String = "",
    val tokenExpirationTimeMillis: Long = 0L,
    val usedStorageBytes: Long = 0L,
    val totalStorageBytes: Long = 16106127360L, // Default 15 GB
    var role: AccountRole = AccountRole.BOTH,
    val isPrimary: Boolean = false
) {
    val id: String get() = email

    val freeStorageBytes: Long
        get() = (totalStorageBytes - usedStorageBytes).coerceAtLeast(0L)

    val usedPercentage: Float
        get() = if (totalStorageBytes > 0) (usedStorageBytes.toFloat() / totalStorageBytes.toFloat()).coerceIn(0f, 1f) else 0f

    fun isTokenExpired(): Boolean = System.currentTimeMillis() >= (tokenExpirationTimeMillis - 60000L)
}
