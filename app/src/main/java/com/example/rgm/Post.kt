package com.pushkar.RGM

data class Post(
    val id: String = java.util.UUID.randomUUID().toString(),
    val owner: String = "",
    val mediaUri: String = "",
    val mediaType: String = "", // "IMAGE" or "VIDEO"
    val caption: String? = null,
    val likes: MutableList<String>? = mutableListOf(),
    val comments: MutableList<Comment>? = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val user: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
