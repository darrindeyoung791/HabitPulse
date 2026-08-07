package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData

/** `ask_question`：向用户提问并暂停等待回答。 */
object AskQuestionChatTool : ChatTool {
    override val name = "ask_question"

    private val validTypes = setOf("choice", "time", "day_of_week", "multi_choice", "text", "confirm", "time_of_day")

    override fun spec() = functionSpec(
        name = name,
        description = "向用户提问以澄清意图。当信息不完整或意图含糊（如多个同名习惯）时使用。调用后暂停等待用户回答。",
        properties = mapOf(
            "type" to mapOf("type" to "string", "enum" to validTypes.toList()),
            "prompt" to mapOf("type" to "string", "description" to "向用户展示的问题"),
            "options" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "可选答案"),
            "allow_custom_input" to mapOf("type" to "boolean", "description" to "是否允许自定义输入"),
            "question_id" to mapOf("type" to "string", "description" to "问题 id（可选，缺省自动生成）")
        ),
        required = listOf("type", "prompt")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val type = arguments["type"]?.toString()?.lowercase().orEmpty()
        if (type !in validTypes) {
            return ChatToolResult.Error("无效的问题类型: $type，可选值: ${validTypes.joinToString()}")
        }
        val prompt = arguments["prompt"]?.toString()
        if (prompt.isNullOrBlank()) {
            return ChatToolResult.Error("问题描述不能为空")
        }
        val options = parseStringList(arguments["options"])
        val allowCustom = arguments["allow_custom_input"]?.toString()?.toBooleanStrictOrNull() ?: false
        val questionId = arguments["question_id"]?.toString() ?: "q_${System.currentTimeMillis()}_${(0..999).random()}"
        return ChatToolResult.Success(
            PendingQuestionData(questionId, type, prompt, options, allowCustom)
        )
    }

    private val gson = Gson()
    private fun parseStringList(value: Any?): List<String> = when (value) {
        is List<*> -> value.mapNotNull { it?.toString() }
        is String -> try {
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        else -> emptyList()
    }
}

/** `reply`：纯文本回复。 */
object ReplyChatTool : ChatTool {
    override val name = "reply"

    override fun spec() = functionSpec(
        name = name,
        description = "向用户回复一段纯文本消息。",
        properties = mapOf("text" to mapOf("type" to "string", "description" to "回复文本，≤500 字")),
        required = listOf("text")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val text = arguments["text"]?.toString().orEmpty()
        if (text.isBlank()) return ChatToolResult.Error("回复内容不能为空")
        if (text.length > 500) return ChatToolResult.Error("回复内容过长，请控制在500字以内")
        return ChatToolResult.Success(ReplyChatData(text))
    }
}

data class ReplyChatData(val text: String)