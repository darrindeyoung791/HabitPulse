package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import io.github.darrindeyoung791.habitpulse.ai.llm.ToolDef

/**
 * 新版 AI 对话的工具注册表（挂起式工具）。
 *
 * 构造时注入具体工具，并把外部依赖（HabitRepository / UserPreferences）通过
 * `__repo` / `__prefs` 参数传给各个工具。
 */
class ChatToolRegistry(
    private val tools: List<ChatTool>
) {
    private val byName: Map<String, ChatTool> = tools.associateBy { it.name }

    fun toolDefs(): List<ToolDef> = tools.map { it.spec() }

    fun canExecute(name: String): Boolean = name in byName

    fun allToolNames(): Set<String> = byName.keys

    suspend fun execute(name: String, arguments: Map<String, Any?>): ChatToolResult {
        val tool = byName[name]
            ?: return ChatToolResult.Error("未知的工具: $name")
        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            ChatToolResult.Error("工具执行失败: ${e.message}")
        }
    }

    companion object {
        /** 供 ChatTool 从 arguments 获取注入的仓库/偏好，并附加到参数映射。 */
        fun buildArguments(
            arguments: Map<String, Any?>,
            repo: Any? = null,
            prefs: Any? = null
        ): Map<String, Any?> {
            val map = arguments.toMutableMap()
            if (repo != null) map["__repo"] = repo
            if (prefs != null) map["__prefs"] = prefs
            return map
        }
    }
}