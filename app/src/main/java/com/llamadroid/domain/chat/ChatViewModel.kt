package com.llamadroid.domain.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.data.models.Conversation
import com.llamadroid.data.models.Message
import com.llamadroid.data.repository.ConversationDao
import com.llamadroid.data.repository.MessageDao
import com.llamadroid.data.repository.SystemPromptDao
import com.llamadroid.domain.inference.InferenceEngine
import com.llamadroid.domain.inference.SamplingParams
import com.llamadroid.domain.media.ImageAttachment
import com.llamadroid.domain.media.TtsManager
import com.llamadroid.domain.rag.RagEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val streamedText: String = "",
    val tokensPerSec: Float = 0f,
    val modelLoaded: Boolean = false,
    val modelPath: String = "",
    val params: SamplingParams = SamplingParams(),
    val showParams: Boolean = false,
    val currentSystemPrompt: String = "",
    val ragEnabled: Boolean = false,
    val pendingImage: String? = null,
    val pendingImageThumb: String? = null,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val systemPromptDao: SystemPromptDao,
    private val inferenceEngine: InferenceEngine,
    private val conversationIO: ConversationIO,
    private val ragEngine: RagEngine,
    private val imageAttachment: ImageAttachment,
    private val ttsManager: TtsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conversationDao.all().collect { convs ->
                _uiState.value = _uiState.value.copy(conversations = convs)
            }
        }
    }

    fun setInput(text: String) { _uiState.value = _uiState.value.copy(inputText = text) }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            val conv = conversationDao.get(id) ?: return@launch
            _uiState.value = _uiState.value.copy(currentConversation = conv, currentSystemPrompt = conv.systemPrompt)
            messageDao.byConversation(id).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun newConversation() {
        val id = UUID.randomUUID().toString()
        val conv = Conversation(
            id = id, title = "New Chat",
            modelId = _uiState.value.modelPath,
            systemPrompt = _uiState.value.currentSystemPrompt
        )
        viewModelScope.launch {
            conversationDao.upsert(conv)
            selectConversation(id)
        }
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            val processed = imageAttachment.process(context, uri)
            if (processed != null) {
                _uiState.value = _uiState.value.copy(
                    pendingImage = processed.filePath,
                    pendingImageThumb = processed.thumbnailPath
                )
            }
        }
    }

    fun removePendingImage() {
        _uiState.value.pendingImage?.let { File(it).delete() }
        _uiState.value.pendingImageThumb?.let { File(it).delete() }
        _uiState.value = _uiState.value.copy(pendingImage = null, pendingImageThumb = null)
    }

    fun send() {
        val text = _uiState.value.inputText.trim()
        val conv = _uiState.value.currentConversation ?: return
        if (text.isEmpty() && _uiState.value.pendingImage == null) return
        if (text.isEmpty() && _uiState.value.pendingImage != null) { /* send with image only */ }
        if (_uiState.value.isGenerating) return

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conv.id,
            role = "user",
            content = text,
            imagePath = _uiState.value.pendingImage
        )

        _uiState.value = _uiState.value.copy(
            inputText = "", isGenerating = true, streamedText = "",
            pendingImage = null, pendingImageThumb = null, error = null
        )

        viewModelScope.launch {
            messageDao.insert(userMsg)
            val prompt = buildPrompt(conv, text)
            try {
                val result = inferenceEngine.generate(
                    modelPath = conv.modelId, prompt = prompt, params = _uiState.value.params
                )
                messageDao.insert(Message(
                    id = UUID.randomUUID().toString(), conversationId = conv.id,
                    role = "assistant", content = result.text, tokens = result.tokens
                ))
                _uiState.value = _uiState.value.copy(isGenerating = false, streamedText = "", tokensPerSec = result.tokensPerSec)
                result.text.takeIf { it.isNotBlank() }?.let { ttsManager.speak(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message ?: "Generation failed")
            }
        }
    }

    fun speakResponse(text: String) { ttsManager.speak(text) }

    fun stopGeneration() { inferenceEngine.stop(_uiState.value.modelPath); _uiState.value = _uiState.value.copy(isGenerating = false) }

    fun clearConversation() {
        val conv = _uiState.value.currentConversation ?: return
        viewModelScope.launch { messageDao.deleteByConversation(conv.id); _uiState.value = _uiState.value.copy(messages = emptyList(), streamedText = "") }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.deleteById(id)
            if (_uiState.value.currentConversation?.id == id) _uiState.value = _uiState.value.copy(currentConversation = null, messages = emptyList())
        }
    }

    fun setSystemPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(currentSystemPrompt = prompt)
        _uiState.value.currentConversation?.let { conv ->
            viewModelScope.launch { conversationDao.upsert(conv.copy(systemPrompt = prompt)) }
        }
    }

    fun toggleRag() { _uiState.value = _uiState.value.copy(ragEnabled = !_uiState.value.ragEnabled) }

    fun importConversation(uri: android.net.Uri) {
        viewModelScope.launch {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().readText()
                conversationIO.import(text)
            }
        }
    }

    fun exportConversation() {
        val convId = _uiState.value.currentConversation?.id ?: return
        viewModelScope.launch { conversationIO.export(convId, context)?.let { conversationIO.share(it, context) } }
    }

    fun updateParams(params: SamplingParams) { _uiState.value = _uiState.value.copy(params = params) }
    fun toggleParams() { _uiState.value = _uiState.value.copy(showParams = !_uiState.value.showParams) }

    fun loadModel(path: String) {
        viewModelScope.launch {
            val ok = inferenceEngine.loadModel(path)
            _uiState.value = _uiState.value.copy(modelLoaded = ok, modelPath = path)
        }
    }

    private suspend fun buildPrompt(conv: Conversation, userText: String): String {
        var sysPrompt = conv.systemPrompt
        if (_uiState.value.ragEnabled) {
            sysPrompt = ragEngine.injectIntoPrompt(userText, sysPrompt.ifBlank { "You are a helpful assistant." })
        }
        // ponytail: template variable interpolation
        val now = java.time.LocalDateTime.now()
        sysPrompt = sysPrompt
            .replace("{{date}}", now.toLocalDate().toString())
            .replace("{{time}}", now.toLocalTime().toString().take(5))
            .replace("{{model}}", _uiState.value.modelPath.substringAfterLast('/').substringBeforeLast('.'))
        val imageTag = if (_uiState.value.pendingImage != null) "<image>\n" else ""
        val preamble = if (sysPrompt.isNotBlank()) "<|system>|\n$sysPrompt\n" else ""
        return "$preamble<|user|>|\n${imageTag}$userText\n<|assistant|>|"
    }

    override fun onCleared() { super.onCleared(); ttsManager.shutdown() }
}
