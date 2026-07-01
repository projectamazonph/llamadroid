package com.llamadroid.data.huggingface

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class HfModel(
    val id: String,
    val likes: Int = 0,
    val downloads: Int = 0,
    val siblings: List<HfFile> = emptyList()
)

@Serializable
data class HfFile(val rfilename: String, val fileSize: Long = 0)

@Singleton
class HfApi @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val HF_API = "https://huggingface.co/api/models"
        private const val TIMEOUT_MS = 15000
    }

    suspend fun search(query: String, limit: Int = 30): List<HfModel> {
        val url = URL("$HF_API?search=$query&sort=downloads&direction=-1&limit=$limit")
        val body = httpGet(url) ?: return emptyList()
        return json.decodeFromString<List<HfModel>>(body).filter { hasGguf(it) }
    }

    suspend fun listModels(task: String = "text-generation", limit: Int = 50): List<HfModel> {
        val url = URL("$HF_API?pipeline_tag=$task&sort=downloads&direction=-1&limit=$limit")
        val body = httpGet(url) ?: return emptyList()
        return json.decodeFromString<List<HfModel>>(body).filter { hasGguf(it) }
    }

    suspend fun getModel(id: String): HfModel? {
        val url = URL("$HF_API/$id")
        val body = httpGet(url) ?: return null
        return json.decodeFromString<HfModel>(body)
    }

    fun ggufFiles(model: HfModel): List<HfFile> =
        model.siblings.filter { it.rfilename.endsWith(".gguf") }

    private fun hasGguf(model: HfModel): Boolean =
        model.siblings.any { it.rfilename.endsWith(".gguf") }

    private fun httpGet(url: URL): String? {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        return try {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            reader.readText()
        } finally {
            conn.disconnect()
        }
    }
}
