package com.pushkar.RGM

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_table")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String,
    val nickname: String? = "",
    val profileImage: String? = "",
    val isProfileCreated: Boolean = false
)
