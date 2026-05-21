package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads ORDER BY timestamp DESC")
    fun getAllThreads(): Flow<List<ChatThread>>

    @Query("SELECT * FROM chat_threads WHERE id = :id LIMIT 1")
    suspend fun getThreadById(id: Long): ChatThread?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThread): Long

    @Update
    suspend fun updateThread(thread: ChatThread)

    @Query("DELETE FROM chat_threads WHERE id = :threadId")
    suspend fun deleteThreadById(threadId: Long)

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: Long)
}
