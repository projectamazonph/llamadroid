package com.llamadroid.domain.inference

import com.llamadroid.data.native.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GenerationResult(val text: String = "", val tokensPerSec: Float = 0f, val tokens: Int = 0, val error: String? = null)

@Serializable
data class ModelInfo(val desc: String = "", val contextLength: Int = 0, val embeddingLength: Int = 0, val layers: Int = 0, val size: Long = 0, val parameters: Long = 0, val error: String? = null)

@Serializable
data class BenchmarkResult(val modelName: String = "", val promptTokensPerSec: Float = 0f, val genTokensPerSec: Float = 0f, val totalTokens: Int = 0, val peakMemoryMb: Long = 0, val contextSize: Int = 0, val threadsUsed: Int = 0, val gpuLayers: Int = 0, val durationMs: Long = 0, val error: String? = null)

@Singleton
class InferenceEngine @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val contexts = mutableMapOf<String, Long>()

    suspend fun loadModel(modelPath: String, config: ContextConfig = ContextConfig()): Boolean = withContext(Dispatchers.Default) {
        try {
            NativeLib.ensureLoaded()
            val handle = NativeLib.init(modelPath, json.encodeToString(config))
            if (handle == 0L) return@withContext false
            contexts[modelPath] = handle; true
        } catch (_: Exception) { false }
    }

    suspend fun generate(modelPath: String, prompt: String, params: SamplingParams = SamplingParams()): GenerationResult = withContext(Dispatchers.Default) {
        val handle = contexts[modelPath] ?: return@withContext GenerationResult(error = "Model not loaded")
        try { json.decodeFromString<GenerationResult>(NativeLib.generate(handle, prompt, json.encodeToString(params))) }
        catch (e: Exception) { GenerationResult(error = e.message) }
    }

    fun stop(modelPath: String) { contexts[modelPath]?.let { NativeLib.stop(it) } }

    fun unloadModel(modelPath: String) { contexts[modelPath]?.let { NativeLib.release(it) }; contexts.remove(modelPath) }

    suspend fun embed(modelPath: String, text: String): FloatArray? = withContext(Dispatchers.Default) {
        val handle = contexts[modelPath] ?: return@withContext null
        try {
            val cleaned = NativeLib.embed(handle, text).removeSurrounding("[", "]")
            if (cleaned.isBlank()) null else cleaned.split(",").map { it.trim().toFloat() }.toFloatArray()
        } catch (_: Exception) { null }
    }

    suspend fun getModelInfo(modelPath: String): ModelInfo = withContext(Dispatchers.Default) {
        try { NativeLib.ensureLoaded(); json.decodeFromString<ModelInfo>(NativeLib.modelInfo(modelPath)) }
        catch (e: Exception) { ModelInfo(error = e.message) }
    }

    suspend fun runBenchmark(modelPath: String, config: ContextConfig = ContextConfig()): String = withContext(Dispatchers.Default) {
        NativeLib.ensureLoaded(); NativeLib.benchmark(modelPath, json.encodeToString(config))
    }

    suspend fun getSystemInfo(): String = withContext(Dispatchers.Default) { NativeLib.ensureLoaded(); NativeLib.systemInfo() }
}
