#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <string>
#include <map>
#include <mutex>
#include "llama.h"
#include "ggml.h"

#define LOG_TAG "LlamaDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declarations from implementation files
struct InferenceContext;
InferenceContext* inference_create(const char* model_path, const char* config_json);
void inference_destroy(InferenceContext* ctx);
const char* inference_generate(InferenceContext* ctx, const char* prompt, const char* params_json);
void inference_stop(InferenceContext* ctx);
void inference_reset(InferenceContext* ctx);
int inference_context_usage(InferenceContext* ctx);
float* inference_embed(InferenceContext* ctx, const char* text, int* out_dim);
bool inference_load_lora(InferenceContext* ctx, const char* lora_path, float scale);
bool inference_unload_lora(InferenceContext* ctx);
const char* inference_benchmark(const char* model_path, const char* config_json);
const char* inference_system_info();
const char* inference_model_info(const char* model_path);

// Context registry (thread-safe)
static std::map<jlong, InferenceContext*> contexts;
static std::mutex contexts_mutex;
static jlong next_handle = 1;

static jlong register_context(InferenceContext* ctx) {
    std::lock_guard<std::mutex> lock(contexts_mutex);
    jlong handle = next_handle++;
    contexts[handle] = ctx;
    return handle;
}

static InferenceContext* get_context(jlong handle) {
    std::lock_guard<std::mutex> lock(contexts_mutex);
    auto it = contexts.find(handle);
    return (it != contexts.end()) ? it->second : nullptr;
}

static void unregister_context(jlong handle) {
    std::lock_guard<std::mutex> lock(contexts_mutex);
    contexts.erase(handle);
}

static jstring str_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// ── JNI exports ──────────────────────────────────────────────

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_llamadroid_data_native_NativeLib_init(
    JNIEnv* env, jclass, jstring model_path, jstring config_json) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    const char* config = env->GetStringUTFChars(config_json, nullptr);
    LOGI("init(%s)", path);

    InferenceContext* ctx = inference_create(path, config);

    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(config_json, config);

    if (!ctx) {
        LOGE("Failed to create inference context");
        return 0;
    }
    return register_context(ctx);
}

JNIEXPORT void JNICALL
Java_com_llamadroid_data_native_NativeLib_release(JNIEnv*, jclass, jlong handle) {
    LOGI("release(%lld)", (long long)handle);
    InferenceContext* ctx = get_context(handle);
    if (ctx) { inference_destroy(ctx); unregister_context(handle); }
}

JNIEXPORT jstring JNICALL
Java_com_llamadroid_data_native_NativeLib_generate(
    JNIEnv* env, jclass, jlong handle, jstring prompt, jstring params_json) {
    InferenceContext* ctx = get_context(handle);
    if (!ctx) return str_to_jstring(env, "{\"error\":\"invalid context\"}");

    const char* p = env->GetStringUTFChars(prompt, nullptr);
    const char* params = env->GetStringUTFChars(params_json, nullptr);

    const char* result = inference_generate(ctx, p, params);

    env->ReleaseStringUTFChars(prompt, p);
    env->ReleaseStringUTFChars(params_json, params);

    return str_to_jstring(env, result ? result : "{\"error\":\"generation failed\"}");
}

JNIEXPORT void JNICALL
Java_com_llamadroid_data_native_NativeLib_stop(JNIEnv*, jclass, jlong handle) {
    InferenceContext* ctx = get_context(handle);
    if (ctx) inference_stop(ctx);
}

JNIEXPORT void JNICALL
Java_com_llamadroid_data_native_NativeLib_resetContext(JNIEnv*, jclass, jlong handle) {
    InferenceContext* ctx = get_context(handle);
    if (ctx) inference_reset(ctx);
}

JNIEXPORT jint JNICALL
Java_com_llamadroid_data_native_NativeLib_contextUsage(JNIEnv*, jclass, jlong handle) {
    InferenceContext* ctx = get_context(handle);
    return ctx ? inference_context_usage(ctx) : 0;
}

JNIEXPORT jstring JNICALL
Java_com_llamadroid_data_native_NativeLib_embed(
    JNIEnv* env, jclass, jlong handle, jstring text) {
    InferenceContext* ctx = get_context(handle);
    if (!ctx) return str_to_jstring(env, "[]");

    const char* t = env->GetStringUTFChars(text, nullptr);
    int dim = 0;
    float* emb = inference_embed(ctx, t, &dim);
    env->ReleaseStringUTFChars(text, t);

    if (!emb || dim == 0) return str_to_jstring(env, "[]");

    std::string json = "[";
    for (int i = 0; i < dim; i++) {
        if (i > 0) json += ",";
        json += std::to_string(emb[i]);
    }
    json += "]";
    free(emb);
    return str_to_jstring(env, json);
}

JNIEXPORT jboolean JNICALL
Java_com_llamadroid_data_native_NativeLib_loadLora(
    JNIEnv* env, jclass, jlong handle, jstring lora_path, jfloat scale) {
    InferenceContext* ctx = get_context(handle);
    if (!ctx) return JNI_FALSE;
    const char* path = env->GetStringUTFChars(lora_path, nullptr);
    bool ok = inference_load_lora(ctx, path, (float)scale);
    env->ReleaseStringUTFChars(lora_path, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_llamadroid_data_native_NativeLib_unloadLora(JNIEnv*, jclass, jlong handle) {
    InferenceContext* ctx = get_context(handle);
    return ctx && inference_unload_lora(ctx) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_llamadroid_data_native_NativeLib_benchmark(
    JNIEnv* env, jclass, jstring model_path, jstring config_json) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    const char* config = env->GetStringUTFChars(config_json, nullptr);
    const char* result = inference_benchmark(path, config);
    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(config_json, config);
    return str_to_jstring(env, result ? result : "{\"error\":\"benchmark failed\"}");
}

JNIEXPORT jstring JNICALL
Java_com_llamadroid_data_native_NativeLib_systemInfo(JNIEnv* env, jclass) {
    return str_to_jstring(env, inference_system_info());
}

JNIEXPORT jstring JNICALL
Java_com_llamadroid_data_native_NativeLib_modelInfo(
    JNIEnv* env, jclass, jstring model_path) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    const char* info = inference_model_info(path);
    env->ReleaseStringUTFChars(model_path, path);
    return str_to_jstring(env, info ? info : "{}");
}

} // extern "C"
