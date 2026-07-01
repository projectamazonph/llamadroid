package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String,
    val systemPrompt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tokenCount: Int = 0,
    val messageCount: Int = 0
)
