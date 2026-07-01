#include "llama.h"
#include "ggml.h"
#include <android/log.h>
#include <cstring>
#include <string>
#include <sstream>
#include <chrono>
#include <vector>

#define LOG_TAG "LlamaDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct InferenceContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    void* sampler = nullptr;
    int n_ctx = 4096;
    int n_threads = 4;
};

const char* inference_model_info(const char* model_path) {
    static std::string result;
    auto mparams = llama_model_default_params();
    mparams.use_mmap = false;
    auto model = llama_load_model_from_file(model_path, mparams);
    if (!model) { result = "{\"error\":\"failed to load model\"}"; return result.c_str(); }

    auto desc = llama_model_desc(model);
    auto n_ctx_train = llama_n_ctx_train(model);
    auto n_embd = llama_n_embd(model);
    auto n_layers = llama_n_layer(model);
    auto size = llama_model_size(model);
    auto n_params = llama_model_n_params(model);

    std::stringstream ss;
    ss << "{";
    ss << "\"desc\":\"" << desc << "\",";
    ss << "\"contextLength\":" << n_ctx_train << ",";
    ss << "\"embeddingLength\":" << n_embd << ",";
    ss << "\"layers\":" << n_layers << ",";
    ss << "\"size\":" << size << ",";
    ss << "\"parameters\":" << n_params;
    ss << "}";

    llama_free_model(model);
    result = ss.str();
    return result.c_str();
}

const char* inference_benchmark(const char* model_path, const char* config_json) {
    static std::string result;
    auto mparams = llama_model_default_params();
    mparams.use_mmap = true;

    auto start_time = std::chrono::high_resolution_clock::now();
    auto model = llama_load_model_from_file(model_path, mparams);
    if (!model) return "{\"error\":\"failed to load model\"}";

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 512;
    cparams.n_batch = 512;
    auto ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) { llama_free_model(model); return "{\"error\":\"failed to create context\"}"; }

    const char* prompt = "Hello world. This is a benchmark prompt for llama.cpp on Android.";
    int n_tokens = llama_tokenize(model, prompt, strlen(prompt), nullptr, 0, true, false);
    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(model, prompt, strlen(prompt), tokens.data(), n_tokens, true, false);

    auto bench_start = std::chrono::high_resolution_clock::now();
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    llama_decode(ctx, batch);
    auto bench_end = std::chrono::high_resolution_clock::now();
    float prompt_ms = std::chrono::duration<float, std::milli>(bench_end - bench_start).count();
    float total_ms = std::chrono::duration<float, std::milli>(bench_end - start_time).count();
    auto model_size = llama_model_size(model);

    auto gen_start = std::chrono::high_resolution_clock::now();
    int n_gen = 0;
    for (int i = 0; i < 50; i++) {
        auto logits = llama_get_logits(ctx);
        auto n_vocab = llama_n_vocab(model);
        // Simple argmax for benchmark
        llama_token max_token = 0;
        float max_val = logits[0];
        for (llama_token t = 1; t < n_vocab; t++) {
            if (logits[t] > max_val) { max_val = logits[t]; max_token = t; }
        }
        if (max_token == llama_token_eos(model)) break;
        llama_batch next = llama_batch_get_one(&max_token, 1);
        if (llama_decode(ctx, next) != 0) break;
        n_gen++;
    }
    auto gen_end = std::chrono::high_resolution_clock::now();
    float gen_ms = std::chrono::duration<float, std::milli>(gen_end - gen_start).count();
    float gen_tps = gen_ms > 0 ? (n_gen / (gen_ms / 1000.0f)) : 0;

    llama_free(ctx);
    llama_free_model(model);

    std::stringstream ss;
    ss << "{";
    // desc will be wrong after free but we saved it before
    ss << "\"promptTokensPerSec\":" << (prompt_ms > 0 ? (n_tokens / (prompt_ms / 1000.0f)) : 0) << ",";
    ss << "\"genTokensPerSec\":" << gen_tps << ",";
    ss << "\"totalTokens\":" << n_gen << ",";
    ss << "\"peakMemoryMb\":" << (model_size / (1024 * 1024)) << ",";
    ss << "\"contextSize\":512,";
    ss << "\"threadsUsed\":4,";
    ss << "\"durationMs\":" << (long long)total_ms;
    ss << "}";

    result = ss.str();
    return result.c_str();
}
