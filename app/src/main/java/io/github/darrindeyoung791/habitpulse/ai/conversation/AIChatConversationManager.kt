package io.github.darrindeyoung791.habitpulse.ai.conversation

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.llm.Message
import io.github.darrindeyoung791.habitpulse.ai.llm.ToolCallData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatEvent
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatToolRegistry
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatToolResult
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SessionUsage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 新版 AI 对话引擎：原生 function-calling 多轮工具链。
 *
 * - 单轮 while 循环：执行 assistant 消息的全部 tool_calls，结果以 `role=tool` 回灌，
 *   直到无工具或命中暂停点（ask_question / create_habit / delete_habit）。
 * - 流式累积 `delta.tool_calls`；`Done` 空内容或无工具时回退一次非流式。
 * - `ToolResult.Error` 累计 `retryCount`，≥ MAX_RETRIES 触发错误事件。
 * - 设置类工具连续报错（≥3）触发 [ChatEvent.GuardBlocked]。
 * - 仅累计提供商返回的 usage（[SessionUsage]），无 usage 不计数。
 *
 * 与旧版 [ConversationManager]（文本 ```json 协议）并存，不破坏旧 AI。
 */
class AIChatConversationManager(
    private val llmClient: LLMClient,
    private val toolRegistry: ChatToolRegistry,
    private val streamingEnabled: Boolean,
    private val scope: CoroutineScope,
    private val systemPrompt: String,
    private val thinkingEnabled: Boolean = false,
    private val thinkingMessageId: String = "thinking",
    private val maxToolRetries: Int = 20
) {
    private val gson = Gson()

    private val _events = MutableStateFlow<ChatEvent?>(null)
    val events: StateFlow<ChatEvent?> = _events.asStateFlow()

    private val _usage = MutableStateFlow(SessionUsage())
    val usage: StateFlow<SessionUsage> = _usage.asStateFlow()

    private val messages = mutableListOf<Message>()

    private val guard = ConversationGuard()
    private var retryCount = 0
    private var isStopped = false
    private var isStreaming = false
    private var needUserPause = false

    suspend fun startConversation(userInput: String) {
        if (isStopped) return
        retryCount = 0
        messages.add(Message(role = "system", content = systemPrompt))
        messages.add(Message(role = "user", content = userInput))
        sendToLLM()
    }

    suspend fun continueConversation(userInput: String) {
        if (isStopped) return
        needUserPause = false
        messages.add(Message(role = "user", content = userInput))
        sendToLLM()
    }

    suspend fun submitAnswer(answer: String) {
        if (isStopped) return
        needUserPause = false
        messages.add(Message(role = "user", content = answer))
        sendToLLM()
    }

    suspend fun stop() {
        isStopped = true
        isStreaming = false
        _events.value = ChatEvent.Stopped
    }

    fun resume() {
        isStopped = false
        needUserPause = false
    }

    fun reset() {
        messages.clear()
        _events.value = null
        _usage.value = SessionUsage()
        retryCount = 0
        isStopped = false
        isStreaming = false
        needUserPause = false
        guard.resetInvalidSettingTries()
    }

    /** 删除最后一个 assistant turn（含其 role=tool 尾消息）后重发。 */
    suspend fun retryLastTurn() {
        if (isStreaming || isStopped) return
        isStopped = false
        removeLastAssistantTurn()
        retryCount = 0
        guard.resetInvalidSettingTries()
        needUserPause = false
        sendToLLM()
    }

    private fun removeLastAssistantTurn() {
        val lastAssistant = messages.indexOfLast { it.role == "assistant" }
        if (lastAssistant >= 0) {
            while (messages.size > lastAssistant) {
                messages.removeAt(messages.lastIndex)
            }
        }
    }

    private suspend fun sendToLLM() {
        if (isStopped) return
        isStreaming = true

        var fallbackAttempted = false

        while (true) {
            if (isStopped) break

            val turn = if (streamingEnabled && !fallbackAttempted) {
                runStreamingTurn()
            } else {
                runNonStreamingTurn()
            }

            if (turn == null) {
                // 出错或已停止（错误事件已发出），结束本轮
                break
            }

            // 流式空内容且无工具 → 回退一次非流式
            if (turn.usedStreaming && turn.toolCalls.isEmpty() && turn.content.isBlank() && !fallbackAttempted) {
                fallbackAttempted = true
                continue
            }

            fallbackAttempted = false

            if (turn.toolCalls.isEmpty()) {
                // 非流式：补发最终正文（流式路径已在 Done 时发送）
                if (!turn.usedStreaming && turn.content.isNotBlank()) {
                    _events.value = ChatEvent.AssistantText(turn.content, turn.thoughts)
                }
                break
            }

            // 有工具调用：执行并回灌
            var pause = false
            var hadToolError = false
            for (tc in turn.toolCalls) {
                val name = tc.function?.name ?: continue
                val argsJson = tc.function?.arguments ?: "{}"
                val args = parseArguments(argsJson)
                val executionArgs = ChatToolRegistry.buildArguments(
                    args,
                    repo = contextRepo,
                    prefs = contextPrefs
                )

                when (val outcome = toolRegistry.execute(name, executionArgs)) {
                    is ChatToolResult.Success -> {
                        messages.add(
                            Message(
                                role = "tool",
                                content = encodeResult(outcome.data),
                                toolCallId = tc.id
                            )
                        )
                        _events.value = ChatEvent.ToolExecuted(name, outcome.data)
                        retryCount = 0
                        when (name) {
                            "ask_question", "create_habit", "delete_habit" -> {
                                pause = true
                                _events.value = ChatEvent.PauseForUser(name)
                            }
                        }
                        if (name == "update_setting") {
                            guard.resetInvalidSettingTries()
                        }
                    }
                    is ChatToolResult.Error -> {
                        hadToolError = true
                        retryCount++
                        if (name == "update_setting" || name == "open_settings_page") {
                            guard.recordInvalidSetting()
                        }
                        messages.add(
                            Message(
                                role = "tool",
                                content = "错误：${outcome.message}",
                                toolCallId = tc.id
                            )
                        )
                        _events.value = ChatEvent.ToolError(name, outcome.message)
                        if (retryCount >= maxToolRetries) {
                            _events.value = ChatEvent.Error("已自动重试${maxToolRetries}次失败: ${outcome.message}")
                            isStreaming = false
                            return
                        }
                    }
                }
            }

            if (hadToolError && guard.check(ConversationState()).shouldStop) {
                _events.value = ChatEvent.GuardBlocked
                isStreaming = false
                isStopped = true
                return
            }

            if (pause) {
                isStreaming = false
                break
            }

            // 无暂停且成功 → 继续循环驱动下一轮
        }

        isStreaming = false
    }

    /** 上下文注入（由构造方通过 setContextDependencies 设置）。 */
    private var contextRepo: Any? = null
    private var contextPrefs: Any? = null

    fun setContextDependencies(repo: Any?, prefs: Any?) {
        contextRepo = repo
        contextPrefs = prefs
    }

    private data class TurnResult(
        val content: String,
        val thoughts: String,
        val toolCalls: List<ToolCallData>,
        val usedStreaming: Boolean
    )

    private suspend fun runNonStreamingTurn(): TurnResult? {
        lastTurnResult = null
        val job = scope.launch(Dispatchers.IO) {
            ensureActive()
            when (val result = llmClient.chat(messages, toolRegistry.toolDefs(), "auto")) {
                is LLMClient.LLMResult.Success -> {
                    result.usage?.let { accUsage(it) }
                    messages.add(
                        Message(
                            role = "assistant",
                            content = result.content,
                            toolCalls = result.toolCalls.ifEmpty { null }
                        )
                    )
                    lastTurnResult = TurnResult(
                        content = result.content,
                        thoughts = result.thoughts,
                        toolCalls = result.toolCalls,
                        usedStreaming = false
                    )
                }
                is LLMClient.LLMResult.Error -> {
                    _events.value = ChatEvent.Error(result.message)
                }
            }
        }
        try {
            job.join()
        } catch (e: CancellationException) {
            job.cancel()
        }
        return lastTurnResult
    }

    private var lastTurnResult: TurnResult? = null

    private suspend fun runStreamingTurn(): TurnResult? {
        lastTurnResult = null
        var fullContent = StringBuilder()
        var thoughts = StringBuilder()
        var toolCalls = emptyList<ToolCallData>()
        var llmError: String? = null

        val job = scope.launch(Dispatchers.IO) {
            try {
                llmClient.chatStream(messages, toolRegistry.toolDefs(), "auto").collect { chunk ->
                    ensureActive()
                    if (isStopped) throw CancellationException("stopped")
                    when (chunk) {
                        is LLMClient.StreamChunk.Content -> {
                            fullContent.append(chunk.delta)
                            _events.value = ChatEvent.AssistantStreaming(fullContent.toString())
                        }
                        is LLMClient.StreamChunk.Reasoning -> {
                            thoughts.append(chunk.delta)
                            if (thinkingEnabled) {
                                _events.value = ChatEvent.ThinkingStarted(thinkingMessageId)
                                _events.value = ChatEvent.ThinkingUpdated(thoughts.toString())
                            }
                        }
                        is LLMClient.StreamChunk.ToolCallComplete -> {
                            toolCalls = chunk.toolCalls
                        }
                        is LLMClient.StreamChunk.Usage -> {
                            accUsage(chunk.usage)
                        }
                        is LLMClient.StreamChunk.Done -> {
                            _events.value = ChatEvent.AssistantText(fullContent.toString(), thoughts.toString())
                            if (thinkingEnabled && thoughts.isNotEmpty()) {
                                _events.value = ChatEvent.ThinkingEnded(thinkingMessageId)
                            }
                            if (toolCalls.isNotEmpty()) {
                                // 关键：流式工具调用必须回灌 assistant(tool_calls) 消息，
                                // 否则后续 role=tool 消息会因缺少前置 tool_calls 而被服务端拒绝。
                                messages.add(
                                    Message(
                                        role = "assistant",
                                        content = fullContent.toString(),
                                        toolCalls = toolCalls
                                    )
                                )
                            }
                            lastTurnResult = TurnResult(
                                content = fullContent.toString(),
                                thoughts = thoughts.toString(),
                                toolCalls = toolCalls,
                                usedStreaming = true
                            )
                        }
                        is LLMClient.StreamChunk.ToolCallFragment -> {}
                        is LLMClient.StreamChunk.Error -> {
                            llmError = chunk.message
                            _events.value = ChatEvent.Error(chunk.message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                llmError = e.message
                _events.value = ChatEvent.Error(e.message ?: "连接失败")
            }
        }
        try {
            job.join()
        } catch (e: CancellationException) {
            job.cancel()
            return null
        }
        if (llmError != null) return null
        return lastTurnResult
    }

    private fun accUsage(usage: LLMClient.TokenUsage) {
        _usage.value = _usage.value.add(
            usage.promptTokens,
            usage.completionTokens,
            usage.totalTokens
        )
        _events.value = ChatEvent.Usage(_usage.value)
    }

    private fun parseArguments(json: String): Map<String, Any?> {
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, Map::class.java) as? Map<String, Any?> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun encodeResult(data: Any?): String {
        return try {
            gson.toJson(data)
        } catch (e: Exception) {
            "{}"
        }
    }
}
