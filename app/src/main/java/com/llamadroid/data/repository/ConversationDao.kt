package com.llamadroid.data.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.llamadroid.data.models.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun all(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: Conversation)

    @Update
    suspend fun update(conversation: Conversation)

    @Delete
    suspend fun delete(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE conversations SET isPinned = :pinned WHERE id = :id")
    suspend fun pin(id: String, pinned: Boolean)
}
