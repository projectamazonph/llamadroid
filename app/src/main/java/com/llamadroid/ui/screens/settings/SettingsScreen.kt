package com.llamadroid.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onServerClick: () -> Unit = {}, onBenchmarkClick: () -> Unit = {}, onPromptsClick: () -> Unit = {}, onRagClick: () -> Unit = {}) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Section("General") {
                SettingsToggle("Dark Theme", "Follow system", true) {}
                SettingsToggle("Dynamic Colors", "Material You theming", true) {}
            }
            Spacer(Modifier.height(16.dp))
            Section("Features") {
                SettingsNav("Local API Server", "OpenAI-compatible HTTP server", Icons.Filled.Dns, onServerClick)
                SettingsNav("Document Library (RAG)", "Import docs and search in chat", Icons.Filled.Description, onRagClick)
                SettingsNav("Benchmark", "Measure inference performance", Icons.Filled.Memory, onBenchmarkClick)
                SettingsNav("System Prompts", "Manage prompt presets", Icons.Filled.Psychology, onPromptsClick)
            }
            Spacer(Modifier.height(16.dp))
            Section("Downloads") { SettingsToggle("Wi-Fi only", "Only download on Wi-Fi", false) {} }
            Spacer(Modifier.height(16.dp))
            Section("About") {
                Text("LlamaDroid v0.1.0", style = MaterialTheme.typography.bodyMedium)
                Text("On-device LLM inference for Android", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(4.dp)) { content() }
    }
}

@Composable private fun SettingsToggle(label: String, description: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable private fun SettingsNav(label: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
