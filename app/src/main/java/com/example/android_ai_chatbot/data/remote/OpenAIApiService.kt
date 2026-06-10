package com.example.android_ai_chatbot.data.remote

import com.example.android_ai_chatbot.domian.model.MessageRole
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming


data class OpenAIRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<OpenAIMessage>,
    val stream: Boolean = true,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2048,
    val temperature: Double = 0.9
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIStreamChunk(
    val id: String?,
    val choices: List<OpenAIStreamChoice>?
)

data class OpenAIStreamChoice(
    val delta: OpenAIDelta?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class OpenAIDelta(
    val content: String?,
    val role: String?
)


interface OpenAIApiService {
    @Streaming
    @Headers("Accept: text/event-stream")
    @POST("v1/chat/completions")
    suspend fun streamChatCompletion(
        @Body request: OpenAIRequest
    ): Response<ResponseBody>

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Body request: OpenAIRequest
    ): Response<ResponseBody>
}


fun MessageRole.toOpenAIRole(): String = when (this) {
    MessageRole.USER -> "user"
    MessageRole.ASSISTANT -> "assistant"
}