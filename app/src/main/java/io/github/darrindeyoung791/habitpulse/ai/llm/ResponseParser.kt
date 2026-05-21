package io.github.darrindeyoung791.habitpulse.ai.llm

import com.google.gson.Gson

object ResponseParser {
    private val gson = Gson()
    private val codeBlockPattern = Regex("""```(\w+)?\s*([\s\S]*?)```""")
    private val thinkingPattern = Regex("""<thinking>([\s\S]*?)</thinking>""", RegexOption.IGNORE_CASE)

    data class ParsedResponse(
        val text: String,
        val toolCalls: List<ToolCall>,
        val thoughts: String = ""
    )

    data class ToolCall(
        val name: String,
        val arguments: String,
        val rawJson: String
    )

    fun parse(text: String): ParsedResponse {
        val toolCalls = mutableListOf<ToolCall>()
        val textWithoutCodeBlocks = StringBuilder()
        var lastEnd = 0

        val thoughtsBuilder = StringBuilder()
        thinkingPattern.findAll(text).forEach { match ->
            thoughtsBuilder.append(match.groupValues[1].trim())
            thoughtsBuilder.append("\n")
        }
        val thoughts = thoughtsBuilder.toString().trim()

        codeBlockPattern.findAll(text).forEach { match ->
            textWithoutCodeBlocks.append(text.substring(lastEnd, match.range.first))
            lastEnd = match.range.last + 1

            val language = match.groupValues[1]
            val content = match.groupValues[2].trim()

            if (language.isNotEmpty() && content.isNotEmpty()) {
                val name = extractToolName(content)
                if (name != null) {
                    val args = extractArguments(content)
                    toolCalls.add(ToolCall(name, args, match.value))
                } else {
                    textWithoutCodeBlocks.append(match.value)
                }
            } else {
                textWithoutCodeBlocks.append(match.value)
            }
        }

        textWithoutCodeBlocks.append(text.substring(lastEnd))

        var cleanText = textWithoutCodeBlocks.toString().trim()

        val toolCallsFromJson = parseToolCallsFromJson(text)
        toolCalls.addAll(toolCallsFromJson)

        if (toolCallsFromJson.isNotEmpty() && cleanText.startsWith("{") && isJsonResponse(cleanText)) {
            cleanText = ""
        }

        return ParsedResponse(
            text = cleanText,
            toolCalls = toolCalls,
            thoughts = thoughts
        )
    }

    private fun isJsonResponse(text: String): Boolean {
        return text.contains("\"choices\"") || text.contains("\"id\"")
    }

    private fun parseToolCallsFromJson(text: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        val normalized = normalizeJsonString(text)
        try {
            @Suppress("UNCHECKED_CAST")
            val map: Map<String, Any?> = gson.fromJson(normalized, Map::class.java) as? Map<String, Any?> ?: return toolCalls

            val choices = map["choices"] as? List<Map<String, Any?>>
            val choice = choices?.firstOrNull()
            val message = choice?.get("message") as? Map<String, Any?>
            val calls = message?.get("tool_calls") as? List<Map<String, Any?>>

            calls?.forEach { call ->
                val type = call["type"] as? String ?: return@forEach
                if (type != "function") return@forEach

                val function = call["function"] as? Map<String, Any?> ?: return@forEach
                val name = function["name"] as? String ?: return@forEach
                val arguments = function["arguments"] as? String ?: "{}"

                if (isValidToolName(name)) {
                    toolCalls.add(ToolCall(name, arguments, call.toString()))
                }
            }
        } catch (e: Exception) {
        }
        return toolCalls
    }

    private fun normalizeJsonString(json: String): String {
        return json
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u300C', '"')
            .replace('\u300D', '"')
    }

    private fun extractToolName(content: String): String? {
        val normalized = normalizeJsonString(content)

        for (pattern in listOf(
            Regex("""^\s*(\w+)\s*\("""),
            Regex(""""\w+"\s*:\s*\{"""),
            Regex("""\{[\s\S]*?"(\w+)":\s*\{""")
        )) {
            val match = pattern.find(normalized)
            if (match != null) {
                val name = match.groupValues[1]
                if (isValidToolName(name)) {
                    return name
                }
            }
        }

        return when {
            normalized.contains("\"text\"") -> "reply"
            normalized.contains("\"title\"") || normalized.contains("\"repeat_cycle\"") -> "create_habit"
            normalized.contains("\"type\"") && normalized.contains("\"prompt\"") -> "ask_question"
            else -> null
        }
    }

    private fun extractArguments(content: String): String {
        val normalized = normalizeJsonString(content)
        for (pattern in listOf(
            Regex("""\{[\s\S]*$"""),
            Regex("""\{"[^"]*":\s*[\s\S]*$""")
        )) {
            val match = pattern.find(normalized)
            if (match != null) {
                return match.value.trimEnd(')', ' ', '\n', '\r', '\t', ',')
            }
        }
        return "{}"
    }

    private fun isValidToolName(name: String): Boolean {
        val validNames = setOf("create_habit", "ask_question", "reply", "confirm")
        return validNames.contains(name)
    }

    fun extractCodeBlocks(text: String): List<String> {
        return codeBlockPattern.findAll(text).map { it.value }.toList()
    }

    fun removeCodeBlocks(text: String): String {
        return text.replace(codeBlockPattern, "")
    }
}