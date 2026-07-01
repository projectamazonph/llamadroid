package com.llamadroid.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.llamadroid.data.models.Message
import com.llamadroid.domain.chat.ChatViewModel
import com.llamadroid.domain.media.SpeechInputContract
import com.llamadroid.ui.components.ParameterPanel
import com.llamadroid.ui.screens.settings.SystemPromptViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(conversationId: String? = null, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showPromptPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.attachImage(it) }
    }

    val speechLauncher = rememberLauncherForActivityResult(SpeechInputContract()) { text ->
        text?.let { viewModel.setInput(it) }
    }

    LaunchedEffect(conversationId) {
        if (conversationId != null) viewModel.selectConversation(conversationId)
        else if (state.currentConversation == null) viewModel.newConversation()
    }

    LaunchedEffect(state.messages.size, state.streamedText) {
        if (state.messages.isNotEmpty() || state.streamedText.isNotEmpty())
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.currentConversation?.title ?: "LlamaDroid", style = MaterialTheme.typography.titleMedium)
                        if (state.isGenerating) Text("${state.tokensPerSec} t/s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleRag() }) { Icon(Icons.Filled.MenuBook, "RAG", tint = if (state.ragEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { showPromptPicker = true }) { Icon(Icons.Filled.Psychology, "System Prompt") }
                    if (state.isGenerating) { IconButton(onClick = { viewModel.stopGeneration() }) { Icon(Icons.Filled.Stop, "Stop") } }
                    IconButton(onClick = { viewModel.toggleParams() }) { Icon(Icons.Filled.Settings, "Parameters") }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.Menu, "Menu") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Export") }, onClick = { viewModel.exportConversation(); showMenu = false }, leadingIcon = { Icon(Icons.Filled.Share, null) })
                            DropdownMenuItem(text = { Text("Clear") }, onClick = { viewModel.clearConversation(); showMenu = false }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = state.showParams) { ParameterPanel(params = state.params, onParamsChanged = { viewModel.updateParams(it) }) }
                if (state.isGenerating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                // Pending image preview
                state.pendingImageThumb?.let { thumb ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            AsyncImage(model = File(thumb), contentDescription = "Attached image", modifier = Modifier.size(48.dp), contentScale = ContentScale.Crop)
                            Text("Image attached", modifier = Modifier.padding(horizontal = 8.dp).weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { viewModel.removePendingImage() }) { Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }

                InputRow(
                    text = state.inputText,
                    enabled = !state.isGenerating,
                    hasImage = state.pendingImage != null,
                    onTextChanged = { viewModel.setInput(it) },
                    onSend = { viewModel.send() },
                    onImagePick = { imagePicker.launch("image/*") },
                    onSpeech = { speechLauncher.launch(Unit) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty() && !state.isGenerating) {
                EmptyHint(state.currentSystemPrompt, state.ragEnabled)
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)) {
                    items(state.messages, key = { it.id }) { MessageBubble(it, viewModel) }
                    if (state.streamedText.isNotEmpty()) { item(key = "streaming") { StreamingBubble(state.streamedText) } }
                    if (state.error != null) { item(key = "error") { ErrorBanner(state.error!!) } }
                }
            }
        }
    }

    if (showPromptPicker) {
        SystemPromptPickerDialog(state.currentSystemPrompt, onSelect = { viewModel.setSystemPrompt(it); showPromptPicker = false }, onDismiss = { showPromptPicker = false })
    }
}

// ── Components ──

@Composable
private fun InputRow(text: String, enabled: Boolean, hasImage: Boolean, onTextChanged: (String) -> Unit, onSend: () -> Unit, onImagePick: () -> Unit, onSpeech: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
        IconButton(onClick = onImagePick, enabled = enabled) { Icon(Icons.Filled.Image, "Attach image") }
        OutlinedTextField(
            value = text, onValueChange = onTextChanged, modifier = Modifier.weight(1f),
            placeholder = { Text("Message...") }, enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (enabled && (text.isNotBlank() || hasImage)) onSend() }),
            maxLines = 4, colors = OutlinedTextFieldDefaults.colors()
        )
        if (text.isBlank()) {
            IconButton(onClick = onSpeech, enabled = enabled) { Icon(Icons.Filled.Mic, "Voice input") }
        }
        IconButton(onClick = onSend, enabled = enabled && (text.isNotBlank() || hasImage)) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
    }
}

@Composable
private fun MessageBubble(msg: Message, viewModel: ChatViewModel) {
    val isUser = msg.role == "user"
    val shape = if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Card(modifier = Modifier.widthIn(max = 320.dp), shape = shape, colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(modifier = Modifier.padding(12.dp)) {
                msg.imagePath?.let { path ->
                    if (File(path).exists()) {
                        AsyncImage(model = File(path), contentDescription = "Image", modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(4.dp))
                    }
                }
                if (msg.content.isNotBlank()) {
                    if (isUser) Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                    else MarkdownText(msg.content)
                }
                if (!isUser && msg.content.isNotBlank()) {
                    IconButton(onClick = { viewModel.speakResponse(msg.content) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.VolumeUp, "Read aloud", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${msg.tokens} tok", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(modifier = Modifier.padding(12.dp)) {
                MarkdownText(text)
                CircularProgressIndicator(modifier = Modifier.size(12.dp).padding(top = 4.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyHint(systemPrompt: String, ragEnabled: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LlamaDroid", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Load a model and start chatting", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (systemPrompt.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("System prompt active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            if (ragEnabled) { Spacer(Modifier.height(2.dp)); Text("RAG enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
        }
    }
}

@Composable
private fun SystemPromptPickerDialog(currentPrompt: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val promptViewModel: SystemPromptViewModel = hiltViewModel()
    val prompts by promptViewModel.prompts.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("System Prompt") }, text = {
        Column {
            Text("Current: ${currentPrompt.take(80)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            prompts.forEach { prompt ->
                TextButton(onClick = { onSelect(prompt.content) }, modifier = Modifier.fillMaxWidth()) { Text("${prompt.name}: ${prompt.content.take(60)}...", maxLines = 1) }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun MarkdownText(text: String) {
    val parts = text.split("```")
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) { part.split("\n").filter { it.isNotBlank() }.forEach { line -> Text(line, style = MaterialTheme.typography.bodyLarge) } }
            else {
                val code = part.lines().drop(1).joinToString("\n")
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Text(code.trimEnd('\n'), modifier = Modifier.padding(8.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
