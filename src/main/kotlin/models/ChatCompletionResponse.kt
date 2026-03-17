package models

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
