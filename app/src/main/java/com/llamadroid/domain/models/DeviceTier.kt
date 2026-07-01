package com.llamadroid.domain.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DeviceTier(val label: String, val maxModelSizeGb: Int, val recommendedQuant: String) {
    FLAGSHIP("Flagship", 8, "Q4_K_M"),
    MID_RANGE("Mid-range", 4, "Q4_K_M"),
    BUDGET("Budget", 2, "Q4_K_M"),
    LOW_END("Low-end", 1, "Q4_K_M");

    companion object {
        suspend fun detect(): DeviceTier = withContext(Dispatchers.Default) {
            val cores = Runtime.getRuntime().availableProcessors()
            val maxHeap = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024)
            when {
                cores >= 8 && maxHeap >= 4 -> FLAGSHIP
                cores >= 6 -> MID_RANGE
                cores >= 4 -> BUDGET
                else -> LOW_END
            }
        }
    }
}

data class RecommendedModel(
    val hfId: String,
    val name: String,
    val paramSize: String,
    val quant: String,
    val fileSizeGb: Float,
    val ggufFile: String
)

val recommendedModels = mapOf(
    DeviceTier.FLAGSHIP to listOf(
        RecommendedModel("bartowski/Mistral-Nemo-Instruct-2407-GGUF", "Mistral Nemo", "12B", "Q4_K_M", 7.0f, "Mistral-Nemo-Instruct-2407-Q4_K_M.gguf"),
        RecommendedModel("QuantFactory/Meta-Llama-3-8B-Instruct-GGUF", "Llama 3 8B", "8B", "Q4_K_M", 4.9f, "Meta-Llama-3-8B-Instruct.Q4_K_M.gguf"),
    ),
    DeviceTier.MID_RANGE to listOf(
        RecommendedModel("QuantFactory/Meta-Llama-3-8B-Instruct-GGUF", "Llama 3 8B", "8B", "Q4_K_M", 4.9f, "Meta-Llama-3-8B-Instruct.Q4_K_M.gguf"),
        RecommendedModel("QuantFactory/Microsoft-Phi-3-medium-4k-instruct-GGUF", "Phi-3 Medium", "14B", "Q4_K_M", 7.8f, "Phi-3-medium-4k-instruct.Q4_K_M.gguf"),
    ),
    DeviceTier.BUDGET to listOf(
        RecommendedModel("QuantFactory/Microsoft-Phi-3-mini-4k-instruct-GGUF", "Phi-3 Mini", "3.8B", "Q4_K_M", 2.2f, "Phi-3-mini-4k-instruct.Q4_K_M.gguf"),
        RecommendedModel("QuantFactory/gemma-2b-it-GGUF", "Gemma 2B", "2B", "Q4_K_M", 1.2f, "gemma-2b-it.Q4_K_M.gguf"),
    ),
    DeviceTier.LOW_END to listOf(
        RecommendedModel("QuantFactory/gemma-2b-it-GGUF", "Gemma 2B", "2B", "Q4_K_M", 1.2f, "gemma-2b-it.Q4_K_M.gguf"),
        RecommendedModel("QuantFactory/TinyLlama-1.1B-Chat-v1.0-GGUF", "TinyLlama 1.1B", "1.1B", "Q4_K_M", 0.7f, "TinyLlama-1.1B-Chat-v1.0.Q4_K_M.gguf"),
    )
)
