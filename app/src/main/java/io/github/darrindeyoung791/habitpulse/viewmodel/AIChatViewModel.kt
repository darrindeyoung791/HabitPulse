package io.github.darrindeyoung791.habitpulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.conversation.AIChatConversationManager
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.prompt.SystemPrompt
import io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.AskQuestionChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatEvent
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatToolRegistry
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.CreateHabitChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.CreateHabitPayload
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.DeleteHabitChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.EditHabitChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.GetSettingsStatusChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitBrief
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitDeleteData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitEditData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitSearchData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.OpenSettingsPageChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ReplyChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SearchHabitsChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingChangeData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingsNavData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingsStatusData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SessionUsage
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.UpdateSettingChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.habitToBrief
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 新版 AI 对话页的 ViewModel。
 *
 * - 读取全局激活配置（[UserPreferences.activeConfigFlow]），切换配置即重建对话引擎。
 * - 通过 [AIChatConversationManager] 驱动原生 function-calling；把引擎事件
 *   （流式正文 / 工具卡 / 暂停点 / 思考 / 用量）映射为 UI 消息列表。
 * - `create_habit`/`delete_habit` 的确认、`ask_question` 的作答、设置开关的撤销
 *   都先由用户点击卡反馈后，再调用引擎的续跑与仓库落库。
 */
class AIChatViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitPulseApplication
    private val userPreferences = UserPreferences.getInstance(application)
    private val repository = HabitRepository(
        app.database.habitDao(),
        app.database.habitCompletionDao()
    )

    private var conversationManager: AIChatConversationManager? = null

    private val _messages = MutableStateFlow<List<AiChatUiMessage>>(emptyList())
    val messages: StateFlow<List<AiChatUiMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    /** 重试时将用户上次发送的文本回填到输入框（一次性信号，UI 读取后应清零）。 */
    private val _retryInputText = MutableStateFlow<String?>(null)
    val retryInputText: StateFlow<String?> = _retryInputText.asStateFlow()

    /** UI 消费重试信号后调用，清零以避免重复触发。 */
    fun clearRetryInputText() {
        _retryInputText.value = null
    }

    private val _usage = MutableStateFlow(SessionUsage())
    val usage: StateFlow<SessionUsage> = _usage.asStateFlow()

    val configs: Flow<List<io.github.darrindeyoung791.habitpulse.data.model.AIConfig>> =
        userPreferences.aiConfigsFlow
    val activeConfig: Flow<io.github.darrindeyoung791.habitpulse.data.model.AIConfig?> =
        userPreferences.activeConfigFlow

    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    private var currentAssistantMessageId: String? = null

    init {
        viewModelScope.launch {
            _selectedConfigId.value = userPreferences.activeConfigFlow.first().let { active ->
                active?.id ?: userPreferences.aiConfigsFlow.first().firstOrNull()?.id
            }
        }
    }

    private fun observeConversation() {
        viewModelScope.launch {
            conversationManager?.events?.collect { event ->
                when (event) {
                    null -> {}
                    is ChatEvent.AssistantStreaming -> {
                        appendOrUpdateAssistant(event.text, streaming = true)
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = true, showStop = true)
                    }
                    is ChatEvent.AssistantText -> {
                        if (event.text.isNotBlank()) {
                            appendOrUpdateAssistant(event.text, thoughts = event.thoughts, streaming = false)
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false)
                    }
                    is ChatEvent.ToolExecuted -> handleToolExecuted(event.toolName, event.data)
                    is ChatEvent.PauseForUser -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false)
                    }
                    is ChatEvent.ToolError -> {
                        _messages.value = _messages.value + AiChatUiMessage.ToolError(event.toolName, event.message)
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = true, showStop = true)
                    }
                    is ChatEvent.Usage -> _usage.value = event.usage
                    is ChatEvent.Error -> {
                        appendSystemError(event.message)
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false, showRetry = true)
                    }
                    ChatEvent.GuardBlocked -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false, showRetry = true)
                        appendSystemError(getApplication<HabitPulseApplication>().getString(R.string.ai_error_guard_blocked))
                    }
                    ChatEvent.EmptyResponse -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false, showRetry = true)
                        appendSystemError(getApplication<HabitPulseApplication>().getString(R.string.ai_chat_empty_response))
                    }
                    ChatEvent.Stopped -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isGenerating = false, showStop = false, showRetry = true)
                        stopStreamingAssistant()
                    }
                }
            }
        }
    }

    private fun handleToolExecuted(toolName: String, data: Any?) {
        when (toolName) {
            "ask_question" -> {
                val q = data as? PendingQuestionData ?: return
                _messages.value = _messages.value + AiChatUiMessage.QuestionCard(q)
            }
            "create_habit" -> {
                val payload = data as? CreateHabitPayload ?: return
                _messages.value = _messages.value + AiChatUiMessage.CreatedHabitCard(payload)
            }
            "search_habits" -> {
                val d = data as? HabitSearchData ?: return
                _messages.value = _messages.value + AiChatUiMessage.HabitPickerCard(UUID.randomUUID().toString(), d)
            }
            "edit_habit" -> {
                val d = data as? HabitEditData ?: return
                _messages.value = _messages.value + AiChatUiMessage.HabitEditCard(d)
            }
            "delete_habit" -> {
                val d = data as? HabitDeleteData ?: return
                _messages.value = _messages.value + AiChatUiMessage.HabitDeleteCard(d)
            }
            "get_settings_status" -> {
                val d = data as? SettingsStatusData ?: return
                _messages.value = _messages.value + AiChatUiMessage.SettingsStatusCard(d)
            }
            "update_setting" -> {
                val d = data as? SettingChangeData ?: return
                _messages.value = _messages.value + AiChatUiMessage.SettingChangeCard(d)
            }
            "open_settings_page" -> {
                val d = data as? SettingsNavData ?: return
                _messages.value = _messages.value + AiChatUiMessage.SettingsNavCard(d)
            }
            "reply" -> {
                // reply 工具结果由引擎继续驱动下一轮；无卡片。
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val configId = _selectedConfigId.value ?: return
        viewModelScope.launch {
            val config = userPreferences.getAIConfig(configId)
            if (config == null || !config.isValid()) {
                _messages.value = _messages.value + AiChatUiMessage.SystemError(
                    getApplication<HabitPulseApplication>().getString(R.string.ai_error_not_configured)
                )
                return@launch
            }

            _messages.value = _messages.value + AiChatUiMessage.UserBubble(text)
            _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false, isBlocked = false)

            val manager = conversationManager
            if (manager == null) {
                val client = LLMClient(
                    config.toLLMConfig().copy(
                        maxOutputTokens = userPreferences.aiMaxOutputTokensFlow.first(),
                        maxThinkingTokens = userPreferences.aiMaxThinkingTokensFlow.first()
                    )
                )
                val registry = ChatToolRegistry(
                    listOf(
                        CreateHabitChatTool,
                        SearchHabitsChatTool,
                        EditHabitChatTool,
                        DeleteHabitChatTool,
                        GetSettingsStatusChatTool,
                        UpdateSettingChatTool,
                        OpenSettingsPageChatTool,
                        AskQuestionChatTool,
                        ReplyChatTool
                    )
                )
                val newManager = AIChatConversationManager(
                    llmClient = client,
                    toolRegistry = registry,
                    streamingEnabled = config.streamingEnabled,
                    scope = viewModelScope,
                    systemPrompt = SystemPrompt.getChatSystemPrompt(getApplication()),
                    thinkingEnabled = false,
                    maxToolRetries = userPreferences.aiToolRetryLimitFlow.first()
                )
                newManager.setContextDependencies(repository, userPreferences, getApplication())
                conversationManager = newManager
                observeConversation()
                newManager.startConversation(text)
            } else {
                manager.continueConversation(text)
            }
        }
    }

    fun submitAnswer(answer: String) {
        if (answer.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
        viewModelScope.launch {
            conversationManager?.submitAnswer(answer)
        }
    }

    fun confirmCreateHabit(payload: CreateHabitPayload) {
        viewModelScope.launch {
            ensureHabitInserted(payload)
            _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
            conversationManager?.continueConversation(
                "习惯已创建成功（名称=${payload.habit.title}），请继续会话"
            )
        }
    }

    /**
     * 编辑未确认的习惯卡：未确认先自动确认（插入 DB），随后跳转手动编辑页。
     */
    fun editCreateHabit(payload: CreateHabitPayload, onOpenEditor: (UUID) -> Unit) {
        viewModelScope.launch {
            val dbId = ensureHabitInserted(payload)
            onOpenEditor(dbId)
        }
    }

    /**
     * 删除新建习惯卡：已确认的先删除 DB 行，再移除内存卡片。
     */
    fun deleteCreateHabit(payload: CreateHabitPayload) {
        viewModelScope.launch {
            val card = _messages.value.filterIsInstance<AiChatUiMessage.CreatedHabitCard>()
                .firstOrNull { it.payload.habit.tempId == payload.habit.tempId }
            card?.dbId?.let { dbId ->
                runCatching { UUID.fromString(dbId.toString()) }.getOrNull()?.let { id ->
                    repository.getHabitById(id)?.let { repository.deleteHabit(it) }
                }
            }
            _messages.value = _messages.value.filterNot {
                it is AiChatUiMessage.CreatedHabitCard && it.payload.habit.tempId == payload.habit.tempId
            }
        }
    }

    /** 从既有习惯选择卡直接删除一条（含确认前的行内确认）。 */
    fun deleteHabitById(id: String, title: String) {
        viewModelScope.launch {
            runCatching { UUID.fromString(id) }.getOrNull()?.let { uuid ->
                repository.getHabitById(uuid)?.let { repository.deleteHabit(it) }
            }
            _messages.value = _messages.value.map {
                if (it is AiChatUiMessage.HabitPickerCard) {
                    it.copy(data = it.data.copy(habits = it.data.habits.filterNot { brief -> brief.id == id }))
                } else it
            } + AiChatUiMessage.HabitDeletedConfirmed(HabitDeleteData(id, title))
            _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
            conversationManager?.continueConversation("该习惯已删除，请继续会话")
        }
    }

    /**
     * 在既有习惯选择卡内搜索：按关键字查全部习惯（标题/备注模糊匹配），
     * 更新对应卡片的数据；空关键字回到全部习惯。
     */
    fun searchHabitsInPicker(cardId: String, keyword: String) {
        viewModelScope.launch {
            val habits = if (keyword.isBlank()) {
                repository.habitsBySortOrderFlow.first()
            } else {
                repository.searchHabitsFlow(keyword).first()
            }
            val briefs = habits.take(20).map { habitToBrief(it) }
            _messages.value = _messages.value.map { msg ->
                if (msg is AiChatUiMessage.HabitPickerCard && msg.cardId == cardId) {
                    msg.copy(data = msg.data.copy(habits = briefs))
                } else msg
            }
        }
    }

    /**
     * 提交选择卡中选中的习惯给 AI：记录用户已选择，并继续会话。
     */
    fun submitPickerSelection(cardId: String, selected: List<HabitBrief>) {
        if (selected.isEmpty()) return
        val titles = selected.joinToString("、") { it.title }
        val ids = selected.joinToString(",") { it.id }
        _messages.value = _messages.value + AiChatUiMessage.UserBubble(
            getApplication<HabitPulseApplication>().getString(R.string.ai_chat_picker_submitted, titles)
        )
        _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
        viewModelScope.launch {
            conversationManager?.continueConversation("用户已选择以下习惯（id=$ids, 名称=$titles），请基于此继续。")
        }
    }

    /**
     * 用户已手动完成操作：无需额外选择，继续会话。
     */
    fun manualPickerDone(cardId: String) {
        _messages.value = _messages.value + AiChatUiMessage.UserBubble(
            getApplication<HabitPulseApplication>().getString(R.string.ai_chat_picker_manual_done)
        )
        _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
        viewModelScope.launch {
            conversationManager?.continueConversation("用户已手动完成操作。请立即用简短一句话确认已继续（例如：好的，我们可以继续了），不要再询问用户是否继续，也不要等待用户输入，然后直接继续协助用户。")
        }
    }

    /** 插入习惯（若卡片尚未确认）并返回 DB id；已确认则直接返回既有 id。 */
    private suspend fun ensureHabitInserted(payload: CreateHabitPayload): UUID {
        val existing = _messages.value.filterIsInstance<AiChatUiMessage.CreatedHabitCard>()
            .firstOrNull { it.payload.habit.tempId == payload.habit.tempId }
        if (existing?.dbId != null) return existing.dbId

        val habit = toHabitEntity(payload.habit)
        repository.insertHabit(habit)
        _messages.value = _messages.value.map {
            if (it is AiChatUiMessage.CreatedHabitCard && it.payload.habit.tempId == payload.habit.tempId) {
                it.copy(confirmed = true, dbId = habit.id)
            } else it
        }
        return habit.id
    }

    fun confirmDeleteHabit(data: HabitDeleteData) {
        viewModelScope.launch {
            val id = runCatching { UUID.fromString(data.habitId) }.getOrNull()
            if (id != null) {
                repository.getHabitById(id)?.let { repository.deleteHabit(it) }
            }
            _messages.value = _messages.value + AiChatUiMessage.HabitDeletedConfirmed(data)
            _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
            conversationManager?.continueConversation("该习惯已删除，请继续会话")
        }
    }

    fun cancelDeleteHabit(data: HabitDeleteData) {
        _messages.value = _messages.value + AiChatUiMessage.HabitDeleteCancelled(data)
        _uiState.value = _uiState.value.copy(isLoading = true, isGenerating = true, showStop = true, showRetry = false)
        viewModelScope.launch {
            conversationManager?.continueConversation("用户取消了删除，请继续会话")
        }
    }

    fun undoSettingChange(d: SettingChangeData) {
        viewModelScope.launch {
            when (d.key) {
                "reminder_enabled" -> userPreferences.setReminderEnabled(d.oldValue)
                "dnd_enabled" -> userPreferences.setDndEnabled(d.oldValue)
                "persistent_notification" -> userPreferences.setPersistentNotification(d.oldValue)
                "haptic_feedback_enabled" -> userPreferences.setHapticsEnabled(d.oldValue)
                "show_splash_ad" -> userPreferences.setShowSplashAd(d.oldValue)
                "force_tablet_landscape" -> userPreferences.setForceTabletLandscape(d.oldValue)
                "dark_mode" -> userPreferences.setDarkMode(d.oldIntValue ?: 0)
            }
            _messages.value = _messages.value + AiChatUiMessage.SettingReverted(d)
        }
    }

    fun selectConfig(id: String) {
        if (id == _selectedConfigId.value) return
        _selectedConfigId.value = id
        clearConversation()
    }

    fun retryLastTurn() {
        viewModelScope.launch {
            val msgs = _messages.value.toMutableList()
            val lastUserIndex = msgs.indexOfLast { it is AiChatUiMessage.UserBubble }
            if (lastUserIndex < 0) return@launch

            // 提取用户上次发送的文本，用于回填输入框
            val lastUserText = (msgs[lastUserIndex] as AiChatUiMessage.UserBubble).text

            // 移除最后的用户消息及其之后的所有消息（assistant 回复等）
            while (msgs.size > lastUserIndex) msgs.removeAt(msgs.lastIndex)
            _messages.value = msgs

            // 同步清理 conversation manager 内部消息历史：移除最后的用户消息及其 assistant 回复
            conversationManager?.removeLastUserAndAssistantTurn()

            // 回到空闲态：不自动发送，等待用户编辑后手动重发
            currentAssistantMessageId = null
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isGenerating = false,
                showStop = false,
                showRetry = false
            )

            // 发送一次性信号，让 UI 将文本回填到输入框并聚焦
            _retryInputText.value = lastUserText
        }
    }

    fun stopGeneration() {
        viewModelScope.launch { conversationManager?.stop() }
    }

    fun toggleSetting(key: String, newValue: Boolean) {
        viewModelScope.launch {
            when (key) {
                "reminder_enabled" -> userPreferences.setReminderEnabled(newValue)
                "dnd_enabled" -> userPreferences.setDndEnabled(newValue)
                "persistent_notification" -> userPreferences.setPersistentNotification(newValue)
                "haptic_feedback_enabled" -> userPreferences.setHapticsEnabled(newValue)
                "show_splash_ad" -> userPreferences.setShowSplashAd(newValue)
                "force_tablet_landscape" -> userPreferences.setForceTabletLandscape(newValue)
            }
        }
    }

    fun setDarkModeSetting(intValue: Int) {
        viewModelScope.launch { userPreferences.setDarkMode(intValue) }
        // 更新 UI 中对应 SettingChangeCard 的显示值，使下拉菜单跟随用户最后选择
        val msgs = _messages.value.toMutableList()
        val lastSettingCard = msgs.indexOfLast { it is AiChatUiMessage.SettingChangeCard && it.data.key == "dark_mode" }
        if (lastSettingCard >= 0) {
            val card = msgs[lastSettingCard] as AiChatUiMessage.SettingChangeCard
            msgs[lastSettingCard] = card.copy(data = card.data.copy(newIntValue = intValue))
            _messages.value = msgs
        }
    }

    fun clearConversation() {
        conversationManager?.reset()
        conversationManager = null
        _messages.value = emptyList()
        currentAssistantMessageId = null
        _usage.value = SessionUsage()
        _uiState.value = AiChatUiState()
    }

    private suspend fun toHabitEntity(partial: PartialHabit): Habit {
        val now = System.currentTimeMillis()
        val allHabits = repository.getAllHabits()
        val topSort = if (allHabits.isEmpty()) 0 else allHabits.minOf { it.sortOrder } - 1
        return Habit(
            title = partial.title,
            repeatCycle = if (partial.repeatCycle == "DAILY") RepeatCycle.DAILY else RepeatCycle.WEEKLY,
            repeatDays = partial.repeatDays.joinToString(",", "[", "]") { it.toString() },
            reminderTimes = partial.reminderTimes.joinToString(",", "[", "]") { "\"${it}\"" },
            notes = partial.notes,
            createdDate = now,
            modifiedDate = now,
            sortOrder = topSort
        )
    }

    private fun appendOrUpdateAssistant(text: String, thoughts: String = "", streaming: Boolean) {
        val msgs = _messages.value.toMutableList()
        val last = msgs.lastOrNull()
        if (last is AiChatUiMessage.AssistantBubble && last.id == currentAssistantMessageId) {
            msgs[msgs.lastIndex] = last.copy(
                text = text,
                thoughts = thoughts.ifEmpty { last.thoughts },
                isStreaming = streaming
            )
        } else {
            val id = UUID.randomUUID().toString()
            currentAssistantMessageId = id
            msgs += AiChatUiMessage.AssistantBubble(id = id, text = text, thoughts = thoughts, isStreaming = streaming)
        }
        _messages.value = msgs
    }

    private fun stopStreamingAssistant() {
        val msgs = _messages.value.toMutableList()
        val last = msgs.lastOrNull()
        if (last is AiChatUiMessage.AssistantBubble && last.isStreaming) {
            msgs[msgs.lastIndex] = last.copy(isStreaming = false)
            _messages.value = msgs
        }
    }

    private fun appendSystemError(message: String) {
        _messages.value = _messages.value + AiChatUiMessage.SystemError(message)
    }
}

