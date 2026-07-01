package com.llamadroid.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.llamadroid.data.models.SystemPrompt
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemPromptDao {
    @Query("SELECT * FROM system_prompts ORDER BY isBuiltin DESC, createdAt ASC")
    fun all(): Flow<List<SystemPrompt>>

    @Query("SELECT * FROM system_prompts WHERE id = :id")
    suspend fun get(id: String): SystemPrompt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prompt: SystemPrompt)

    @Query("DELETE FROM system_prompts WHERE id = :id AND isBuiltin = 0")
    suspend fun delete(id: String)
}
