package com.pushkar.RGM

data class User(
    val username: String = "",
    val password: String = "",
    val nickname: String? = null,
    val birthday: String? = null,
    val comment: String? = null,
    val profileImage: String? = null
)
