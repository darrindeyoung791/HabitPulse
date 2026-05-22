package io.github.darrindeyoung791.habitpulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.ai.conversation.*
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.prompt.SystemPrompt
import io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData
import io.github.darrindeyoung791.habitpulse.ai.tools.ToolRegistry
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

object AIPrefillHabitHolder {
    var prefillHabit: PartialHabit? = null
    var editingHabitDbId: UUID? = null
}

private typealias TempId = UUID
private typealias DbId = UUID

class AICreateHabitViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences.getInstance(application)
    private val app = application as HabitPulseApplication
    private val repository = HabitRepository(
        app.database.habitDao(),
        app.database.habitCompletionDao()
    )

    private val toolRegistry = ToolRegistry()

    private var conversationManager: ConversationManager? = null

    private val _uiState = MutableStateFlow(AICreateHabitUIState())
    val uiState: StateFlow<AICreateHabitUIState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageUIItem>>(emptyList())
    val messages: StateFlow<List<ChatMessageUIItem>> = _messages.asStateFlow()

    private val _collectedHabits = MutableStateFlow<List<PartialHabit>>(emptyList())
    val collectedHabits: StateFlow<List<PartialHabit>> = _collectedHabits.asStateFlow()

    private val savedPartialToDbId = mutableMapOf<TempId, DbId>()

    private val _pendingQuestion = MutableStateFlow<PendingQuestionUI?>(null)
    val pendingQuestion: StateFlow<PendingQuestionUI?> = _pendingQuestion.asStateFlow()

    private val _confirmedTempIds = MutableStateFlow<Set<UUID>>(emptySet())
    val confirmedTempIds: StateFlow<Set<UUID>> = _confirmedTempIds.asStateFlow()

    private fun observeConversation() {
        viewModelScope.launch {
            conversationManager?.events?.collect { event ->
                when (event) {
                    is ConversationManager.ConversationEvent.AIMessageReceived -> {
                        addOrUpdateAIMessage(event.text, event.thoughts, event.isStreaming)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showClearButton = true
                        )
                    }
                    is ConversationManager.ConversationEvent.QuestionReceived -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        val q = event.question
                        _pendingQuestion.value = PendingQuestionUI(
                            questionId = q.questionId,
                            type = q.type,
                            prompt = q.prompt,
                            options = q.options,
                            allowCustomInput = q.allowCustomInput
                        )
                    }
                    is ConversationManager.ConversationEvent.ReplyReceived -> {
                        addOrUpdateAIMessage(event.text)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is ConversationManager.ConversationEvent.HabitCreated -> {
                        val partialHabit = event.habit
                        try {
                            val newHabit = Habit(
                                title = partialHabit.title,
                                repeatCycle = if (partialHabit.repeatCycle == "DAILY") RepeatCycle.DAILY else RepeatCycle.WEEKLY,
                                repeatDays = partialHabit.repeatDays.joinToString(",", "[", "]") { it.toString() },
                                reminderTimes = partialHabit.reminderTimes.joinToString(",", "[\"", "\"]") { it },
                                notes = partialHabit.notes
                            )
                            val dbId = repository.insertHabit(newHabit)
                            savedPartialToDbId[partialHabit.tempId] = dbId
                        } catch (_: Exception) { }
                        _collectedHabits.value = _collectedHabits.value + partialHabit
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _messages.value = _messages.value + ChatMessageUIItem(
                            id = UUID.randomUUID().toString(),
                            type = ChatMessageType.HABIT,
                            text = "",
                            habit = partialHabit
                        )
                    }
                    is ConversationManager.ConversationEvent.ConfirmationRequested -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        confirmAndSaveHabits()
                    }
                    is ConversationManager.ConversationEvent.Error -> {
                        addOrUpdateAIMessage("错误: ${event.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = event.message
                        )
                    }
                    ConversationManager.ConversationEvent.GuardBlocked -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isStopped = true
                        )
                    }
                    is ConversationManager.ConversationEvent.ThinkingStarted -> {}
                    is ConversationManager.ConversationEvent.ThinkingUpdated -> {
                        updateLastMessageThoughts(event.thoughts)
                    }
                    is ConversationManager.ConversationEvent.ThinkingEnded -> {}
                    null -> {}
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val endpoint = userPreferences.llmApiEndpointFlow.first()
            val apiKey = userPreferences.llmApiKeyFlow.first()
            val modelName = userPreferences.llmModelNameFlow.first()
            val streamingEnabled = userPreferences.llmStreamingResponseFlow.first()

            if (endpoint.isBlank() || apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "请先在设置中配置 API"
                )
                return@launch
            }

            addUserMessage(text)
            _uiState.value = _uiState.value.copy(isLoading = true, showClearButton = true)

            if (conversationManager == null) {
                val client = LLMClient.fromPreferences(endpoint, apiKey, modelName, streamingEnabled)
                val prompt = SystemPrompt.getSystemPrompt(getApplication())
                conversationManager = ConversationManager(
                    llmClient = client,
                    toolRegistry = toolRegistry,
                    streamingEnabled = streamingEnabled,
                    scope = viewModelScope,
                    systemPrompt = prompt
                )
                observeConversation()
                conversationManager?.startConversation(text)
            } else {
                conversationManager?.continueConversation(text)
            }
        }
    }

    fun submitAnswer(answer: String) {
        val question = _pendingQuestion.value ?: return

        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessageUIItem(
                id = UUID.randomUUID().toString(),
                type = ChatMessageType.QUESTION,
                text = question.prompt,
                answer = answer
            )
            _pendingQuestion.value = null
            _uiState.value = _uiState.value.copy(isLoading = true)

            conversationManager?.submitAnswer(answer, question.questionId)
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            conversationManager?.stop()
        }
    }

    fun clearConversation() {
        conversationManager?.reset()
        conversationManager = null
        _messages.value = emptyList()
        _collectedHabits.value = emptyList()
        savedPartialToDbId.clear()
        _pendingQuestion.value = null
        _confirmedTempIds.value = emptySet()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showClearButton = false,
            isStopped = false
        )
    }

    fun showExitConfirmation() {
        _uiState.value = _uiState.value.copy(showExitConfirmation = true)
    }

    fun dismissExitConfirmation() {
        _uiState.value = _uiState.value.copy(showExitConfirmation = false)
    }

    fun showClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = true)
    }

    fun dismissClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = false)
    }

    fun showSettingsConfirmation() {
        _uiState.value = _uiState.value.copy(showSettingsConfirmation = true)
    }

    fun dismissSettingsConfirmation() {
        _uiState.value = _uiState.value.copy(showSettingsConfirmation = false)
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun confirmHabit(tempId: UUID) {
        val newConfirmed = _confirmedTempIds.value + tempId
        _confirmedTempIds.value = newConfirmed
        if (newConfirmed.size >= _collectedHabits.value.size && _collectedHabits.value.isNotEmpty()) {
            confirmAndSaveHabits()
        }
    }

    fun confirmAndSaveHabits() {
        viewModelScope.launch {
            val habitsToSave = _collectedHabits.value
            for (habit in habitsToSave) {
                if (habit.tempId in savedPartialToDbId) continue
                val newHabit = Habit(
                    title = habit.title,
                    repeatCycle = if (habit.repeatCycle == "DAILY") RepeatCycle.DAILY else RepeatCycle.WEEKLY,
                    repeatDays = habit.repeatDays.joinToString(",", "[", "]") { it.toString() },
                    reminderTimes = habit.reminderTimes.joinToString(",", "[\"", "\"]") { it },
                    notes = habit.notes
                )
                val dbId = repository.insertHabit(newHabit)
                savedPartialToDbId[habit.tempId] = dbId
            }
            _uiState.value = _uiState.value.copy(
                showConfirmDialog = false
            )
        }
    }

    fun getDbIdForTempId(tempId: UUID): UUID? = savedPartialToDbId[tempId]

    fun deleteHabitByTempId(tempId: UUID) {
        val dbId = savedPartialToDbId[tempId] ?: return
        viewModelScope.launch {
            repository.getHabitById(dbId)?.let { repository.deleteHabit(it) }
            savedPartialToDbId.remove(tempId)
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessageUIItem(
            id = UUID.randomUUID().toString(),
            type = ChatMessageType.USER,
            text = text
        )
    }

    private fun addOrUpdateAIMessage(text: String, thoughts: String = "", isStreaming: Boolean = false) {
        val currentMessages = _messages.value.toMutableList()
        val lastMsg = if (currentMessages.isNotEmpty()) currentMessages.last() else null

        if (lastMsg != null && lastMsg.type == ChatMessageType.AI && lastMsg.isStreaming) {
            currentMessages[currentMessages.lastIndex] = lastMsg.copy(
                text = text,
                thoughts = thoughts,
                isStreaming = isStreaming
            )
        } else {
            currentMessages.add(
                ChatMessageUIItem(
                    id = UUID.randomUUID().toString(),
                    type = ChatMessageType.AI,
                    text = text,
                    thoughts = thoughts,
                    isStreaming = isStreaming
                )
            )
        }
        _messages.value = currentMessages
    }

    private fun updateLastMessageThoughts(thoughts: String) {
        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.isNotEmpty() && currentMessages.last().type == ChatMessageType.AI) {
            val lastIndex = currentMessages.lastIndex
            currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                thoughts = thoughts,
                isStreaming = true
            )
            _messages.value = currentMessages
        }
    }
}

data class AICreateHabitUIState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStopped: Boolean = false,
    val errorMessage: String? = null,
    val showExitConfirmation: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val showSettingsConfirmation: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showClearButton: Boolean = false,
    val habitsSaved: Boolean = false
)

data class ChatMessageUIItem(
    val id: String,
    val type: ChatMessageType,
    val text: String,
    val thoughts: String = "",
    val isStreaming: Boolean = false,
    val habit: PartialHabit? = null,
    val answer: String = ""
)

enum class ChatMessageType {
    USER, AI, QUESTION, HABIT
}

data class PendingQuestionUI(
    val questionId: String,
    val type: String,
    val prompt: String,
    val options: List<String> = emptyList(),
    val allowCustomInput: Boolean = false
)
