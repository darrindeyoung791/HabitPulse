package io.github.darrindeyoung791.habitpulse.ai.tools

sealed class ToolResult {
    data class Success(val data: Any?) : ToolResult()
    data class Error(val message: String) : ToolResult()
}

interface Tool {
    val name: String
    fun execute(arguments: Map<String, Any?>): ToolResult
}

interface ToolExecutor {
    fun canExecute(toolName: String): Boolean
    fun execute(toolName: String, arguments: Map<String, Any?>): ToolResult
}