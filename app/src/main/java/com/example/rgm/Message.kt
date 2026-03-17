package com.pushkar.RGM

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "",
    val receiver: String = "",
    val text: String? = null,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE" or "VIDEO"
    val isOneTime: Boolean = false,
    val isViewed: Boolean = false,
    val isSaved: Boolean = false,
    val deletedBy: MutableList<String>? = mutableListOf(),
    val isEdited: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
