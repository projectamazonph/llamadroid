package com.llamadroid.domain.inference

import kotlinx.serialization.Serializable

@Serializable
data class SamplingParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val minP: Float = 0.05f,
    val mirostatMode: Int = 0,
    val mirostatTau: Float = 5.0f,
    val mirostatEta: Float = 0.1f,
    val maxTokens: Int = 2048,
    val seed: Int = -1,
    val draftModelPath: String = "",
    val speculativeLookahead: Int = 16
) {
    val speculativeDecodingEnabled: Boolean get() = draftModelPath.isNotBlank()
}
