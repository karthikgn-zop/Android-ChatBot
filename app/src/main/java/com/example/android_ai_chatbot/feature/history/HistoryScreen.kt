package com.example.android_ai_chatbot.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_ai_chatbot.domian.model.Conversation
import com.example.android_ai_chatbot.domian.repository.ConversationRepository
import com.example.android_ai_chatbot.domian.usecase.CreateConversationUseCase
import com.example.android_ai_chatbot.domian.usecase.DeleteConversationUseCase
import com.example.android_ai_chatbot.domian.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase,
    private val createConversation: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<Conversation>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) getConversations()
            else conversationRepo.searchConversations(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun newConversation(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val conv = createConversation("New Chat")
            onCreated(conv.id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch { deleteConversationUseCase(id) }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch { conversationRepo.renameConversation(id, newTitle) }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenConversation: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        topBar = {
            if (showSearch) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search conversations...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        viewModel.onSearchQueryChanged("")
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Conversations") },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.newConversation { id ->
                        onOpenConversation(id, "New chat")
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "New chat") },
                text = { Text("New chat") }
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isBlank())
                            Icons.Default.ChatBubbleOutline
                        else
                            Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank())
                            "No conversations yet"
                        else
                            "No results for \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (searchQuery.isBlank()) {
                        Text(
                            "Tap + to start one",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        searchQuery = searchQuery,
                        onClick = { onOpenConversation(conversation.id, conversation.title) },
                        onRename = { showRenameDialog = conversation },
                        onDelete = { viewModel.deleteConversation(conversation.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }

    showRenameDialog?.let { conv ->
        var newTitle by remember { mutableStateOf(conv.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename conversation") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameConversation(conv.id, newTitle.trim())
                    showRenameDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    searchQuery: String,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            if (searchQuery.isBlank()) {
                Text(
                    conversation.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                HighlightedText(
                    text = conversation.title,
                    searchQuery = searchQuery
                )
            }
        },
        supportingContent = {
            Text(
                conversation.updatedAt.toRelativeTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        },
        leadingContent = {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { expanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { expanded = false; onDelete() }
                    )
                }
            }
        }
    )
}

@Composable
private fun HighlightedText(
    text: String,
    searchQuery: String
) {
    val startIndex = text.lowercase().indexOf(searchQuery.lowercase())
    if (startIndex == -1) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val endIndex = startIndex + searchQuery.length
    val annotated = buildAnnotatedString {
        append(text.substring(0, startIndex))
        withStyle(
            SpanStyle(
                background = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(text.substring(startIndex, endIndex))
        }
        append(text.substring(endIndex))
    }
    Text(
        text = annotated,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
    }
}