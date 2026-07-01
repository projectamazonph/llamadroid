# Development Plan — LlamaDroid

**Version:** 1.0 | **Status:** Active | **Last Updated:** 2026-07-02

---

## Phase 1: Core Engine

**Status:** ✅ COMPLETE

| Task | Details |
|------|---------|
| llama.cpp JNI integration | Native C++ inference via JNI bridge |
| GGUF model loader | Load + run GGUF format models |
| Basic token generation | Streaming token-by-token generation |
| Device detection | Tier-based performance detection |

## Phase 2: Chat & Management

**Status:** ✅ COMPLETE

| Task | Details |
|------|---------|
| Chat interface | Streaming UI with markdown rendering |
| Conversation management | Save, browse, export/import as JSON |
| Model Hub | Browse/download models from HuggingFace |
| System prompts | 6 presets + custom |
| Parameter controls | Temperature, top-p/k, penalties, mirostat |

## Phase 3: Advanced Features

**Status:** ✅ COMPLETE

| Task | Details |
|------|---------|
| Performance profiles | Creative / Precise / Fast presets |
| RAG system | Document import, chunking, keyword search |
| Server Mode | OpenAI-compatible API + Quick Settings tile |
| Multi-modal | Vision models, STT, TTS |
| Speculative Decoding | Draft model support |
| LoRA Adapters | Load/unload at runtime |
| Benchmark | Speed and memory measurements |

## Phase 4: Polish & Release

**Status:** 🟡 ACTIVE

| Task | Priority |
|------|----------|
| Onboarding wizard | High |
| Error handling & crash reporting | High |
| App store submission (F-Droid) | Medium |
| User documentation | Medium |
| GitHub CI/CD | Medium |
| Performance optimization | High |
