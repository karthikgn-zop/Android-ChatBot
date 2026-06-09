package com.example.android_ai_chatbot.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Section: Appearance ──────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "Appearance")
            }

            item {
                ListItem(
                    headlineContent = { Text("Clear all history") },
                    supportingContent = { Text("Delete all conversations and messages") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable(
                        onClick = { showClearHistoryDialog = true }
                    )
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Section: Data ────────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "Data")
            }

            item {
                ListItem(
                    headlineContent   = { Text("Clear all history") },
                    supportingContent = { Text("Delete all conversations and messages") },
                    leadingContent    = {
                        Icon(
                            imageVector        = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { showClearHistoryDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Section: About ───────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                ListItem(
                    headlineContent   = { Text("Version") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent    = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
            }

            item {
                ListItem(
                    headlineContent   = { Text("Powered by") },
                    supportingContent = { Text("OpenAI") },
                    leadingContent    = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    }
                )
            }
        }
    }

    // ── Clear history confirmation dialog ────────────────────────────────────
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon             = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear all history?") },
            text  = {
                Text("This will permanently delete all conversations and messages. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: wire up DeleteAllConversationsUseCase here
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

