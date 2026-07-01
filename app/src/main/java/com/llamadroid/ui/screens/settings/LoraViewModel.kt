package com.llamadroid.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.native.NativeLib
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class LoraAdapter(val path: String, val name: String, val scale: Float = 1.0f)

data class LoraUiState(
    val loadedAdapters: List<LoraAdapter> = emptyList(),
    val isImporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoraViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(LoraUiState())
    val state: StateFlow<LoraUiState> = _state.asStateFlow()
    private var currentModelHandle: Long = 0

    fun setModelHandle(handle: Long) { currentModelHandle = handle }

    fun importAdapter(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null)
            try {
                val dir = File(context.cacheDir, "lora").also { it.mkdirs() }
                val file = File(dir, "adapter_${System.currentTimeMillis()}.gguf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                _state.value = _state.value.copy(
                    loadedAdapters = _state.value.loadedAdapters + LoraAdapter(file.absolutePath, file.name)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally { _state.value = _state.value.copy(isImporting = false) }
        }
    }

    fun loadAdapter(adapter: LoraAdapter) {
        viewModelScope.launch {
            if (currentModelHandle == 0L) return@launch
            NativeLib.ensureLoaded()
            NativeLib.loadLora(currentModelHandle, adapter.path, adapter.scale)
            _state.value = _state.value.copy(
                loadedAdapters = _state.value.loadedAdapters.map { it.copy(scale = adapter.scale) }
            )
        }
    }

    fun unloadAdapter(adapter: LoraAdapter) {
        viewModelScope.launch {
            if (currentModelHandle == 0L) return@launch
            NativeLib.ensureLoaded()
            NativeLib.unloadLora(currentModelHandle)
            _state.value = _state.value.copy(
                loadedAdapters = _state.value.loadedAdapters.filter { it.path != adapter.path }
            )
        }
    }

    fun removeAdapter(adapter: LoraAdapter) { _state.value = _state.value.copy(loadedAdapters = _state.value.loadedAdapters - adapter) }
}
