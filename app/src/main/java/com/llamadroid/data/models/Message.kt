package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("conversationId")]
)
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val tokens: Int = 0
)
