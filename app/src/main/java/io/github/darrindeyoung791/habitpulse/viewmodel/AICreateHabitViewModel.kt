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
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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

    private val _completedTempIds = MutableStateFlow<Set<UUID>>(emptySet())
    val completedTempIds: StateFlow<Set<UUID>> = _completedTempIds.asStateFlow()

    private val _retrySignal = MutableStateFlow(0)
    val retrySignal: StateFlow<Int> = _retrySignal.asStateFlow()

    private val _showRetryConfirmation = MutableStateFlow(false)
    val showRetryConfirmation: StateFlow<Boolean> = _showRetryConfirmation.asStateFlow()

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
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showConfirmDialog = true
                        )
                    }
                    is ConversationManager.ConversationEvent.Error -> {
                        addOrUpdateAIMessage(getApplication<HabitPulseApplication>().getString(R.string.ai_error_prefix, event.message))
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
                    ConversationManager.ConversationEvent.Stopped -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        val msgs = _messages.value.toMutableList()
                        if (msgs.isNotEmpty()) {
                            val lastIndex = msgs.lastIndex
                            if (msgs[lastIndex].type == ChatMessageType.AI && msgs[lastIndex].isStreaming) {
                                msgs[lastIndex] = msgs[lastIndex].copy(isStreaming = false)
                                _messages.value = msgs
                            }
                        }
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
            val activeConfig = userPreferences.getActiveAIConfig()

            if (activeConfig == null || !activeConfig.isValid()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = getApplication<HabitPulseApplication>().getString(R.string.ai_error_not_configured)
                )
                return@launch
            }

            addUserMessage(text)
            _uiState.value = _uiState.value.copy(isLoading = true, showClearButton = true)

            if (conversationManager == null) {
                val client = LLMClient(activeConfig.toLLMConfig())
                val prompt = SystemPrompt.getSystemPrompt(getApplication())
                conversationManager = ConversationManager(
                    llmClient = client,
                    toolRegistry = toolRegistry,
                    streamingEnabled = activeConfig.streamingEnabled,
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

    fun retryLastTurn() {
        if (_completedTempIds.value.isNotEmpty() || _collectedHabits.value.isNotEmpty()) {
            _showRetryConfirmation.value = true
        } else {
            executeRetry()
        }
    }

    fun dismissRetryConfirmation() {
        _showRetryConfirmation.value = false
    }

    fun confirmRetry() {
        _showRetryConfirmation.value = false
        executeRetry()
    }

    private fun executeRetry() {
        viewModelScope.launch {
            val msgs = _messages.value.toMutableList()
            val lastUser = msgs.indexOfLast { it.type == ChatMessageType.USER }
            if (lastUser < 0) return@launch

            val prefillText = msgs[lastUser].text

            val removedTempIds = msgs.drop(lastUser)
                .filter { it.type == ChatMessageType.HABIT && it.habit != null }
                .map { it.habit!!.tempId }
                .toSet()

            for (tid in removedTempIds) {
                savedPartialToDbId[tid]?.let { dbId ->
                    repository.getHabitById(dbId)?.let { repository.deleteHabit(it) }
                }
                savedPartialToDbId.remove(tid)
            }

            // Remove all messages from the user's message onwards (including it)
            while (msgs.isNotEmpty() && msgs.lastIndex >= lastUser) {
                msgs.removeAt(msgs.lastIndex)
            }
            _messages.value = msgs

            if (removedTempIds.isNotEmpty()) {
                _collectedHabits.value = _collectedHabits.value.filter { it.tempId !in removedTempIds }
            }

            _completedTempIds.value = _completedTempIds.value - removedTempIds
            _confirmedTempIds.value = _confirmedTempIds.value - removedTempIds
            conversationManager?.removeLastAssistantTurn()
            _pendingQuestion.value = null
            _retrySignal.value = _retrySignal.value + 1
            _uiState.value = _uiState.value.copy(isLoading = false, inputText = prefillText)
        }
    }

    fun clearConversation() {
        cleanupUnconfirmedHabits()
        conversationManager?.reset()
        conversationManager = null
        _messages.value = emptyList()
        _collectedHabits.value = emptyList()
        savedPartialToDbId.clear()
        _pendingQuestion.value = null
        _confirmedTempIds.value = emptySet()
        _completedTempIds.value = emptySet()
        _showRetryConfirmation.value = false
        _retrySignal.value = 0
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
        viewModelScope.launch {
            saveHabitIfNeeded(tempId)
            val newConfirmed = _confirmedTempIds.value + tempId
            _confirmedTempIds.value = newConfirmed
            if (newConfirmed.size >= _collectedHabits.value.size && _collectedHabits.value.isNotEmpty()) {
                confirmAndSaveHabits()
            }
        }
    }

    fun confirmAndSaveHabits() {
        viewModelScope.launch {
            for (habit in _collectedHabits.value) {
                saveHabitIfNeeded(habit.tempId)
            }
            _confirmedTempIds.value = _collectedHabits.value.map { it.tempId }.toSet()
            _uiState.value = _uiState.value.copy(
                showConfirmDialog = false,
                isLoading = true
            )
            conversationManager?.resume()
            conversationManager?.continueConversation("如有剩余习惯等待建立，请继续。若无，与用户道别")
        }
    }

    fun toggleHabitCompleted(tempId: UUID) {
        viewModelScope.launch {
            val current = _completedTempIds.value
            if (tempId in current) {
                val dbId = savedPartialToDbId[tempId] ?: return@launch
                repository.getHabitById(dbId)?.let { repository.deleteHabit(it) }
                savedPartialToDbId.remove(tempId)
                _completedTempIds.value = current - tempId
                _confirmedTempIds.value = _confirmedTempIds.value - tempId
            } else {
                saveHabitIfNeeded(tempId)
                _completedTempIds.value = current + tempId
                _confirmedTempIds.value = _confirmedTempIds.value + tempId
                if (_completedTempIds.value.size >= _collectedHabits.value.size && _collectedHabits.value.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    conversationManager?.resume()
                    conversationManager?.continueConversation("如有剩余习惯等待建立，请继续。若无，与用户道别")
                }
            }
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

    fun saveHabitAndGetId(tempId: UUID, onComplete: (UUID?) -> Unit) {
        viewModelScope.launch {
            if (tempId !in savedPartialToDbId) {
                val habit = _collectedHabits.value.find { it.tempId == tempId }
                if (habit != null) {
                    val dbId = repository.insertHabit(toHabitEntity(habit))
                    savedPartialToDbId[tempId] = dbId
                }
            }
            onComplete(savedPartialToDbId[tempId])
        }
    }

    fun cleanupUnconfirmedHabits() {
        viewModelScope.launch {
            for ((tempId, dbId) in savedPartialToDbId.toList()) {
                if (tempId !in _confirmedTempIds.value) {
                    repository.getHabitById(dbId)?.let { repository.deleteHabit(it) }
                }
            }
        }
    }

    private suspend fun saveHabitIfNeeded(tempId: UUID) {
        if (tempId in savedPartialToDbId) return
        val habit = _collectedHabits.value.find { it.tempId == tempId } ?: return
        val dbId = repository.insertHabit(toHabitEntity(habit))
        savedPartialToDbId[tempId] = dbId
        _completedTempIds.value = _completedTempIds.value + tempId
        _confirmedTempIds.value = _confirmedTempIds.value + tempId
    }

    private fun toHabitEntity(partial: PartialHabit): Habit {
        return Habit(
            title = partial.title,
            repeatCycle = if (partial.repeatCycle == "DAILY") RepeatCycle.DAILY else RepeatCycle.WEEKLY,
            repeatDays = partial.repeatDays.joinToString(",", "[", "]") { it.toString() },
            reminderTimes = partial.reminderTimes.joinToString(",", "[", "]") { "\"${it}\"" },
            notes = partial.notes
        )
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
