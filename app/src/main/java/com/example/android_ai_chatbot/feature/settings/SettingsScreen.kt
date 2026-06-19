package com.example.android_ai_chatbot.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.android_ai_chatbot.domian.model.AiModel
import com.example.android_ai_chatbot.domian.model.AvailableModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }  // ← add

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Appearance ───────────────────────────────────────────────────
            item { SettingsSectionHeader("Appearance") }

            item {
                ListItem(
                    headlineContent = { Text("Dark mode") },
                    supportingContent = { Text("Switch between light and dark theme") },
                    leadingContent = {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── AI Model ─────────────────────────────────────────────────────
            item { SettingsSectionHeader("AI Model") }

            item {
                val currentModel = AvailableModels.findById(selectedModelId)
                ListItem(
                    headlineContent = { Text("Model") },
                    supportingContent = {
                        Column {
                            Text(currentModel.displayName)
                            Text(
                                currentModel.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (currentModel.supportsImages) {
                                Text(
                                    "✓ Supports images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showModelDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Data ─────────────────────────────────────────────────────────
            item { SettingsSectionHeader("Data") }

            item {
                ListItem(
                    headlineContent = { Text("Clear all history") },
                    supportingContent = { Text("Delete all conversations and messages") },
                    leadingContent = {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { showClearHistoryDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            item {
                ListItem(
                    headlineContent = { Text("Sign out") },
                    supportingContent = { Text(viewModel.userEmail) },
                    leadingContent = {
                        Icon(Icons.Default.Logout, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        viewModel.signOut()
                        onNavigateToLogin()
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── About ────────────────────────────────────────────────────────
            item { SettingsSectionHeader("About") }

            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Powered by") },
                    supportingContent = { Text("Groq API") },
                    leadingContent = { Icon(Icons.Default.AutoAwesome, null) }
                )
            }
        }
    }

    // ── Model selector dialog ─────────────────────────────────────────────────
    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Select AI Model") },
            text = {
                LazyColumn {
                    items(AvailableModels.models) { model ->
                        ModelItem(
                            model = model,
                            isSelected = model.id == selectedModelId,
                            onSelect = {
                                viewModel.selectModel(model.id)
                                showModelDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Clear history dialog ──────────────────────────────────────────────────
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear all history?") },
            text = {
                Text("This will permanently delete all conversations and messages. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete all") }
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
private fun ModelItem(
    model: AiModel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(model.displayName) },
        supportingContent = {
            Column {
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (model.supportsImages) {
                    Text(
                        "✓ Supports images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "✗ Text only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = onSelect)
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

