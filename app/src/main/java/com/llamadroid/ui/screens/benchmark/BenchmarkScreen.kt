package com.llamadroid.ui.screens.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.llamadroid.data.models.DownloadedModel
import com.llamadroid.domain.inference.BenchmarkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(viewModel: BenchmarkViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Benchmark") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Measure your model's inference performance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { /* will run benchmark on selected model */ },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isRunning) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Run Benchmark")
                }
                OutlinedButton(onClick = { viewModel.clearResults() }) {
                    Text("Clear")
                }
            }

            if (state.results.isNotEmpty()) {
                HorizontalDivider()
                Text("Results", style = MaterialTheme.typography.titleSmall)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.results.reversed(), key = { it.durationMs }) { result ->
                        BenchmarkResultCard(result)
                    }
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BenchmarkResultCard(result: BenchmarkResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.modelName.ifBlank { "Model" }, style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Prompt t/s", String.format("%.1f", result.promptTokensPerSec))
                StatItem("Generation t/s", String.format("%.1f", result.genTokensPerSec))
                StatItem("Duration", "${result.durationMs / 1000}.${(result.durationMs % 1000) / 100}s")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Context: ${result.contextSize}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Memory: ${result.peakMemoryMb} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
