package com.llamadroid.ui.screens.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.repository.ModelDao
import com.llamadroid.domain.inference.BenchmarkResult
import com.llamadroid.domain.inference.ContextConfig
import com.llamadroid.domain.inference.InferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class BenchmarkUiState(
    val selectedModelPath: String = "",
    val selectedModelName: String = "",
    val isRunning: Boolean = false,
    val results: List<BenchmarkResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelDao: ModelDao
) : ViewModel() {
    private val _state = MutableStateFlow(BenchmarkUiState())
    val state: StateFlow<BenchmarkUiState> = _state.asStateFlow()

    fun runBenchmark(modelPath: String, modelName: String) {
        _state.value = _state.value.copy(isRunning = true, error = null)
        viewModelScope.launch {
            val config = ContextConfig()
            val result = inferenceEngine.runBenchmark(modelPath, config)
            val br = Json.decodeFromString<BenchmarkResult>(result).copy(modelName = modelName)
            _state.value = _state.value.copy(isRunning = false, results = _state.value.results + br, error = br.error)
        }
    }

    fun clearResults() { _state.value = _state.value.copy(results = emptyList()) }
}
