package com.llamadroid.core

import android.content.Context
import androidx.room.Room
import com.llamadroid.data.repository.ConversationDao
import com.llamadroid.data.repository.LlamaDatabase
import com.llamadroid.data.repository.MessageDao
import com.llamadroid.data.repository.ModelDao
import com.llamadroid.data.repository.RagChunkDao
import com.llamadroid.data.repository.RagDocumentDao
import com.llamadroid.data.repository.SystemPromptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LlamaDatabase =
        Room.databaseBuilder(context, LlamaDatabase::class.java, "llamadroid.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideConversationDao(db: LlamaDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: LlamaDatabase): MessageDao = db.messageDao()
    @Provides fun provideModelDao(db: LlamaDatabase): ModelDao = db.modelDao()
    @Provides fun provideSystemPromptDao(db: LlamaDatabase): SystemPromptDao = db.systemPromptDao()
    @Provides fun provideRagDocumentDao(db: LlamaDatabase): RagDocumentDao = db.ragDocumentDao()
    @Provides fun provideRagChunkDao(db: LlamaDatabase): RagChunkDao = db.ragChunkDao()
}
