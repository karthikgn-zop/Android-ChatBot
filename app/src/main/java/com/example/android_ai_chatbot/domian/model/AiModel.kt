package com.example.android_ai_chatbot.domian.model

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val supportsImages: Boolean = false
)

object AvailableModels {
    val models = listOf(
        AiModel(
            id = "llama-3.1-8b-instant",
            displayName = "Llama 3.1 8B",
            description = "Fast and efficient for everyday tasks",
            supportsImages = false
        ),
        AiModel(
            id = "llama-3.3-70b-versatile",
            displayName = "Llama 3.3 70B",
            description = "More capable, best for complex tasks",
            supportsImages = false
        ),
        AiModel(
            id = "meta-llama/llama-4-scout-17b-16e-instruct",
            displayName = "Llama 4 Scout",
            description = "Multimodal — supports text and images",
            supportsImages = true
        ),
        AiModel(
            id = "gemma2-9b-it",
            displayName = "Gemma 2 9B",
            description = "Google's open model, great for reasoning",
            supportsImages = false
        ),
        AiModel(
            id = "mixtral-8x7b-32768",
            displayName = "Mixtral 8x7B",
            description = "Large context window — up to 32k tokens",
            supportsImages = false
        )
    )

    val default = models.first()

    fun findById(id: String) = models.find { it.id == id } ?: default
}