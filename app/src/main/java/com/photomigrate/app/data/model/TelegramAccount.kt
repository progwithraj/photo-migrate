package com.photomigrate.app.data.model

data class TelegramAccount(
    val id: String,
    val botToken: String,
    val chatId: String,
    val botName: String
)
