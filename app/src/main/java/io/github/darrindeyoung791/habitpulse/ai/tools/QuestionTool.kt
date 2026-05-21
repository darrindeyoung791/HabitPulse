package io.github.darrindeyoung791.habitpulse.ai.tools

import com.google.gson.Gson

class QuestionTool : Tool {
    override val name = "ask_question"

    private val gson = Gson()

    override fun execute(arguments: Map<String, Any?>): ToolResult {
        val type = arguments["type"]?.toString()?.lowercase()
        if (type.isNullOrBlank()) {
            return ToolResult.Error("问题类型不能为空")
        }

        val validTypes = setOf("choice", "time", "day_of_week", "multi_choice", "text", "confirm", "time_of_day")
        if (type !in validTypes) {
            return ToolResult.Error("无效的问题类型: $type，可选值: ${validTypes.joinToString()}")
        }

        val prompt = arguments["prompt"]?.toString()
        if (prompt.isNullOrBlank()) {
            return ToolResult.Error("问题描述不能为空")
        }

        val options = parseStringList(arguments["options"])
        val allowCustomInput = arguments["allow_custom_input"]?.toString()?.toBooleanStrictOrNull() ?: false
        val questionId = arguments["question_id"]?.toString() ?: generateQuestionId()

        val question = PendingQuestionData(
            questionId = questionId,
            type = type,
            prompt = prompt,
            options = options,
            allowCustomInput = allowCustomInput
        )

        return ToolResult.Success(question)
    }

    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun generateQuestionId(): String {
        return "q_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}