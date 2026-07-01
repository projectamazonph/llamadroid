package com.llamadroid.domain.rag

import javax.inject.Inject
import javax.inject.Singleton

data class Chunk(val index: Int, val text: String)

@Singleton
class TextChunker @Inject constructor() {

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 512
        private const val DEFAULT_OVERLAP = 64
    }

    fun chunk(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<Chunk> {
        if (text.isBlank() || chunkSize <= 0) return emptyList()
        val paragraphs = text.split("\n\n")
        val chunks = mutableListOf<Chunk>()
        val current = StringBuilder()
        var index = 0

        for (para in paragraphs) {
            val words = para.split(" ")
            for (word in words) {
                val nextLen = if (current.isEmpty()) word.length else current.length + 1 + word.length
                if (nextLen > chunkSize && current.isNotEmpty()) {
                    chunks.add(Chunk(index, current.toString().trim()))
                    index++
                    // Keep last `overlap` characters for context
                    val overlapText = current.takeLast(overlap * 5).split(" ").drop(1).joinToString(" ")
                    current.clear()
                    current.append(overlapText)
                    if (overlapText.isNotBlank()) current.append(" ")
                    current.append(word)
                } else {
                    if (current.isNotEmpty()) current.append(" ")
                    current.append(word)
                }
            }
            current.append("\n\n")
        }

        if (current.isNotBlank()) {
            chunks.add(Chunk(index, current.toString().trim()))
        }

        return chunks
    }
}
