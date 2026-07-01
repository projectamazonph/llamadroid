package com.llamadroid.domain.rag

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedDocument(val filename: String, val text: String, val fileType: String)

@Singleton
class DocumentProcessor @Inject constructor() {
    fun process(file: File): ProcessedDocument? {
        val ext = file.extension.lowercase()
        val text = when (ext) {
            "txt" -> file.readText()
            "md" -> file.readText()
            else -> return null
        }
        return ProcessedDocument(filename = file.name, text = text, fileType = ext)
    }
}
