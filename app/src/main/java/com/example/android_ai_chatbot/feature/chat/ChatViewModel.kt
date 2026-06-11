package com.example.android_ai_chatbot.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_ai_chatbot.data.remote.ImageContentPart
import com.example.android_ai_chatbot.data.remote.ImageUrl
import com.example.android_ai_chatbot.data.remote.TextContentPart
import com.example.android_ai_chatbot.domian.model.ChatState
import com.example.android_ai_chatbot.domian.model.Message
import com.example.android_ai_chatbot.domian.model.MessageRole
import com.example.android_ai_chatbot.domian.repository.ChatRepository
import com.example.android_ai_chatbot.domian.repository.ConversationRepository
import com.example.android_ai_chatbot.domian.usecase.GetMessagesUseCase
import com.example.android_ai_chatbot.domian.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val chatState: ChatState = ChatState.Idle,
    val inputText: String = "",
    val conversationTitle: String = "New chat",
    val attachedImageUri: android.net.Uri? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val conversationRepository: ConversationRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { list ->
                val title = list.find { it.id == conversationId }?.title ?: "New Chat"
                _uiState.update { it.copy(conversationTitle = title) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(context: android.content.Context) {
        val prompt = _uiState.value.inputText.trim()
        val imageUri = _uiState.value.attachedImageUri
        if (prompt.isBlank() && imageUri == null) return
        if (_uiState.value.chatState == ChatState.Streaming) return

        val isFirstMessage = _uiState.value.messages.none { it.role == MessageRole.USER }
        _uiState.update {
            it.copy(
                inputText = "",
                attachedImageUri = null,
                chatState = ChatState.Streaming
            )
        }

        streamingJob = viewModelScope.launch {
            val accumulatedContent = StringBuilder()

            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                content = prompt.ifBlank { "Image attached" },
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis(),
                isStreaming = false,
                imageUri = imageUri?.toString()
            )
            chatRepository.saveMessage(userMessage)
            conversationRepository.updateTimestamp(conversationId, System.currentTimeMillis())

            val aiMessageId = UUID.randomUUID().toString()
            val aiPlaceholder = Message(
                id = aiMessageId,
                conversationId = conversationId,
                content = "",
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis() + 1,
                isStreaming = true
            )

            runCatching {
                val messageContent: Any = if (imageUri != null) {
                    val base64 = imageUri.toBase64(context)
                    val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    listOf(
                        TextContentPart(text = prompt.ifBlank { "What's in this image?" }),
                        ImageContentPart(imageUrl = ImageUrl("data:$mimeType;base64,$base64"))
                    )
                } else {
                    prompt
                }


                chatRepository.sendMessageStream(
                    conversationId = conversationId,
                    prompt = prompt.ifBlank { "Image attached" },
                    history = _uiState.value.messages,
                    messageContent = messageContent
                ).collect { token ->
                    if (accumulatedContent.isEmpty()) {
                        chatRepository.saveMessage(aiPlaceholder)
                    }
                    accumulatedContent.append(token)
                    chatRepository.updateMessageContent(aiMessageId, accumulatedContent.toString())
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(chatState = ChatState.Error(error.message ?: "Unknown error"))
                }
                chatRepository.updateMessageContent(aiMessageId, "Error: ${error.message}")
            }

            if (isFirstMessage) {
                runCatching {
                    val title =
                        chatRepository.generateTitle(prompt.ifBlank { "Image conversation" })
                    conversationRepository.renameConversation(conversationId, title)
                }
            }

            _uiState.update { it.copy(chatState = ChatState.Idle) }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(chatState = ChatState.Idle) }
    }

    fun setVoiceInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearError() {
        _uiState.update { it.copy(chatState = ChatState.Idle) }
    }

    fun setAttachedImage(uri: android.net.Uri) {
        _uiState.update { it.copy(attachedImageUri = uri) }
    }

    fun clearAttachment() {
        _uiState.update { it.copy(attachedImageUri = null) }
    }

    private fun android.net.Uri.toBase64(context: android.content.Context): String {
        val bytes = context.contentResolver.openInputStream(this)?.readBytes() ?: return ""
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

}