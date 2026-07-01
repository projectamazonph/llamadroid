package com.llamadroid.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.models.DownloadedModel
import com.llamadroid.data.repository.ModelDao
import com.llamadroid.domain.inference.InferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ModelLibraryViewModel @Inject constructor(
    private val modelDao: ModelDao,
    private val inferenceEngine: InferenceEngine
) : ViewModel() {

    private val _models = MutableStateFlow<List<DownloadedModel>>(emptyList())
    val models: StateFlow<List<DownloadedModel>> = _models.asStateFlow()

    init {
        viewModelScope.launch {
            modelDao.all().collect { _models.value = it }
        }
    }

    fun deleteModel(model: DownloadedModel) {
        viewModelScope.launch {
            File(model.localPath).delete()
            modelDao.delete(model.id)
        }
    }

    fun getFileSize(path: String): String {
        val file = File(path)
        if (!file.exists()) return "N/A"
        val bytes = file.length()
        return when {
            bytes > 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000f)
            bytes > 1_000_000 -> String.format("%.0f MB", bytes / 1_000_000f)
            else -> String.format("%.0f KB", bytes / 1_000f)
        }
    }
}
