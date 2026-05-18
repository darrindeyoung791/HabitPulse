package io.github.darrindeyoung791.habitpulse.ai.llm

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>,
    @SerializedName("stream")
    val stream: Boolean = false,
    @SerializedName("temperature")
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096
)

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class ChatResponse(
    @SerializedName("id")
    val id: String?,
    @SerializedName("model")
    val model: String?,
    @SerializedName("choices")
    val choices: List<Choice>?,
    @SerializedName("error")
    val error: ResponseError?
)

data class Choice(
    @SerializedName("index")
    val index: Int?,
    @SerializedName("message")
    val message: AssistantMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class AssistantMessage(
    @SerializedName("role")
    val role: String?,
    @SerializedName("content")
    val content: String?
)

data class ResponseError(
    @SerializedName("message")
    val message: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)