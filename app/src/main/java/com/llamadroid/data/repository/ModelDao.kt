package com.llamadroid.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.llamadroid.data.models.DownloadedModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM downloaded_models ORDER BY downloadedAt DESC")
    fun all(): Flow<List<DownloadedModel>>

    @Query("SELECT * FROM downloaded_models WHERE id = :id")
    suspend fun get(id: String): DownloadedModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: DownloadedModel)

    @Query("DELETE FROM downloaded_models WHERE id = :id")
    suspend fun delete(id: String)
}
