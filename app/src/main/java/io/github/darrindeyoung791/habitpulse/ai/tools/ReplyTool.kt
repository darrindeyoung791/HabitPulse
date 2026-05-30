package io.github.darrindeyoung791.habitpulse.ai.tools

class ReplyTool : Tool {
    override val name = "reply"

    override fun execute(arguments: Map<String, Any?>): ToolResult {
        val text = arguments["text"]?.toString()
        if (text.isNullOrBlank()) {
            return ToolResult.Error("回复内容不能为空")
        }

        if (text.length > 500) {
            return ToolResult.Error("回复内容过长，请控制在500字以内")
        }

        return ToolResult.Success(ReplyData(text))
    }
}

data class ReplyData(
    val text: String
)