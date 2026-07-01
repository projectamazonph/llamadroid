#include "llama.h"
#include <android/log.h>
#include <vector>
#include <cstdlib>
#include <cmath>
#include <algorithm>

#define LOG_TAG "LlamaDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ── Sampling implementations ──

static float get_random() {
    return (float)rand() / (float)RAND_MAX;
}

// Temperature sampling with top-p filtering
static llama_token sample_top_p_top_k(
    llama_context* ctx,
    float temperature,
    float top_p,
    int top_k
) {
    auto logits = llama_get_logits(ctx);
    auto n_vocab = llama_n_vocab(llama_get_model(ctx));

    std::vector<std::pair<float, llama_token>> candidates;
    candidates.reserve(n_vocab);

    for (llama_token i = 0; i < n_vocab; i++) {
        candidates.emplace_back(logits[i], i);
    }

    // Apply temperature
    if (temperature > 0.0f) {
        for (auto& c : candidates) c.first /= temperature;
    }

    // Softmax
    float max_logit = std::max_element(candidates.begin(), candidates.end(),
        [](auto& a, auto& b) { return a.first < b.first; })->first;
    float sum = 0.0f;
    for (auto& c : candidates) {
        c.first = expf(c.first - max_logit);
        sum += c.first;
    }
    for (auto& c : candidates) c.first /= sum;

    // Sort by probability descending
    std::sort(candidates.begin(), candidates.end(),
        [](auto& a, auto& b) { return a.first > b.first; });

    // Top-K truncation
    if (top_k > 0 && top_k < (int)candidates.size())
        candidates.resize(top_k);

    // Top-P truncation
    float cumsum = 0.0f;
    size_t last_idx = candidates.size();
    for (size_t i = 0; i < candidates.size(); i++) {
        cumsum += candidates[i].first;
        if (cumsum > top_p) { last_idx = i + 1; break; }
    }
    if (last_idx < candidates.size()) candidates.resize(last_idx);

    // Sample from filtered distribution
    float r = get_random();
    cumsum = 0.0f;
    for (auto& c : candidates) {
        cumsum += c.first;
        if (r < cumsum) return c.second;
    }
    return candidates.empty() ? 0 : candidates[0].second;
}

// Mirostat v2 sampling
static llama_token sample_mirostat_v2(
    llama_context* ctx,
    float tau,
    float eta,
    int* mu,
    int n_vocab
) {
    auto logits = llama_get_logits(ctx);

    std::vector<std::pair<float, llama_token>> candidates;
    candidates.reserve(n_vocab);
    for (llama_token i = 0; i < n_vocab; i++)
        candidates.emplace_back(logits[i], i);

    // Softmax to get probabilities
    float max_l = candidates[0].first;
    for (auto& c : candidates) if (c.first > max_l) max_l = c.first;
    float sum = 0.0f;
    for (auto& c : candidates) {
        c.first = expf(c.first - max_l);
        sum += c.first;
    }
    for (auto& c : candidates) c.first /= sum;

    std::sort(candidates.begin(), candidates.end(),
        [](auto& a, auto& b) { return a.first > b.first; });

    // Estimate s (surprisal)
    float s = *mu / tau;
    int target = std::max(1, (int)expf(s));
    target = std::min(target, n_vocab);

    // Find top token index
    float r = get_random();
    float cumsum = 0.0f;
    llama_token token = candidates[0].second;
    for (int i = 0; i < target && i < (int)candidates.size(); i++) {
        cumsum += candidates[i].first;
        if (r < cumsum) { token = candidates[i].second; break; }
        if (i == target - 1) token = candidates[i].second;
    }

    // Update mu
    float p = 0.0f;
    for (auto& c : candidates) if (c.second == token) { p = c.first; break; }
    float s_new = -logf(std::max(p, 1e-10f));
    *mu = *mu + (int)(eta * (s_new - s));

    return token;
}

// ── Public API ──

std::vector<llama_token> do_sample(
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
) {
    auto model = llama_get_model(ctx);
    auto n_vocab = llama_n_vocab(model);
    std::vector<llama_token> output;
    int mirostat_mu = mirostat_tau * 2;
    output.reserve(n_predict);

    for (int i = 0; i < n_predict && !stop_flag; i++) {
        llama_token token;
        if (mirostat_mode == 2) {
            token = sample_mirostat_v2(ctx, mirostat_tau, mirostat_eta, &mirostat_mu, n_vocab);
        } else {
            token = sample_top_p_top_k(ctx, temperature, top_p, top_k);
        }

        if (token == llama_token_eos(model)) break;
        output.push_back(token);

        // Evaluate this token
        llama_batch batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, batch) != 0) break;
    }

    return output;
}
