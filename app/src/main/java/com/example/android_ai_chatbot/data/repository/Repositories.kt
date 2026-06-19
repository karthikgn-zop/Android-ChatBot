package com.example.android_ai_chatbot.data.repository

import android.content.Context
import com.aichat.data.local.ConversationDao
import com.aichat.data.local.ConversationEntity
import com.aichat.data.local.MessageDao
import com.aichat.data.local.MessageEntity
import com.example.android_ai_chatbot.BuildConfig
import com.example.android_ai_chatbot.core.util.ModelDownloader
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val messageDao: MessageDao,
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader
) : ChatRepository {

    private val gson = Gson()

    // ── LLM (lazy — only created when USE_LOCAL_MODEL = true) ────────────────
    private val llmInference: LlmInference? by lazy {
        if (!modelDownloader.isModelDownloaded) return@lazy null
        try {
            LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelDownloader.modelPath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(101)
                    .build()
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── Route to local or remote ──────────────────────────────────────────────
    override fun sendMessageStream(
        conversationId: String,
        prompt: String,
        history: List<Message>,
        messageContent: Any
    ): Flow<String> = if (BuildConfig.USE_LOCAL_MODEL) {
        localGemmaStream(prompt, history)
    } else {
        groqApiStream(prompt, history, messageContent)
    }

    // ── On-device Gemma (synchronous → simulated streaming) ──────────────────
    private fun localGemmaStream(
        prompt: String,
        history: List<Message>
    ): Flow<String> = flow {
        val llm = llmInference
            ?: throw Exception("Model not loaded. Please download the model first.")

        val fullPrompt = buildString {
            history.takeLast(10).forEach { msg ->
                if (msg.role == MessageRole.USER)
                    append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
                else
                    append("<start_of_turn>model\n${msg.content}<end_of_turn>\n")
            }
            append("<start_of_turn>user\n$prompt<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }

        // generateResponse is synchronous — runs fully then we stream word by word
        val fullResponse = llm.generateResponse(fullPrompt)

        // Simulate token streaming word by word
        fullResponse.split(" ").forEach { word ->
            emit("$word ")
            delay(30L)  // 30ms between words feels natural
        }
    }.flowOn(Dispatchers.IO)

    // ── Groq SSE streaming ────────────────────────────────────────────────────
    private fun groqApiStream(
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
                            }.getOrNull()?.let { emit(it) }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Messages ──────────────────────────────────────────────────────────────
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

    override suspend fun deleteAllMessages() =
        messageDao.deleteAllMessages()

    // ── Title generation (always uses Groq) ──────────────────────────────────
    override suspend fun generateTitle(prompt: String): String {
        val messages = listOf(
            OpenAIMessage(
                role    = "system",
                content = "Generate a short 3-5 word title for a conversation that starts with the following message. Reply with ONLY the title, no punctuation, no quotes."
            ),
            OpenAIMessage(role = "user", content = prompt)
        )
        val response = apiService.streamChatCompletion(
            OpenAIRequest(
                model     = BuildConfig.MODEL,
                messages  = messages,
                stream    = false,
                maxTokens = 20
            )
        )
        val body = response.body()?.string() ?: return "New Chat"
        return try {
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            json["choices"].asJsonArray[0]
                .asJsonObject["message"]
                .asJsonObject["content"]
                .asString.trim()
        } catch (e: Exception) {
            "New Chat"
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun MessageEntity.toDomain() = Message(
    id             = id,
    conversationId = conversationId,
    content        = content,
    role           = MessageRole.valueOf(role),
    timestamp      = timestamp,
    isStreaming    = isStreaming,
    imageUri       = imageUri
)

private fun Message.toEntity() = MessageEntity(
    id             = id,
    conversationId = conversationId,
    content        = content,
    role           = role.name,
    timestamp      = timestamp,
    isStreaming    = isStreaming,
    imageUri       = imageUri
)

// ── ConversationRepositoryImpl ────────────────────────────────────────────────

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { list -> list.map { it.toDomain() } }

    override suspend fun createConversation(title: String): Conversation {
        val entity = ConversationEntity(
            id        = UUID.randomUUID().toString(),
            title     = title,
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
}

private fun ConversationEntity.toDomain() = Conversation(
    id        = id,
    title     = title,
    createdAt = createdAt,
    updatedAt = updatedAt
)
