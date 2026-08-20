package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import io.github.darrindeyoung791.habitpulse.ai.llm.FunctionDef
import io.github.darrindeyoung791.habitpulse.ai.llm.ToolDef

/**
 * 新版 AI 对话的「可挂起」工具。
 *
 * 与旧版 [io.github.darrindeyoung791.habitpulse.ai.tools.Tool] 的区别：
 * - `execute` 是 suspend（search_habits / settings 等需要 DB / DataStore 读写）；
 * - 每个工具自带 `spec()`，用于生成 function-calling 的 `ToolDef`（JSON Schema）。
 *
 * 旧 AI 沿用 [io.github.darrindeyoung791.habitpulse.ai.tools.Tool]（非挂起、无 spec），
 * 本接口只服务于新版 `AIChatConversationManager`，不破坏旧路径。
 */
interface ChatTool {
    val name: String
    fun spec(): ToolDef
    suspend fun execute(arguments: Map<String, Any?>): ChatToolResult
}

sealed class ChatToolResult {
    data class Success(val data: Any?) : ChatToolResult()
    data class Error(val message: String) : ChatToolResult()
}

/** 便捷构造 function-calling 工具定义的辅助函数。 */
fun functionSpec(
    name: String,
    description: String,
    properties: Map<String, Map<String, Any?>> = emptyMap(),
    required: List<String> = emptyList()
): ToolDef {
    val parameters: MutableMap<String, Any?> = if (properties.isEmpty()) {
        linkedMapOf("type" to "object")
    } else {
        linkedMapOf(
            "type" to "object",
            "properties" to properties,
            "required" to required
        )
    }
    return ToolDef(
        function = FunctionDef(
            name = name,
            description = description,
            parameters = parameters
        )
    )
}