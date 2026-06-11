package com.example.android_ai_chatbot.domian.usecase

import com.example.android_ai_chatbot.domian.model.Conversation
import com.example.android_ai_chatbot.domian.model.Message
import com.example.android_ai_chatbot.domian.model.MessageRole
import com.example.android_ai_chatbot.domian.repository.ChatRepository
import com.example.android_ai_chatbot.domian.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepo: ChatRepository,
    private val conversationRepo: ConversationRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        userText: String,
        history: List<Message>,
        messageContent: Any = userText
    ): Flow<String> {
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            content = userText,
            role = MessageRole.USER,
            timestamp = System.currentTimeMillis()
        )
        chatRepo.saveMessage(userMessage)
        conversationRepo.updateTimestamp(conversationId, System.currentTimeMillis())
        return chatRepo.sendMessageStream(conversationId, userText, history, messageContent)
    }
}

class GetMessagesUseCase @Inject constructor(
    private val chatRepo: ChatRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> =
        chatRepo.getMessages(conversationId)
}

class GetConversationsUseCase @Inject constructor(
    private val conversationRepo: ConversationRepository
) {
    operator fun invoke(): Flow<List<com.example.android_ai_chatbot.domian.model.Conversation>> =
        conversationRepo.getAllConversations()
}

class CreateConversationUseCase @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend operator fun invoke(title: String): Conversation {
        return repository.createConversation(title)
    }
}

class DeleteConversationUseCase @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val chatRepo: ChatRepository
) {
    suspend operator fun invoke(conversationId: String) {
        chatRepo.deleteMessagesForConversation(conversationId)
        conversationRepo.deleteConversation(conversationId)
    }
}

class DeleteAllConversationsUseCase @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val chatRepo: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepo.deleteAllMessages()
        conversationRepo.deleteAllConversations()
    }
}