data class AiChatUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val showStop: Boolean = false,
    val showRetry: Boolean = false,
    val isBlocked: Boolean = false,
    val inputText: String = ""
)

/**
 * 新版 AI 对话页的消息列表项：用户气泡 / AI 流式气泡 / 思考块 / 各类工具卡片 / 错误。
 */
sealed class AiChatUiMessage {
    data class UserBubble(val text: String) : AiChatUiMessage()

    data class AssistantBubble(
        val id: String,
        val text: String,
        val thoughts: String = "",
        val isStreaming: Boolean = false
    ) : AiChatUiMessage()

    data class QuestionCard(val question: PendingQuestionData) : AiChatUiMessage()
    data class CreatedHabitCard(
        val payload: CreateHabitPayload,
        val confirmed: Boolean = false,
        val dbId: UUID? = null
    ) : AiChatUiMessage()
    data class HabitPickerCard(val cardId: String, val data: HabitSearchData) : AiChatUiMessage()
    data class HabitEditCard(val data: HabitEditData) : AiChatUiMessage()
    data class HabitDeleteCard(val data: HabitDeleteData) : AiChatUiMessage()
    data class HabitDeletedConfirmed(val data: HabitDeleteData) : AiChatUiMessage()
    data class HabitDeleteCancelled(val data: HabitDeleteData) : AiChatUiMessage()
    data class SettingsStatusCard(val data: SettingsStatusData) : AiChatUiMessage()
    data class SettingChangeCard(val data: SettingChangeData) : AiChatUiMessage()
    data class SettingReverted(val data: SettingChangeData) : AiChatUiMessage()
    data class SettingsNavCard(val data: SettingsNavData) : AiChatUiMessage()
    data class ToolError(val toolName: String, val message: String) : AiChatUiMessage()
    data class SystemError(val message: String) : AiChatUiMessage()
}
