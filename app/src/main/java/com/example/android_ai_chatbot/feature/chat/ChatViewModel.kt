package com.example.android_ai_chatbot.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections.list
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val chatState: ChatState    = ChatState.Idle,
    val inputText: String       = "",
    val conversationTitle: String = "New chat"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val conversationRepository: ConversationRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel(){
    private val conversationId: String=checkNotNull(savedStateHandle["conversationId"])
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        loadMessages()
    }

    private fun loadMessages(){
        viewModelScope.launch {
            getMessagesUseCase(conversationId).collect { messages ->
                _uiState.update { it.copy(messages=messages) }
            }
        }
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { list ->
                val title = list.find { it.id == conversationId }?.title ?: "New Chat"
                _uiState.update { it.copy(conversationTitle = title) }
            }
        }
    }
    fun onInputChanged(text: String){
        _uiState.update{it.copy(inputText = text)}
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.chatState == ChatState.Streaming) return

        val isFirstMessage = _uiState.value.messages.none { it.role == MessageRole.USER }
        _uiState.update { it.copy(inputText = "", chatState = ChatState.Streaming) }

        streamingJob = viewModelScope.launch {

            // ← NO user message save here, SendMessageUseCase does it
            val accumulatedContent= StringBuilder()
            val aiMessageId = UUID.randomUUID().toString()
            val aiPlaceholder = Message(
                id             = aiMessageId,
                conversationId = conversationId,
                content        = "",
                role           = MessageRole.ASSISTANT,
                timestamp      = System.currentTimeMillis() + 1,
                isStreaming    = true
            )

            runCatching {
                // SendMessageUseCase saves user message first, then returns stream
                sendMessageUseCase(
                    conversationId = conversationId,
                    userText       = prompt,
                    history        = _uiState.value.messages
                ).collect { token ->
                    // Save AI placeholder only on first token so it appears after user message
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
                    val title = chatRepository.generateTitle(prompt)
                    conversationRepository.renameConversation(conversationId, title)
                }
            }

            _uiState.update { it.copy(chatState = ChatState.Idle) }
        }
    }
    fun stopStreaming(){
        streamingJob?.cancel()
        _uiState.update{it.copy(chatState = ChatState.Idle)}
    }
    fun setVoiceInput(text: String){
        _uiState.update{ it.copy(inputText = text)}
    }
    fun clearError(){
        _uiState.update { it.copy(chatState = ChatState.Idle) }
    }
}