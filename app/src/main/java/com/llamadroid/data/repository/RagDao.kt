package com.llamadroid.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.llamadroid.data.models.RagChunk
import com.llamadroid.data.models.RagDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface RagDocumentDao {
    @Query("SELECT * FROM rag_documents ORDER BY importedAt DESC")
    fun all(): Flow<List<RagDocument>>

    @Query("SELECT * FROM rag_documents WHERE id = :id")
    suspend fun get(id: String): RagDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: RagDocument)

    @Query("DELETE FROM rag_documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RagChunkDao {
    @Query("SELECT * FROM rag_chunks WHERE documentId = :docId ORDER BY chunkIndex ASC")
    suspend fun byDocument(docId: String): List<RagChunk>

    @Query("SELECT * FROM rag_chunks WHERE textContent LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun search(query: String, limit: Int = 10): List<RagChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<RagChunk>)

    @Query("DELETE FROM rag_chunks WHERE documentId = :docId")
    suspend fun deleteByDocument(docId: String)

    @Query("SELECT COUNT(*) FROM rag_chunks WHERE documentId = :docId")
    suspend fun countByDocument(docId: String): Int
}
