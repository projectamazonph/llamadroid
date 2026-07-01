package com.llamadroid.ui.screens.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.llamadroid.domain.server.ServerMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(viewModel: ServerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(topBar = { TopAppBar(title = { Text("Local API Server") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(state.isRunning, state.metrics)
            HorizontalDivider()
            Text("Configuration", style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.port.toString(), onValueChange = { it.toIntOrNull()?.let { viewModel.setPort(it) } }, label = { Text("Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.isRunning, modifier = Modifier.width(160.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text("Public access", style = MaterialTheme.typography.bodyMedium); Text("Allow connections from other devices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Switch(checked = state.publicAccess, onCheckedChange = { viewModel.setPublicAccess(it) }, enabled = !state.isRunning)
                    }
                }
            }
            if (state.isRunning) {
                HorizontalDivider()
                Text("Connection", style = MaterialTheme.typography.titleSmall)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val host = if (state.publicAccess) state.localIp else "127.0.0.1"
                        val baseUrl = "http://$host:${state.port}"
                        Text(baseUrl, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text("OpenAI-compatible API", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("/v1/chat/completions", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            IconButton(onClick = { clipboard.setText(AnnotatedString("$baseUrl/v1/chat/completions")) }, modifier = Modifier.width(24.dp).height(24.dp)) { Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.padding(0.dp)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("/v1/models", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            IconButton(onClick = { clipboard.setText(AnnotatedString("$baseUrl/v1/models")) }, modifier = Modifier.width(24.dp).height(24.dp)) { Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.padding(0.dp)) }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { viewModel.toggle() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(if (state.isRunning) "Stop Server" else "Start Server", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable private fun StatusCard(running: Boolean, metrics: ServerMetrics) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (running) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (running) "Running" else "Stopped", style = MaterialTheme.typography.titleLarge, color = if (running) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Text(if (running) "●" else "○", style = MaterialTheme.typography.titleLarge, color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (running) { Spacer(Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Requests", "${metrics.requestsServed}"); StatItem("Tokens", "${metrics.tokensGenerated}")
                StatItem("Uptime", "${metrics.uptimeSeconds / 60}m ${metrics.uptimeSeconds % 60}s")
            }}
        }
    }
}

@Composable private fun StatItem(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
}}
