package com.llamadroid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.llamadroid.domain.inference.SamplingParams

@Composable
fun ParameterPanel(params: SamplingParams, onParamsChanged: (SamplingParams) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().height(360.dp).padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
            Text("Performance Profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { onParamsChanged(SamplingParams(temperature = 0.8f, maxTokens = 4096)) }, contentPadding = ButtonDefaults.TextButtonContentPadding) { Text("Creative", style = MaterialTheme.typography.labelSmall) }
                Button(modifier = Modifier.weight(1f), onClick = { onParamsChanged(SamplingParams(temperature = 0.3f, topK = 20, repeatPenalty = 1.2f)) }, contentPadding = ButtonDefaults.TextButtonContentPadding) { Text("Precise", style = MaterialTheme.typography.labelSmall) }
                Button(modifier = Modifier.weight(1f), onClick = { onParamsChanged(SamplingParams(temperature = 0.1f, topK = 10, maxTokens = 512)) }, contentPadding = ButtonDefaults.TextButtonContentPadding) { Text("Fast", style = MaterialTheme.typography.labelSmall) }
            }
            Spacer(Modifier.height(4.dp))
            SliderField("Temperature", params.temperature, 0f..2f) { onParamsChanged(params.copy(temperature = it)) }
            SliderField("Top-P", params.topP, 0f..1f) { onParamsChanged(params.copy(topP = it)) }
            SliderField("Top-K", params.topK.toFloat(), 0f..100f) { onParamsChanged(params.copy(topK = it.toInt())) }
            SliderField("Repeat Penalty", params.repeatPenalty, 1f..2f) { onParamsChanged(params.copy(repeatPenalty = it)) }
            SliderField("Min-P", params.minP, 0f..1f) { onParamsChanged(params.copy(minP = it)) }
            MaxTokensField(params.maxTokens) { onParamsChanged(params.copy(maxTokens = it)) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Speculative Decoding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = params.speculativeDecodingEnabled, onCheckedChange = { if (!it) onParamsChanged(params.copy(draftModelPath = "")) })
            }
            if (params.speculativeDecodingEnabled) {
                SliderField("Lookahead", params.speculativeLookahead.toFloat(), 8f..64f) { onParamsChanged(params.copy(speculativeLookahead = it.toInt())) }
            }
        }
    }
}

@Composable private fun SliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChanged: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChanged, valueRange = range, modifier = Modifier.fillMaxWidth())
    }
}

@Composable private fun MaxTokensField(value: Int, onChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Max Tokens", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(value = value.toString(), onValueChange = { it.toIntOrNull()?.let(onChanged) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(120.dp).padding(top = 4.dp))
    }
}
