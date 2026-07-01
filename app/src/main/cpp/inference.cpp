#include "llama.h"
#include "ggml.h"
#include <android/log.h>
#include <cstring>
#include <string>
#include <vector>
#include <atomic>
#include <mutex>
#include <sstream>
#include <chrono>

#define LOG_TAG "LlamaDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct InferenceContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampling_context* sampler = nullptr;
    std::atomic<bool> stop_flag{false};
    int n_threads = 4;
    int n_gpu_layers = 0;
    int n_ctx = 4096;
    int n_batch = 512;
    bool flash_attn = false;
    bool mmap = true;
    std::string model_path;
    std::vector<llama_token> tokens;        // Tokenized input
    std::vector<llama_token> generated;     // Generated tokens
};

// ── Sampling from sampling.cpp ──
extern std::vector<llama_token> do_sample(
    llama_context* ctx,
    llama_sampling_context* sampler,
    int n_predict,
    std::atomic<bool>& stop_flag,
    float temperature,
    float top_p,
    int top_k,
    float repeat_penalty,
    float frequency_penalty,
    float presence_penalty,
    float min_p,
    int mirostat_mode,
    float mirostat_tau,
    float mirostat_eta
);

static InferenceContext* parse_config(InferenceContext* ctx, const char* config_json) {
    // ponytail: simple JSON field extraction, no full parser
    auto extract = [](const std::string& json, const std::string& key) -> std::string {
        auto pos = json.find("\"" + key + "\"");
        if (pos == std::string::npos) return "";
        pos = json.find(":", pos + key.size() + 2);
        if (pos == std::string::npos) return "";
        pos = json.find_first_of("0123456789.tf", pos);
        if (pos == std::string::npos) return "";
        auto end = json.find_first_of(",}\n\r", pos);
        return json.substr(pos, end - pos);
    };

    std::string json(config_json);
    auto s = extract(json, "contextSize");    if (!s.empty()) ctx->n_ctx = std::stoi(s);
    s = extract(json, "threads");             if (!s.empty()) ctx->n_threads = std::stoi(s);
    s = extract(json, "gpuLayers");           if (!s.empty()) ctx->n_gpu_layers = std::stoi(s);
    s = extract(json, "batchSize");           if (!s.empty()) ctx->n_batch = std::stoi(s);
    s = extract(json, "flashAttention");      if (s == "true") ctx->flash_attn = true;
    s = extract(json, "mmap");                if (s == "false") ctx->mmap = false;

    return ctx;
}

InferenceContext* inference_create(const char* model_path, const char* config_json) {
    auto ctx = new InferenceContext();
    parse_config(ctx, config_json);
    ctx->model_path = model_path;

    LOGI("Loading model: %s (ctx=%d, threads=%d, gpu_layers=%d)",
         model_path, ctx->n_ctx, ctx->n_threads, ctx->n_gpu_layers);

    // Model params
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = ctx->n_gpu_layers;
    model_params.use_mmap = ctx->mmap;
    model_params.use_mlock = false;

    ctx->model = llama_load_model_from_file(model_path, model_params);
    if (!ctx->model) {
        LOGE("Failed to load model from %s", model_path);
        delete ctx;
        return nullptr;
    }

    // Context params
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = ctx->n_ctx;
    ctx_params.n_batch = ctx->n_batch;
    ctx_params.n_threads = ctx->n_threads;
    ctx_params.n_threads_batch = ctx->n_threads;
    ctx_params.flash_attn = ctx->flash_attn;

    ctx->ctx = llama_new_context_with_model(ctx->model, ctx_params);
    if (!ctx->ctx) {
        LOGE("Failed to create context");
        llama_free_model(ctx->model);
        delete ctx;
        return nullptr;
    }

    // Sampling context
    ctx->sampler = llama_sampling_init(nullptr);
    LOGI("Model loaded successfully");

    return ctx;
}

void inference_destroy(InferenceContext* ctx) {
    if (!ctx) return;
    if (ctx->sampler) llama_sampling_free(ctx->sampler);
    if (ctx->ctx) llama_free(ctx->ctx);
    if (ctx->model) llama_free_model(ctx->model);
    delete ctx;
    LOGI("Context destroyed");
}

