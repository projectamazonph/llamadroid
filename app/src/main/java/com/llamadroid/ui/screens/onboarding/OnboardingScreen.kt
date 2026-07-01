package com.llamadroid.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    if (state.isComplete) { onComplete(); return }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        when (state.currentPage) {
            0 -> WelcomePage()
            1 -> DeviceInfoPage(state.deviceTier)
            2 -> RecommendationsPage()
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(); Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { viewModel.skip() }) { Text("Skip") }
            Button(onClick = { viewModel.nextPage() }) { Text(if (state.currentPage < state.pageCount - 1) "Next" else "Get Started") }
        }
    }
}

@Composable
private fun WelcomePage() { Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Welcome to LlamaDroid", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text("Run powerful AI models directly on your device. Everything is private and works offline.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("No data leaves your phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
}}

@Composable
private fun DeviceInfoPage(tier: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Your Device", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text("Detected tier: $tier", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("We'll recommend models that run well on your device.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}}

@Composable
private fun RecommendationsPage() { Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Recommended Models", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text("Here are models that work well on your device. You can download them from the Hub tab.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("Or skip and choose your own later.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}}
