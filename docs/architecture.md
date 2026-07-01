# Architecture — LlamaDroid

## System Architecture

```
┌──────────────────────────────────────────┐
│            Android App (Kotlin)           │
├──────────────────────────────────────────┤
│  UI Layer                                 │
│  ┌────────────────────────────────────┐   │
│  │ Chat · Model Hub · Settings · RAG  │   │
│  │ Server · Benchmark · Conversations │   │
│  └────────────────────────────────────┘   │
├──────────────────────────────────────────┤
│  Service Layer                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ Inference│ │ Download │ │ Server   │  │
│  │ Engine   │ │ Manager  │ │ Mode     │  │
│  └──────────┘ └──────────┘ └──────────┘  │
├──────────────────────────────────────────┤
│  Native Layer (JNI)                       │
│  ┌────────────────────────────────────┐   │
│  │      llama.cpp (C++)                │   │
│  │  GGUF loader · Tokenizer · Sampler │   │
│  └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

## Key Components

| Component | Description |
|-----------|-------------|
| **Inference Engine** | llama.cpp via JNI — model loading, token generation, sampling |
| **Model Hub** | HuggingFace API client — browse, download GGUF models |
| **RAG Engine** | Document import → chunking → embedding → keyword search → context injection |
| **Server Mode** | OpenAI-compatible HTTP API with live metrics dashboard |
| **Benchmark** | Token generation speed (tok/s), memory usage, prompt processing |

## Data Flow

```
User Input (text)
  → Kotlin UI
  → JNI Bridge
  → llama.cpp inference
  → Token streaming back through JNI
  → Kotlin renders markdown in real-time
  → Conversation saved to local SQLite/JSON
```
