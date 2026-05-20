package io.github.darrindeyoung791.habitpulse.ai.tools

class ToolRegistry : ToolExecutor {
    private val tools: Map<String, Tool> = listOf(
        CreateHabitTool(),
        QuestionTool(),
        ReplyTool()
    ).associateBy { it.name }

    override fun canExecute(toolName: String): Boolean {
        return toolName in tools
    }

    override fun execute(toolName: String, arguments: Map<String, Any?>): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult.Error("未知的工具: $toolName")

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            ToolResult.Error("工具执行失败: ${e.message}")
        }
    }

    fun getAllToolNames(): List<String> = tools.keys.toList()
}