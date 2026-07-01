package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rag_documents")
data class RagDocument(
    @PrimaryKey val id: String,
    val filename: String,
    val fileType: String,  // "txt", "md", "pdf"
    val fileSize: Long = 0,
    val chunkCount: Int = 0,
    val importedAt: Long = System.currentTimeMillis(),
    val status: String = "ready"  // "pending", "processing", "ready", "error"
)
