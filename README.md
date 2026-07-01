# LlamaDroid

On-device LLM inference for Android — local, private, full control.

Run large language models entirely on your phone. No internet required after model download. No accounts, no telemetry, no data leaves your device.

## Features

- **Chat** — streaming token-by-token generation, markdown rendering, conversation management
- **Model Hub** — browse and download GGUF models directly from HuggingFace with progress notifications
- **System Prompts** — 6 built-in presets (Creative, Precise, Code, Translator, Summarizer, Default) + custom
- **Full Parameter Suite** — temperature, top-p/k, mirostat v2, min-p, repeat/frequency/presence penalties
- **Performance Profiles** — one-tap presets (Creative / Precise / Fast)
- **RAG** — import TXT/MD documents, auto-chunk, keyword search, context-injected chat with citations
- **Server Mode** — OpenAI-compatible API with live metrics, Quick Settings tile, public/LAN access
- **Multi-modal** — image attachment for vision models, speech-to-text, text-to-speech
- **Speculative Decoding** — draft model support with configurable lookahead
- **LoRA Adapters** — import and load/unload adapters at runtime
- **Benchmark** — measure prompt/generation speed, memory usage
- **Conversations** — save, browse, export/import as JSON
- **Onboarding** — first-launch wizard with device tier detection and model recommendations

## Quick Start

```bash
git clone --recurse-submodules https://github.com/projectamazonph/llamadroid
cd llamadroid
git submodule update --init --depth 1   # fetch llama.cpp
./gradlew assembleDebug
```

### Prerequisites

- **Android Studio** Koala+ (2024.1+)
- **NDK** 27+ (via SDK Manager)
- **CMake** 3.22+
- **Java** 17+
- **Android SDK** 35+

## Architecture

```
┌─────────────────────────────────────┐
│  Jetpack Compose UI (Material 3)    │
├─────────────────────────────────────┤
│  MVVM + Hilt DI + Room DB           │
├─────────────────────────────────────┤
│  InferenceEngine (Kotlin)           │
├─────────────────────────────────────┤
│  JNI Bridge (C++20)                  │
├─────────────────────────────────────┤
│  llama.cpp (Vulkan/OpenCL/NEON)      │
└─────────────────────────────────────┘
```

**61 Kotlin files** across clean architecture layers (data/domain/ui), **6 C++ bridge files** with full inference, sampling, embeddings, and model info implementation.

## Project Layout

```
app/src/main/
├── java/com/llamadroid/
│   ├── core/           DI modules, preferences
│   ├── data/           Room entities/DAOs, HF Hub API, JNI bridge
│   ├── domain/         Chat, inference, server, RAG, media engines
│   ├── service/        Foreground services + Quick Settings tile
│   └── ui/             Compose screens, components, navigation, theme
├── cpp/                JNI bridge + llama.cpp submodule
└── res/                Resources, manifest, file paths
```

## Build Variants

| Variant | ABIs | Use Case |
|---------|------|----------|
| `debug` | arm64-v8a, x86_64 | Development |
| `release` | arm64-v8a, armeabi-v7a, x86_64 | Production |

## License

MIT
