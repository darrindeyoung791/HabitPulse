package io.github.darrindeyoung791.habitpulse.ai.conversation

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.llm.Message
import io.github.darrindeyoung791.habitpulse.ai.llm.ResponseParser
import io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData
import io.github.darrindeyoung791.habitpulse.ai.tools.ReplyData
import io.github.darrindeyoung791.habitpulse.ai.tools.ToolRegistry
import io.github.darrindeyoung791.habitpulse.ai.tools.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConversationManager(
    private val llmClient: LLMClient,
    private val toolRegistry: ToolRegistry,
    private val streamingEnabled: Boolean = false,
    private val scope: CoroutineScope,
    private val systemPrompt: String
) {
    private val gson = Gson()

    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val messages = mutableListOf<Message>()
    private val _collectedHabits = MutableStateFlow<List<PartialHabit>>(emptyList())
    val collectedHabits: StateFlow<List<PartialHabit>> = _collectedHabits.asStateFlow()

    private val _events = MutableStateFlow<ConversationEvent?>(null)
    val events: StateFlow<ConversationEvent?> = _events.asStateFlow()

    private var isStreaming = false
    private var streamJob: kotlinx.coroutines.Job? = null
    private var retryCount = 0
    private var needsRetry = false
    private var needsAutoContinue = false

    companion object {
        private const val MAX_RETRIES = 10
    }

    sealed class ConversationEvent {
        data class AIMessageReceived(val text: String, val thoughts: String = "", val isStreaming: Boolean = false) : ConversationEvent()
        data class QuestionReceived(val question: PendingQuestionData) : ConversationEvent()
        data class ReplyReceived(val text: String) : ConversationEvent()
        data class HabitCreated(val habit: PartialHabit) : ConversationEvent()
        data class ConfirmationRequested(val habits: List<PartialHabit>) : ConversationEvent()
        data class Error(val message: String) : ConversationEvent()
        object GuardBlocked : ConversationEvent()
        data class ThinkingStarted(val messageId: String) : ConversationEvent()
        data class ThinkingUpdated(val messageId: String, val thoughts: String) : ConversationEvent()
        data class ThinkingEnded(val messageId: String) : ConversationEvent()
        object Stopped : ConversationEvent()
    }

    suspend fun startConversation(userInput: String) {
        retryCount = 0
        messages.add(Message(role = "system", content = systemPrompt))
        messages.add(Message(role = "user", content = userInput))
        sendToLLM()
    }

    suspend fun submitAnswer(answer: String, questionId: String) {
        if (_state.value.isStopped) return
        retryCount = 0

        messages.add(Message(role = "user", content = answer))
        sendToLLM()
    }

    suspend fun stop() {
        streamJob?.cancel()
        isStreaming = false
        needsRetry = false
        needsAutoContinue = false
        _state.value = _state.value.copy(isStopped = true)

        _events.value = ConversationEvent.Stopped
    }

    private suspend fun sendToLLM() {
        if (_state.value.isStopped) return

        if (streamingEnabled) {
            sendToLLMStreaming()
        } else {
            sendToLLMWithRetry()
        }
    }

    private suspend fun sendToLLMStreaming() {
        isStreaming = true
        _state.value = _state.value.copy(isStopped = false)
        var streamingText = ""
        var alreadyProcessed = false
        val completeJsonBlock = Regex("```json[\\s\\S]*?```")

        streamJob = scope.launch(Dispatchers.IO) {
            try {
                llmClient.chatStream(messages).collect { chunk ->
                    ensureActive()
                    if (_state.value.isStopped) {
                        throw kotlinx.coroutines.CancellationException("stopped by user")
                    }
                    if (alreadyProcessed) return@collect
                    when (chunk) {
                        is LLMClient.StreamChunk.Content -> {
                            streamingText += chunk.delta
                            _events.value = ConversationEvent.AIMessageReceived(streamingText, isStreaming = true)
                            if (completeJsonBlock.containsMatchIn(streamingText)) {
                                alreadyProcessed = true
                                processResponse(streamingText)
                                throw kotlinx.coroutines.CancellationException("early stop - complete tool call detected")
                            }
                        }
                        is LLMClient.StreamChunk.Done -> {
                            processResponse(chunk.fullContent)
                        }
                        is LLMClient.StreamChunk.ToolCallFragment -> {}
                        is LLMClient.StreamChunk.ToolCallComplete -> {}
                        is LLMClient.StreamChunk.Reasoning -> {}
                        is LLMClient.StreamChunk.Usage -> {}
                        is LLMClient.StreamChunk.Error -> {
                            _events.value = ConversationEvent.Error(chunk.message)
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) { }
        }
        try {
            streamJob?.join()
        } catch (_: kotlinx.coroutines.CancellationException) {
            streamJob?.cancel()
        }
        isStreaming = false
    }

    private suspend fun sendToLLMWithRetry() {
        do {
            if (_state.value.isStopped) break

            needsAutoContinue = false
            needsRetry = false

            isStreaming = true
            _state.value = _state.value.copy(isStopped = false)

            streamJob = scope.launch(Dispatchers.IO) {
                try {
                    ensureActive()
                    when (val result = llmClient.chat(messages)) {
                        is LLMClient.LLMResult.Success -> processResponse(result.content)
                        is LLMClient.LLMResult.Error -> _events.value = ConversationEvent.Error(result.message)
                    }
                } catch (_: kotlinx.coroutines.CancellationException) { }
            }
            try {
                streamJob?.join()
            } catch (_: kotlinx.coroutines.CancellationException) {
                streamJob?.cancel()
            }

            isStreaming = false

            if (needsAutoContinue) {
                retryCount = 0
            }
        } while ((needsRetry || needsAutoContinue) && retryCount < MAX_RETRIES && !_state.value.isStopped)
    }

    private fun processResponse(content: String) {
        needsRetry = false
        val parsed = ResponseParser.parse(content)

        var displayText = parsed.text
        val toolCalls = parsed.toolCalls
        val thoughts = parsed.thoughts

        messages.add(Message(role = "assistant", content = content))

        if (thoughts.isNotEmpty()) {
            _events.value = ConversationEvent.ThinkingEnded("")
        }

        var createHabitCalled = false

        for (toolCall in toolCalls) {
            if (!toolRegistry.canExecute(toolCall.name)) {
                continue
            }

            val arguments = parseArguments(toolCall.arguments)
            val toolResult = toolRegistry.execute(toolCall.name, arguments)

            when (toolResult) {
                is ToolResult.Success -> {
                    when (toolCall.name) {
                        "ask_question" -> {
                            val question = toolResult.data as? PendingQuestionData
                            if (question != null) {
                                _state.value = _state.value.withIncrementedQuestionCount(question.questionId)
                                _events.value = ConversationEvent.QuestionReceived(question)
                            }
                        }
                        "create_habit" -> {
                            createHabitCalled = true
                            val habit = toolResult.data as? PartialHabit
                            if (habit != null) {
                                _collectedHabits.value = _collectedHabits.value + habit
                                _state.value = _state.value.withIncrementedHabitCount()
                                _events.value = ConversationEvent.HabitCreated(habit)
                            }
                        }
                        "reply" -> {
                            val reply = toolResult.data as? ReplyData
                            if (reply != null) {
                                _events.value = ConversationEvent.ReplyReceived(reply.text)
                            }
                        }
                        "confirm" -> {
                            // no-op: confirm is now UI-triggered only
                        }
                    }
                }
                is ToolResult.Error -> {
                    retryCount++
                    if (retryCount < MAX_RETRIES) {
                        messages.add(Message(role = "user", content = "[\"${toolCall.name}: 执行出错 - ${toolResult.message}\"]"))
                        needsRetry = true
                    } else {
                        _events.value = ConversationEvent.Error("已自动重试${MAX_RETRIES}次失败: ${toolResult.message}")
                    }
                }
            }
        }

        if (toolCalls.isEmpty() || displayText.isNotEmpty()) {
            _events.value = ConversationEvent.AIMessageReceived(displayText, thoughts, isStreaming = false)
        }

        val guard = ConversationGuard()
        if (guard.shouldStopConversation(_state.value)) {
            _events.value = ConversationEvent.GuardBlocked
            _state.value = _state.value.copy(isStopped = true)
        }

        // After create_habit, the conversation stops naturally (no auto-continue).
        // User confirms via the habit card button, then a continuation message is sent.
    }

    private fun parseArguments(json: String): Map<String, Any?> {
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, Map::class.java) as? Map<String, Any?> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun removeLastAssistantTurn() {
        val lastAssistant = messages.indexOfLast { it.role == "assistant" }
        if (lastAssistant >= 0) {
            while (messages.size > lastAssistant) {
                messages.removeAt(messages.lastIndex)
            }
        }
    }

    suspend fun retry() {
        if (isStreaming) return

        _state.value = _state.value.copy(isStopped = false)

        val lastAssistant = messages.indexOfLast { it.role == "assistant" }
        if (lastAssistant >= 0) {
            while (messages.size > lastAssistant) {
                messages.removeAt(messages.lastIndex)
            }
        }

        retryCount = 0
        needsRetry = false
        needsAutoContinue = false
        sendToLLM()
    }

    suspend fun continueConversation(userInput: String) {
        if (_state.value.isStopped) return

        messages.add(Message(role = "user", content = userInput))
        sendToLLM()
    }

    fun resume() {
        _state.value = _state.value.copy(isStopped = false)
        retryCount = 0
        needsRetry = false
        needsAutoContinue = false
    }

    fun reset() {
        messages.clear()
        _collectedHabits.value = emptyList()
        _state.value = ConversationState()
        _events.value = null
        retryCount = 0
        needsRetry = false
        needsAutoContinue = false
    }
}
