package com.llamadroid.domain.inference

import kotlinx.serialization.Serializable

@Serializable
data class ContextConfig(
    val contextSize: Int = 4096,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val batchSize: Int = 512,
    val flashAttention: Boolean = false,
    val mmap: Boolean = true
)
