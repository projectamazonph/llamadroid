package com.llamadroid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.models.SystemPrompt
import com.llamadroid.data.repository.SystemPromptDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SystemPromptViewModel @Inject constructor(
    private val dao: SystemPromptDao
) : ViewModel() {

    private val _prompts = MutableStateFlow<List<SystemPrompt>>(emptyList())
    val prompts: StateFlow<List<SystemPrompt>> = _prompts.asStateFlow()

    init {
        seedBuiltins()
        viewModelScope.launch { dao.all().collect { _prompts.value = it } }
    }

    fun save(name: String, content: String) {
        viewModelScope.launch {
            dao.upsert(SystemPrompt(id = UUID.randomUUID().toString(), name = name, content = content, isBuiltin = false))
        }
    }

    fun delete(prompt: SystemPrompt) { viewModelScope.launch { dao.delete(prompt.id) } }

    private fun seedBuiltins() {
        viewModelScope.launch {
            builtins.forEach { dao.upsert(it) }
        }
    }

    companion object {
        val builtins = listOf(
            SystemPrompt("builtin_default", "Default", "You are a helpful assistant.", true),
            SystemPrompt("builtin_creative", "Creative", "You are a creative assistant. Be imaginative, vivid, and original in your responses.", true),
            SystemPrompt("builtin_precise", "Precise", "You are a precise assistant. Give concise, accurate answers. When unsure, state your uncertainty.", true),
            SystemPrompt("builtin_code", "Code Assistant", "You are an expert programmer. Provide clean, well-documented code. Explain your reasoning.", true),
            SystemPrompt("builtin_translator", "Translator", "You are a translator. Translate the user's input accurately. Preserve tone and context.", true),
            SystemPrompt("builtin_summarizer", "Summarizer", "You are a summarizer. Condense the provided text while preserving key information.", true),
        )
    }
}
