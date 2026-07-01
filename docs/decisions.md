# Decisions — LlamaDroid

## Decision 1: llama.cpp for On-Device Inference

**Context:** Need efficient, optimized C++ inference engine that runs on Android ARM devices with limited memory.

**Decision:** llama.cpp via JNI bridge — the most optimized open-source LLM inference engine for consumer hardware.

**Consequences:**
- ✅ Best performance on mobile ARM chips
- ✅ Supports all major model architectures (LLaMA, Mistral, Gemma, etc.)
- ✅ GGUF format for quantized models (2-bit to 8-bit)
- ⚠️ JNI bridge adds complexity to build system

## Decision 2: GGUF Model Format

**Context:** Models need to be quantized for mobile memory constraints (4-8GB RAM typical).

**Decision:** GGUF format with K-quant quantization — supports 2-8 bit quantization levels for size/quality tradeoff.

**Consequences:**
- ✅ Models as small as 2-4GB for 7B parameters
- ✅ Wide model availability on HuggingFace
- ✅ Built-in support in llama.cpp
- ⚠️ Quantization reduces output quality vs full precision
