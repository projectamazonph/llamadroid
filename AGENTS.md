# AGENTS.md — LlamaDroid Standards & Guardrails

## Project Identity

**LlamaDroid** — on-device LLM inference for Android. Kotlin + Jetpack Compose UI, C++ (llama.cpp) via JNI. Everything runs locally, no network required after model download.

## Code Organization

```
app/src/main/java/com/llamadroid/
├── core/          DI, network, database, preferences, shared utils
├── data/          Models, repositories, JNI bridge classes, HF Hub API client
├── domain/        Business logic — inference, chat, server, RAG
├── ui/            Compose screens, components, navigation, theme
└── service/       Foreground services (inference, server, downloads)

app/src/main/cpp/
├── llama.cpp/     Git submodule (upstream)
├── jni_bridge.cpp JNI entry points
├── inference.cpp  Inference implementation
├── sampling.cpp   Sampling parameters
├── embeddings.cpp Embedding generation
└── server_adapter.cpp
```

## Language Conventions

### Kotlin
- **100% Kotlin**, no Java files.
- Use **Kotlinx Serialization** over Gson/Moshi.
- Use **Coroutines + Flow** for async. No RxJava.
- Use **Hilt** for DI, constructor injection preferred.
- Use **Coil** for image loading.
- Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
- Max line length: **120 chars**.

### C++
- **C++20**, no exceptions in JNI boundary.
- Use `std::` over hand-rolled containers.
- `snake_case` for functions/variables, `PascalCase` for classes.
- JNI methods follow the `Java_com_llamadroid_*` naming convention.
- Max line length: **100 chars**.

## JNI Bridge Rules
- All native methods are `private` in Kotlin, exposed via `internal` wrapper functions.
- Callbacks to Kotlin use `jmethodID` cached at registration — no `GetMethodID` per call.
- Never pass large data across JNI in individual calls — batch transfers.
- Context handles are `Long` values (opaque pointer), validated before each operation.
- Native C++ never throws into Java — all errors caught, logged, returned as error codes.

## Guardrails

### DO NOT
- **Commit model files** — GGUF files, LoRA adapters, test models. They go in `.gitignore` and LFS (for test fixtures).
- **Commit API keys or tokens** — all credentials via environment variables or `EncryptedSharedPreferences`.
- **Commit build artifacts** — `.so`, `.apk`, `.aab`, `.keystore`, `.hprof`.
- **Add new dependencies without this check:** can 50 lines of stdlib/stdlib Kotlin do it?
- **Use Java interop** — `java.*` imports allowed, but no new `.java` files.
- **Hardcode model paths, ports, or tokens** — all configurable via `DataStore` preferences.
- **Add analytics, telemetry, or crash reporting** — app is offline-first and zero-telemetry by design.

### DO
- **Async all native calls** — inference runs on `Dispatchers.Default`, never on Main.
- **Handle configuration changes** — ViewModel scoped, state survives rotation.
- **Check storage before downloads** — verify free space >= model file size * 1.1.
- **Validate user input** — prompt lengths, file paths, parameter ranges.
- **Foreground service** for any operation that runs >30s (inference, download, server).

## Performance Rules
- **Measure before optimizing.** Add benchmarks, profile, then optimize.
- **JNI is not free** — batch calls, minimize crossings.
- **Pre-allocate** native memory for context once, reuse across generations.
- **mmap model files** for loading speed and memory sharing.
- **Thread safety** — one inference context per model instance; use a request queue for multiple consumers.

## Testing
- **Test JNI native methods** via small C++ test binaries (not on Android).
- **Test Kotlin domain logic** with JVM unit tests (ViewModel, use cases).
- **Test UI** with Compose UI tests for critical flows (chat, settings).
- **One check per test** — smallest thing that fails if the logic breaks.

## Git Workflow
- `main` is stable, always buildable.
- Branch naming: `feature/`, `fix/`, `refactor/`, `chore/`.
- Commits: imperative mood, one logical change per commit.
- Squash before merging.

## Build
- `./gradlew assembleDebug` — debug APK for all ABIs.
- `./gradlew assembleRelease` — release APK (requires signing config).
- `./gradlew :app:externalNativeBuildDebug` — native libs only.
- NDK 27+, CMake 3.22+, SDK 35+, JDK 17+.
