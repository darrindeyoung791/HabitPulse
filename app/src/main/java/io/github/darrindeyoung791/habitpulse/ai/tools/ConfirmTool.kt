package io.github.darrindeyoung791.habitpulse.ai.tools

class ConfirmTool : Tool {
    override val name = "confirm"

    override fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult.Success(Unit)
    }
}
