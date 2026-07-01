package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_prompts")
data class SystemPrompt(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val isBuiltin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
