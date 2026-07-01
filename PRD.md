# PRD — LlamaDroid

**Version:** 1.0 | **Status:** Approved | **Last Updated:** 2026-07-02

---

## Product Overview

LlamaDroid is an **on-device LLM inference engine for Android** — a native Kotlin app that runs large language models entirely on the user's phone. No internet required after model download. No accounts, no telemetry, no data leaves the device.

**Core Value Proposition:** Your private AI, fully offline, fully under your control.

---

## Target Audience

- **Privacy-conscious users** who don't want their chats sent to cloud APIs
- **Offline-first users** in areas with limited connectivity
- **Developers** needing local LLM access via OpenAI-compatible API
- **AI enthusiasts** wanting to experiment with models on mobile

---

## Feature List

| Feature | Description | Status |
|---------|-------------|--------|
| **Chat Interface** | Streaming token-by-token generation, markdown rendering, conversation management | ✅ Built |
| **Model Hub** | Browse/download GGUF models from HuggingFace with progress notifications | ✅ Built |
| **System Prompts** | 6 built-in presets + custom presets | ✅ Built |
| **Parameter Suite** | Temperature, top-p/k, mirostat v2, min-p, penalty settings | ✅ Built |
| **Performance Profiles** | One-tap presets (Creative / Precise / Fast) | ✅ Built |
| **RAG** | Import TXT/MD documents, auto-chunk, keyword search, context injection | ✅ Built |
| **Server Mode** | OpenAI-compatible API, live metrics, Quick Settings tile, LAN access | ✅ Built |
| **Multi-modal** | Image attachment (vision models), STT, TTS | ✅ Built |
| **Speculative Decoding** | Draft model support with configurable lookahead | ✅ Built |
| **LoRA Adapters** | Import and load/unload adapters at runtime | ✅ Built |
| **Benchmark** | Prompt/generation speed, memory usage | ✅ Built |
| **Conversations** | Save, browse, export/import as JSON | ✅ Built |
| **Onboarding** | First-launch wizard with device tier detection | ✅ Built |

---

## Technical Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin (Android native) |
| **Inference Engine** | llama.cpp (C++ via JNI) |
| **Model Format** | GGUF |
| **Model Source** | HuggingFace Hub |
| **Min API Level** | Android 8.0 (API 26) |
| **Storage** | Local filesystem |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| GitHub stars | >500 |
| Model downloads | >10,000 |
| Active users (DAU) | >500 |
| App size (APK) | <50MB |
| Inference speed | >10 tok/s (7B model, flagship device) |
