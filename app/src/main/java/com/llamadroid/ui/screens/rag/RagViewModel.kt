package com.llamadroid.ui.screens.rag

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.models.RagDocument
import com.llamadroid.data.repository.RagDocumentDao
import com.llamadroid.domain.rag.RagEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class RagUiState(
    val documents: List<RagDocument> = emptyList(),
    val isImporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RagViewModel @Inject constructor(
    private val ragEngine: RagEngine,
    private val documentDao: RagDocumentDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(RagUiState())
    val state: StateFlow<RagUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            documentDao.all().collect { docs -> _state.value = _state.value.copy(documents = docs) }
        }
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null)
            try {
                val tempDir = File(context.cacheDir, "rag_imports")
                tempDir.mkdirs()
                val tempFile = File(tempDir, "import_${System.currentTimeMillis()}")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }

                val ok = ragEngine.importDocument(tempFile)
                tempFile.delete()
                if (!ok) _state.value = _state.value.copy(error = "Unsupported file type")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(isImporting = false)
            }
        }
    }

    fun deleteDocument(doc: RagDocument) {
        viewModelScope.launch { ragEngine.deleteDocument(doc.id) }
    }
}
