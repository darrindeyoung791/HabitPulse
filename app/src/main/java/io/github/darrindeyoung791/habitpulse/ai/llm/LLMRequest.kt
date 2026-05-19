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
    val error: ResponseError?,
    @SerializedName("usage")
    val usage: Usage?
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
    val content: String?,
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCallData>? = null
)

data class ToolCallData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("function")
    val function: FunctionCallData?
)

data class FunctionCallData(
    @SerializedName("name")
    val name: String?,
    @SerializedName("arguments")
    val arguments: String?
)

data class ResponseError(
    @SerializedName("message")
    val message: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

/**
 * SSE streaming chunk response from Zhipu / GLM API.
 * Matches the ChatCompletionChunk schema.
 */
data class ChatCompletionChunk(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("created")
    val created: Long? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("choices")
    val choices: List<ChunkChoice>? = null,
    @SerializedName("usage")
    val usage: Usage? = null
)

data class ChunkChoice(
    @SerializedName("index")
    val index: Int? = null,
    @SerializedName("delta")
    val delta: Delta? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class Delta(
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)
