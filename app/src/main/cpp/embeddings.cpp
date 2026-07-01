#include "llama.h"
#include <android/log.h>
#include <cstring>
#include <vector>
#include <cstdlib>

#define LOG_TAG "LlamaDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct InferenceContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    void* sampler = nullptr;
    int n_threads = 4;
    int n_gpu_layers = 0;
    int n_ctx = 4096;
    int n_batch = 512;
    bool flash_attn = false;
    bool mmap = true;
    std::string model_path;
};

float* inference_embed(InferenceContext* ctx, const char* text, int* out_dim) {
    if (!ctx || !ctx->model || !ctx->ctx) { *out_dim = 0; return nullptr; }

    int n_tokens = llama_tokenize(ctx->model, text, strlen(text), nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(ctx->model, text, strlen(text), tokens.data(), n_tokens, true, true);

    llama_kv_cache_clear(ctx->ctx);

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(ctx->ctx, batch) != 0) {
        LOGE("Embedding decode failed");
        *out_dim = 0;
        return nullptr;
    }

    const float* emb = llama_get_embeddings(ctx->ctx);
    if (!emb) {
        LOGE("No embeddings returned");
        *out_dim = 0;
        return nullptr;
    }

    int n_embd = llama_n_embd(ctx->model);
    *out_dim = n_embd;

    float* result = (float*)malloc(n_embd * sizeof(float));
    if (result) memcpy(result, emb, n_embd * sizeof(float));

    return result;
}

bool inference_load_lora(InferenceContext* ctx, const char* lora_path, float scale) {
    LOGI("loadLora(%s, %f)", lora_path, (float)scale);
    if (!ctx || !ctx->model) return false;
    // llama.cpp LoRA loading API
    return llama_model_load_lora(ctx->model, lora_path, scale, nullptr, nullptr) == 0;
}

bool inference_unload_lora(InferenceContext* ctx) {
    if (!ctx || !ctx->model) return false;
    // Unload LoRA by reloading without it
    // ponytail: full unload requires model reload, accept limitation
    LOGI("unloadLora (stub - requires model reload for full unload)");
    return true;
}
