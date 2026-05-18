package io.github.darrindeyoung791.habitpulse.ai.llm

object ResponseParser {
    private val codeBlockPattern = Regex("""```(\w+)?\s*([\s\S]*?)```""")
    private val toolCallPattern = Regex("""(\w+)\s*\(\{[^}]*\})""")

    data class ParsedResponse(
        val text: String,
        val toolCalls: List<ToolCall>
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

        return ParsedResponse(
            text = textWithoutCodeBlocks.toString().trim(),
            toolCalls = toolCalls
        )
    }

    private fun extractToolName(content: String): String? {
        val patterns = listOf(
            Regex("""^\s*(\w+)\s*\(""""),
            Regex(""""\w+"\s*:\s*\{"""),
            Regex("""\{[\s\S]*?"(\w+)":\s*\{""")
        )

        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                val name = match.groupValues[1]
                if (isValidToolName(name)) {
                    return name
                }
            }
        }
        return null
    }

    private fun extractArguments(content: String): String {
        val patterns = listOf(
            Regex("""\{[\s\S]*$"""),
            Regex("""\{"[^"]*":\s*[\s\S]*$""")
        )

        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.value
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