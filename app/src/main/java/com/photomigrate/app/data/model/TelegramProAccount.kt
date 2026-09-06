package com.photomigrate.app.data.model

data class TelegramProAccount(
    val id: String,
    val phoneNumber: String,
    val name: String,
    val username: String = "",
    val apiId: Int = 0,
    val apiHash: String = "",
    val sessionData: String = ""
)
