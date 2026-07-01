package com.llamadroid.ui.screens.hub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.huggingface.HfApi
import com.llamadroid.data.huggingface.HfModel
import com.llamadroid.domain.models.DownloadManager
import com.llamadroid.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HubUiState(
    val models: List<HfModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val downloading: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class HubViewModel @Inject constructor(
    private val hfApi: HfApi,
    private val downloadManager: DownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HubUiState())
    val state: StateFlow<HubUiState> = _state.asStateFlow()

    init {
        loadPopular()
    }

    fun loadPopular() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val models = hfApi.listModels()
                _state.value = _state.value.copy(models = models, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) { loadPopular(); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val models = hfApi.search(query)
                _state.value = _state.value.copy(models = models, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun downloadModel(model: HfModel, ggufFile: String) {
        _state.value = _state.value.copy(
            downloading = _state.value.downloading + model.id
        )
        val intent = DownloadService.intent(context, model.id, ggufFile)
        context.startForegroundService(intent)
        // Poll for completion
        viewModelScope.launch {
            while (true) {
                val progress = downloadManager.getProgress()
                if (progress == null) { kotlinx.coroutines.delay(500); continue }
                if (progress.isComplete || progress.error != null) {
                    _state.value = _state.value.copy(
                        downloading = _state.value.downloading - model.id
                    )
                    break
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}
