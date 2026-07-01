package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rag_chunks",
    foreignKeys = [ForeignKey(
        entity = RagDocument::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("documentId")]
)
data class RagChunk(
    @PrimaryKey val id: String,
    val documentId: String,
    val chunkIndex: Int = 0,
    val textContent: String,
    val createdAt: Long = System.currentTimeMillis()
)
