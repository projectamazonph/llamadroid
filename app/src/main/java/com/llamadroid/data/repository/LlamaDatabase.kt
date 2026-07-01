package com.llamadroid.data.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.llamadroid.data.models.Conversation
import com.llamadroid.data.models.DownloadedModel
import com.llamadroid.data.models.Message
import com.llamadroid.data.models.RagChunk
import com.llamadroid.data.models.RagDocument
import com.llamadroid.data.models.SystemPrompt

@Database(
    entities = [Conversation::class, Message::class, DownloadedModel::class, SystemPrompt::class, RagDocument::class, RagChunk::class],
    version = 4,
    exportSchema = false
)
abstract class LlamaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun modelDao(): ModelDao
    abstract fun systemPromptDao(): SystemPromptDao
    abstract fun ragDocumentDao(): RagDocumentDao
    abstract fun ragChunkDao(): RagChunkDao
}
