package com.aichat.data.repository

import com.aichat.data.local.ConversationDao
import com.aichat.data.local.ConversationEntity
import com.aichat.data.local.MessageDao
import com.aichat.data.local.MessageEntity
import com.example.android_ai_chatbot.BuildConfig
import com.example.android_ai_chatbot.data.remote.OpenAIApiService
import com.example.android_ai_chatbot.data.remote.OpenAIMessage
import com.example.android_ai_chatbot.data.remote.OpenAIRequest
import com.example.android_ai_chatbot.data.remote.OpenAIStreamChunk
import com.example.android_ai_chatbot.data.remote.toOpenAIRole
import com.example.android_ai_chatbot.domian.model.Conversation
import com.example.android_ai_chatbot.domian.model.Message
import com.example.android_ai_chatbot.domian.model.MessageRole
import com.example.android_ai_chatbot.domian.repository.ChatRepository
import com.example.android_ai_chatbot.domian.repository.ConversationRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: OpenAIApiService,
    private val messageDao: MessageDao
) : ChatRepository {

    private val gson = Gson()

    override fun sendMessageStream(
        conversationId: String,
        prompt: String,
        history: List<Message>,
        messageContent: Any
    ): Flow<String> = flow {

        val messages = buildList {
            add(OpenAIMessage(role = "system", content = "You are a helpful AI assistant."))
            history.takeLast(20).forEach { msg ->
                add(OpenAIMessage(role = msg.role.toOpenAIRole(), content = msg.content))
            }
            add(OpenAIMessage(role = "user", content = messageContent))
        }

        val response = apiService.streamChatCompletion(
            OpenAIRequest(model = BuildConfig.MODEL, messages = messages)
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw Exception("Groq error ${response.code()}: $errorBody")
        }

        val body = response.body()
            ?: throw Exception("Empty response body")

        body.source().use { source ->
            while (!source.exhausted()) {
                val readBuffer = okio.Buffer()
                source.read(readBuffer, 8192)
                val rawChunk = readBuffer.readUtf8()

                rawChunk.split("\n").forEach { line ->
                    when {
                        line.trim() == "data: [DONE]" -> return@forEach

                        line.startsWith("data: ") -> {
                            val json = line.removePrefix("data: ").trim()
                            if (json.isBlank()) return@forEach

                            runCatching {
                                val chunk = gson.fromJson(json, OpenAIStreamChunk::class.java)
                                chunk.choices
                                    ?.firstOrNull()
                                    ?.delta
                                    ?.content
                                    ?.takeIf { it.isNotEmpty() }
                            }.getOrNull()?.let { token ->
                                emit(token)
                            }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun getMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveMessage(message: Message) =
        messageDao.insertMessage(message.toEntity())

    override suspend fun updateMessageContent(messageId: String, content: String) =
        messageDao.updateMessageContent(messageId, content)

    override suspend fun deleteMessagesForConversation(conversationId: String) =
        messageDao.deleteMessagesForConversation(conversationId)

    override suspend fun generateTitle(prompt: String): String {
        val messages = listOf(
            OpenAIMessage(
                role = "system",
                content = "Generate a short 3-5 word title for a conversation that starts with the following message. Reply with ONLY the title, no punctuation, no quotes."
            ),
            OpenAIMessage(role = "user", content = prompt)
        )
        val response = apiService.streamChatCompletion(
            OpenAIRequest(
                model = BuildConfig.MODEL,
                messages = messages,
                stream = false,
                maxTokens = 20
            )
        )

        val body = response.body()?.string() ?: return "New Chat"
        return try {
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            json["choices"].asJsonArray[0].asJsonObject["message"]
                .asJsonObject["content"].asString.trim()
        } catch (e: Exception) {
            "New Chat"
        }
    }

    override suspend fun deleteAllMessages() = messageDao.deleteAllMessages()


}


private fun MessageEntity.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    content = content,
    role = MessageRole.valueOf(role),
    timestamp = timestamp,
    isStreaming = isStreaming,
    imageUri = imageUri
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    content = content,
    role = role.name,
    timestamp = timestamp,
    isStreaming = isStreaming,
    imageUri = imageUri
)


@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { list -> list.map { it.toDomain() } }

    override suspend fun createConversation(title: String): Conversation {
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(entity)
        return entity.toDomain()
    }

    override suspend fun renameConversation(id: String, newTitle: String) =
        conversationDao.renameConversation(id, newTitle)

    override suspend fun deleteConversation(id: String) =
        conversationDao.deleteConversation(id)

    override suspend fun updateTimestamp(id: String, timestamp: Long) =
        conversationDao.updateTimestamp(id, timestamp)

    override suspend fun deleteAllConversations() =
        conversationDao.deleteAllConversations()

    override fun searchConversations(query: String): Flow<List<Conversation>> =
        conversationDao.searchConversations("$query*")
            .map { list -> list.map { it.toDomain() } }
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt
)