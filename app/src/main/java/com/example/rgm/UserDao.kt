package com.pushkar.RGM

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users_table WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users_table")
    suspend fun getAllUsers(): List<UserEntity>
    
    @Query("SELECT COUNT(*) FROM users_table")
    suspend fun getUserCount(): Int
}
