package com.llamadroid.domain.rag

import com.llamadroid.data.models.RagChunk
import com.llamadroid.data.models.RagDocument
import com.llamadroid.data.repository.RagChunkDao
import com.llamadroid.data.repository.RagDocumentDao
import com.llamadroid.domain.rag.DocumentProcessor
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class RagContext(
    val chunks: List<RagChunk>,
    val formattedContext: String
)

@Singleton
class RagEngine @Inject constructor(
    private val documentDao: RagDocumentDao,
    private val chunkDao: RagChunkDao,
    private val processor: DocumentProcessor,
    private val chunker: TextChunker
) {

    suspend fun importDocument(file: File): Boolean {
        val processed = processor.process(file) ?: return false
        val docId = UUID.randomUUID().toString()

        val doc = RagDocument(
            id = docId,
            filename = processed.filename,
            fileType = processed.fileType,
            fileSize = file.length(),
            status = "processing"
        )
        documentDao.upsert(doc)

        val chunks = chunker.chunk(processed.text)
        val ragChunks = chunks.map { chunk ->
            RagChunk(
                id = "${docId}_chunk_${chunk.index}",
                documentId = docId,
                chunkIndex = chunk.index,
                textContent = chunk.text
            )
        }
        chunkDao.insertAll(ragChunks)

        documentDao.upsert(doc.copy(chunkCount = ragChunks.size, status = "ready"))
        return true
    }

    suspend fun deleteDocument(docId: String) {
        chunkDao.deleteByDocument(docId)
        documentDao.delete(docId)
    }

    suspend fun search(query: String, limit: Int = 5): List<RagChunk> {
        if (query.isBlank()) return emptyList()
        return chunkDao.search(query, limit)
    }

    suspend fun searchByDocument(docId: String): List<RagChunk> {
        return chunkDao.byDocument(docId)
    }

    suspend fun buildContext(query: String, maxChunks: Int = 3): RagContext {
        val chunks = search(query, maxChunks)
        val formatted = chunks.withIndex().joinToString("\n\n") { (i, chunk) ->
            "[Source ${i + 1}]: ${chunk.textContent}"
        }
        return RagContext(chunks, formatted)
    }

    suspend fun injectIntoPrompt(userMessage: String, systemPrompt: String): String {
        val context = buildContext(userMessage)
        if (context.chunks.isEmpty()) return systemPrompt
        return "$systemPrompt\n\nRelevant context from your documents:\n${context.formattedContext}\n\nAnswer based on the context above when relevant."
    }
}
