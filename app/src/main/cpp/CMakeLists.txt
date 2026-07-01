cmake_minimum_required(VERSION 3.22)
project(llamadroid LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# llama.cpp submodule paths
set(LLAMACPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
set(GGML_DIR "${LLAMACPP_DIR}/ggml")

# GPU backends — all optional, detected at build time
option(LLAMADROID_VULKAN "Build with Vulkan support" ON)
option(LLAMADROID_OPENCL "Build with OpenCL support" OFF)
option(LLAMADROID_CUDA "Build with CUDA support" OFF)

# llama.cpp library
add_subdirectory(${LLAMACPP_DIR} llama.cpp EXCLUDE_FROM_ALL)

# Our JNI bridge library
add_library(llamadroid SHARED
    jni_bridge.cpp
    inference.cpp
    sampling.cpp
    embeddings.cpp
    model_info.cpp
)

target_compile_options(llamadroid PRIVATE
    -Wall -Wextra -Wno-unused-parameter -Wno-sign-compare
    -O2 -funroll-loops -fomit-frame-pointer
)

target_link_libraries(llamadroid PRIVATE
    llama
    ggml
    ${log_lib}
    android
)

target_include_directories(llamadroid PRIVATE
    ${LLAMACPP_DIR}
    ${GGML_DIR}
    ${LLAMACPP_DIR}/common
)

# GPU backend linking
if(LLAMADROID_VULKAN)
    target_compile_definitions(llamadroid PRIVATE GGML_VULKAN=1)
    target_link_libraries(llamadroid PRIVATE ggml_vulkan)
endif()

if(LLAMADROID_OPENCL)
    target_compile_definitions(llamadroid PRIVATE GGML_OPENCL=1)
    target_link_libraries(llamadroid PRIVATE ggml_opencl)
endif()

# NEON is always enabled for arm64
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(llamadroid PRIVATE GGML_NEON=1)
endif()

# Android log library
find_library(log_lib log)
