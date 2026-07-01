package com.llamadroid.ui.screens.rag

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.llamadroid.data.models.RagDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagScreen(viewModel: RagViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importDocument(it) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Document Library") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePicker.launch(arrayOf("text/plain", "text/markdown")) }) {
                if (state.isImporting) CircularProgressIndicator()
                else Icon(Icons.Filled.Add, "Import document")
            }
        }
    ) { padding ->
        if (state.documents.isEmpty() && !state.isImporting) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Description, null, modifier = Modifier.padding(bottom = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No documents imported", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to add TXT, MD, or PDF files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(state.documents, key = { it.id }) { doc ->
                    RagDocumentCard(doc, onDelete = { viewModel.deleteDocument(doc) })
                }
            }
        }
    }
}

@Composable
private fun RagDocumentCard(doc: RagDocument, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Description, null, modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.filename, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${doc.chunkCount} chunks · ${doc.fileType.uppercase()} · ${formatSize(doc.fileSize)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000f)
    bytes > 1_000 -> String.format("%.0f KB", bytes / 1_000f)
    else -> "$bytes B"
}
