package com.example.android_ai_chatbot.domian.repository

import com.example.android_ai_chatbot.domian.model.Conversation
import com.example.android_ai_chatbot.domian.model.Message
import kotlinx.coroutines.flow.Flow

// domain/repository/ChatRepository.kt
interface ChatRepository {
    fun sendMessageStream(
        conversationId: String,
        prompt: String,
        history: List<Message>
    ): Flow<String>

    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun saveMessage(message: Message)
    suspend fun updateMessageContent(messageId: String, content: String)
    suspend fun deleteMessagesForConversation(conversationId: String)
    suspend fun generateTitle(prompt: String): String
    suspend fun deleteAllMessages()
}

interface ConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun createConversation(title: String): Conversation
    suspend fun renameConversation(id: String, newTitle: String)
    suspend fun deleteConversation(id: String)
    suspend fun updateTimestamp(id: String, timestamp: Long)
    suspend fun deleteAllConversations()
}
