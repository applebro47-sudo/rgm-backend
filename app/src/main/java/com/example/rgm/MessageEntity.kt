package com.pushkar.RGM

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages_table")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val receiver: String,
    val text: String?,
    val mediaUri: String?,
    val mediaType: String?,
    val timestamp: Long,
    val chatId: String // to easily fetch messages for a specific one-to-one chat
)
