package com.example.android_ai_chatbot.feature.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ModelDownloadScreen(
    onDownloadComplete: () -> Unit,
    viewModel: ModelDownloadViewModel= hiltViewModel()
){
    val progress by viewModel.progress.collectAsState()
    val isDownloaded by viewModel.isDownloaded.collectAsState()

    LaunchedEffect(isDownloaded) {
        if (isDownloaded){
            onDownloadComplete()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint= MaterialTheme.colorScheme.primary
            )
            Text(
                "Downloading AI Model",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Gemma 2B (~1.5GB)\nThis is a one-time download",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            LinearProgressIndicator(
                progress={progress/100f},
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "$progress%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (progress==0){
                Button(onClick = {viewModel.startDownload()}) {
                    Text("Download Model")
                }
            }
        }
    }
}