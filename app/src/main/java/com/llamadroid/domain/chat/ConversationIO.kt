package com.llamadroid.domain.chat

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.llamadroid.data.models.Conversation
import com.llamadroid.data.models.Message
import com.llamadroid.data.repository.ConversationDao
import com.llamadroid.data.repository.MessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ConversationExport(
    val version: Int = 1,
    val title: String,
    val modelId: String,
    val systemPrompt: String,
    val createdAt: Long,
    val messages: List<MessageExport>
)

@Serializable
data class MessageExport(
    val role: String,
    val content: String,
    val createdAt: Long,
    val tokens: Int = 0
)

@Singleton
class ConversationIO @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    private val json = Json { prettyPrint = true }

    suspend fun export(conversationId: String, context: Context): File? = withContext(Dispatchers.IO) {
        val conv = conversationDao.get(conversationId) ?: return@withContext null
        val msgs = messageDao.byConversation(conversationId).first()

        val export = ConversationExport(
            title = conv.title, modelId = conv.modelId,
            systemPrompt = conv.systemPrompt, createdAt = conv.createdAt,
            messages = msgs.map { MessageExport(it.role, it.content, it.createdAt, it.tokens) }
        )

        val file = File(context.cacheDir, "exports/${conv.title.take(40).replace(" ", "_")}.llamadroid.json")
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(export))
        file
    }

    fun share(file: File, context: Context) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Export conversation"))
    }

    suspend fun import(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val export = Json.decodeFromString<ConversationExport>(jsonString)
            val convId = "imported_${System.currentTimeMillis()}"
            conversationDao.upsert(Conversation(id = convId, title = export.title, modelId = export.modelId, systemPrompt = export.systemPrompt, createdAt = export.createdAt))
            export.messages.forEach { msg ->
                messageDao.insert(Message(id = "imp_${convId}_${msg.createdAt}", conversationId = convId, role = msg.role, content = msg.content, createdAt = msg.createdAt, tokens = msg.tokens))
            }
            true
        } catch (_: Exception) { false }
    }
}
