package io.github.darrindeyoung791.habitpulse.ai.conversation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.llm.Message
import io.github.darrindeyoung791.habitpulse.ai.llm.ResponseParser
import io.github.darrindeyoung791.habitpulse.ai.prompt.SystemPrompt
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
    private val habitCountExtractor: HabitCountExtractor,
    private val fallbackReply: FallbackReply,
    private val streamingEnabled: Boolean = false,
    private val scope: CoroutineScope
) {
    private val gson = Gson()

    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val messages = mutableListOf<Message>()
    private val _collectedHabits = MutableStateFlow<List<PartialHabit>>(emptyList())
    val collectedHabits: StateFlow<List<PartialHabit>> = _collectedHabits.asStateFlow()

    private val _events = MutableStateFlow<ConversationEvent?>(null)
    val events: StateFlow<ConversationEvent?> = _events.asStateFlow()

    private var currentLanguage = "zh"
    private var isStreaming = false
    private var streamJob: kotlinx.coroutines.Job? = null

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
    }

    suspend fun startConversation(userInput: String) {
        currentLanguage = SystemPrompt.detectLanguage(userInput)
        val isHabitRelated = habitCountExtractor.isHabitRelated(userInput)

        _state.value = _state.value.copy(isHabitRelated = isHabitRelated)

        if (!isHabitRelated) {
            val reply = fallbackReply.generate(userInput, currentLanguage)
            _events.value = ConversationEvent.ReplyReceived(reply)
            return
        }

        val countResult = habitCountExtractor.extract(userInput, currentLanguage)
        _state.value = _state.value.copy(
            pendingHabitCount = when (countResult) {
                is HabitCountExtractor.HabitCountResult.Explicit -> countResult.count
                is HabitCountExtractor.HabitCountResult.Sequential -> countResult.count
                is HabitCountExtractor.HabitCountResult.Unknown -> null
            }
        )

        messages.add(Message(role = "system", content = SystemPrompt.getSystemPrompt(currentLanguage)))
        messages.add(Message(role = "user", content = userInput))

        sendToLLM()
    }

    suspend fun submitAnswer(answer: String, questionId: String) {
        if (_state.value.isStopped) return

        messages.add(Message(role = "user", content = answer))
        sendToLLM()
    }

    suspend fun stop() {
        streamJob?.cancel()
        isStreaming = false
        _state.value = _state.value.copy(isStopped = true)

        val currentHabits = _collectedHabits.value
        if (currentHabits.isNotEmpty()) {
            _events.value = ConversationEvent.ConfirmationRequested(currentHabits)
        }
    }

    private suspend fun sendToLLM() {
        isStreaming = true
        _state.value = _state.value.copy(isStopped = false)

        if (streamingEnabled) {
            var streamingText = ""
            streamJob = scope.launch(Dispatchers.IO) {
                try {
                    llmClient.chatStream(messages).collect { chunk ->
                        ensureActive()
                        if (_state.value.isStopped) {
                            throw kotlinx.coroutines.CancellationException("stopped by user")
                        }
                        when (chunk) {
                            is LLMClient.StreamChunk.Content -> {
                                streamingText += chunk.delta
                                _events.value = ConversationEvent.AIMessageReceived(
                                    streamingText,
                                    isStreaming = true
                                )
                            }
                            is LLMClient.StreamChunk.Done -> {
                                processResponse(chunk.fullContent)
                            }
                            is LLMClient.StreamChunk.Error -> {
                                _events.value = ConversationEvent.Error(chunk.message)
                            }
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // stopped by user
                }
            }
            try {
                streamJob?.join()
            } catch (_: kotlinx.coroutines.CancellationException) {
                streamJob?.cancel()
            }
        } else {
            streamJob = scope.launch(Dispatchers.IO) {
                try {
                    ensureActive()
                    when (val result = llmClient.chat(messages)) {
                        is LLMClient.LLMResult.Success -> {
                            processResponse(result.content)
                        }
                        is LLMClient.LLMResult.Error -> {
                            _events.value = ConversationEvent.Error(result.message)
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // stopped by user
                }
            }
            try {
                streamJob?.join()
            } catch (_: kotlinx.coroutines.CancellationException) {
                streamJob?.cancel()
            }
        }

        isStreaming = false
    }

    private fun processResponse(content: String) {
        val parsed = ResponseParser.parse(content)

        var displayText = parsed.text
        val toolCalls = parsed.toolCalls
        val thoughts = parsed.thoughts

        messages.add(Message(role = "assistant", content = content))

        if (thoughts.isNotEmpty()) {
            _events.value = ConversationEvent.ThinkingEnded("")
        }

        var needsIntervention = false
        var interventionMessage: String? = null

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
                            val pendingCount = _state.value.pendingHabitCount
                            val collectedCount = _state.value.collectedHabitCount

                            if (pendingCount != null && collectedCount < pendingCount) {
                                needsIntervention = true
                                interventionMessage = "用户描述了 $pendingCount 个习惯，已收集 $collectedCount 个，请继续收集剩余的 ${pendingCount - collectedCount} 个习惯。"
                            } else {
                                _events.value = ConversationEvent.ConfirmationRequested(_collectedHabits.value)
                            }
                        }
                    }
                }
                is ToolResult.Error -> {
                    _events.value = ConversationEvent.Error(toolResult.message)
                }
            }
        }

        if (toolCalls.isEmpty() || displayText.isNotEmpty()) {
            _events.value = ConversationEvent.AIMessageReceived(displayText, thoughts, isStreaming = false)
        }

        if (needsIntervention && interventionMessage != null) {
            messages.add(Message(role = "user", content = interventionMessage))
        }

        val guard = ConversationGuard()
        if (guard.shouldStopConversation(_state.value)) {
            _events.value = ConversationEvent.GuardBlocked
            _state.value = _state.value.copy(isStopped = true)
        }
    }

    private fun parseArguments(json: String): Map<String, Any?> {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun continueConversation(userInput: String) {
        if (_state.value.isStopped) return

        messages.add(Message(role = "user", content = userInput))
        sendToLLM()
    }

    fun reset() {
        messages.clear()
        _collectedHabits.value = emptyList()
        _state.value = ConversationState()
        _events.value = null
    }
}
