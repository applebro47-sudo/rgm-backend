package com.pushkar.RGM

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesByChatId(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages_table WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)
}
