package com.example.android_ai_chatbot.domian.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt:Long
)

data class Message(
    val id: String,
    val conversationId: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

enum class MessageRole{USER,ASSISTANT}

sealed class ChatState{
    object Idle: ChatState()
    object Streaming: ChatState()
    data class Error(val message: String): ChatState()
}