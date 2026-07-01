package com.llamadroid.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.llamadroid.data.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun byConversation(convId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): Message?

    @Insert
    suspend fun insert(message: Message)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)
}
