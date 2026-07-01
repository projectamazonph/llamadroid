package com.llamadroid.data.native

/**
 * JNI bridge to llama.cpp native library.
 * All long handles are opaque pointers to native C++ InferenceContext objects.
 */
object NativeLib {
    private var loaded = false

    fun ensureLoaded() {
        if (!loaded) {
            System.loadLibrary("llamadroid")
            loaded = true
        }
    }

    /** Initialize a model. Returns context handle, or 0 on failure. */
    external fun init(modelPath: String, configJson: String): Long

    /** Release all resources for a context handle. */
    external fun release(contextHandle: Long)

    /** Synchronous generation. Returns JSON: {"text":"...","tokensPerSec":float,"tokens":int}. */
    external fun generate(contextHandle: Long, prompt: String, paramsJson: String): String

    /** Cancel an in-progress generation. Safe from any thread. */
    external fun stop(contextHandle: Long)

    /** Clear the KV cache for a fresh start. */
    external fun resetContext(contextHandle: Long)

    /** Tokens used in current context window. */
    external fun contextUsage(contextHandle: Long): Int

    /** Generate embeddings. Returns JSON array of floats. */
    external fun embed(contextHandle: Long, text: String): String

    /** Load a LoRA adapter. */
    external fun loadLora(contextHandle: Long, loraPath: String, scale: Float): Boolean

    /** Unload LoRA adapter. */
    external fun unloadLora(contextHandle: Long): Boolean

    /** Run a benchmark. Returns JSON with speed/memory metrics. */
    external fun benchmark(modelPath: String, configJson: String): String

    /** System information JSON. */
    external fun systemInfo(): String

    /** Model metadata JSON (type, context length, layers, parameters). */
    external fun modelInfo(modelPath: String): String
}