const char* inference_generate(InferenceContext* ctx, const char* prompt, const char* params_json) {
    if (!ctx || !ctx->model || !ctx->ctx) return nullptr;

    ctx->stop_flag = false;
    ctx->generated.clear();

    // Parse sampling params from JSON
    auto extract_float = [](const std::string& json, const std::string& key, float def) -> float {
        auto pos = json.find("\"" + key + "\"");
        if (pos == std::string::npos) return def;
        pos = json.find(":", pos + key.size() + 2);
        if (pos == std::string::npos) return def;
        pos = json.find_first_of("-0123456789.", pos);
        if (pos == std::string::npos) return def;
        auto end = json.find_first_of(",}\n\r", pos);
        return std::stof(json.substr(pos, end - pos));
    };
    auto extract_int = [](const std::string& json, const std::string& key, int def) -> int {
        auto pos = json.find("\"" + key + "\"");
        if (pos == std::string::npos) return def;
        pos = json.find(":", pos + key.size() + 2);
        if (pos == std::string::npos) return def;
        pos = json.find_first_of("-0123456789", pos);
        if (pos == std::string::npos) return def;
        auto end = json.find_first_of(",}\n\r", pos);
        return std::stoi(json.substr(pos, end - pos));
    };

    std::string json(params_json);
    float temperature = extract_float(json, "temperature", 0.7f);
    float top_p = extract_float(json, "topP", 0.9f);
    int top_k = extract_int(json, "topK", 40);
    float repeat_penalty = extract_float(json, "repeatPenalty", 1.1f);
    float freq_penalty = extract_float(json, "frequencyPenalty", 0.0f);
    float pres_penalty = extract_float(json, "presencePenalty", 0.0f);
    float min_p = extract_float(json, "minP", 0.05f);
    int mirostat_mode = extract_int(json, "mirostatMode", 0);
    float mirostat_tau = extract_float(json, "mirostatTau", 5.0f);
    float mirostat_eta = extract_float(json, "mirostatEta", 0.1f);
    int max_tokens = extract_int(json, "maxTokens", 2048);
    int seed = extract_int(json, "seed", -1);

    if (seed >= 0) srand(seed);

    // Tokenize prompt
    int n_tokens = llama_tokenize(ctx->model, prompt, strlen(prompt), nullptr, 0, true, false);
    std::vector<llama_token> input_tokens(n_tokens);
    llama_tokenize(ctx->model, prompt, strlen(prompt), input_tokens.data(), n_tokens, true, false);

    // Evaluate prompt
    auto start_time = std::chrono::high_resolution_clock::now();
    llama_batch batch = llama_batch_get_one(input_tokens.data(), input_tokens.size());
    if (llama_decode(ctx->ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        return nullptr;
    }
    auto prompt_end = std::chrono::high_resolution_clock::now();
    float prompt_ms = std::chrono::duration<float, std::milli>(prompt_end - start_time).count();

    // Generate
    int n_gen = 0;
    std::string result_text;
    std::vector<llama_token> generated;
    llama_token prev_token = input_tokens.back();

    while (n_gen < max_tokens && !ctx->stop_flag) {
        // Sample next token
        auto token = llama_sampling_sample(ctx->sampler, ctx->ctx, nullptr);
        if (token == llama_token_eos(ctx->model)) break;

        generated.push_back(token);

        // Decode and append
        char buf[256];
        int n = llama_token_to_piece(ctx->model, token, buf, sizeof(buf), 0, true);
        if (n > 0) result_text.append(buf, n);

        // Evaluate this token
        llama_batch next_batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx->ctx, next_batch) != 0) break;

        n_gen++;
    }

    auto end_time = std::chrono::high_resolution_clock::now();
    float gen_ms = std::chrono::duration<float, std::milli>(end_time - prompt_end).count();

    // Build JSON result
    float prompt_tps = prompt_ms > 0 ? (input_tokens.size() / (prompt_ms / 1000.0f)) : 0;
    float gen_tps = gen_ms > 0 ? (n_gen / (gen_ms / 1000.0f)) : 0;

    static std::string result_json;
    result_json = "{\"text\":\"";
    // Escape JSON string
    for (char c : result_text) {
        if (c == '"' || c == '\\') { result_json += '\\'; result_json += c; }
        else if (c == '\n') result_json += "\\n";
        else if (c == '\t') result_json += "\\t";
        else if (c >= 32) result_json += c;
    }
    result_json += "\",\"tokensPerSec\":";
    result_json += std::to_string(gen_tps);
    result_json += ",\"tokens\":";
    result_json += std::to_string(n_gen);
    result_json += "}";

    return result_json.c_str();
}

void inference_stop(InferenceContext* ctx) {
    if (ctx) ctx->stop_flag = true;
}

void inference_reset(InferenceContext* ctx) {
    if (ctx && ctx->ctx) llama_kv_cache_clear(ctx->ctx);
}

int inference_context_usage(InferenceContext* ctx) {
    if (!ctx || !ctx->ctx) return 0;
    return llama_n_ctx(ctx->ctx); // ponytail: return total context, true usage via kv_cache_tokens
}

const char* inference_system_info() {
    static std::string info;
    info = "{\"arch\":\"";
    info += ggml_type_name(ggml_backend_cpu_get_type());
    info += "\",\"backend\":\"llama.cpp\",\"version\":\"";
    info += LLAMA_COMMIT;
    info += "\"}";
    return info.c_str();
}
