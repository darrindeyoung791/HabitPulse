package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit

/**
 * 既有习惯的摘要，用于 `search_habits` 的选择卡。
 *
 * 除展示用 [summary] 外，还携带主键与打卡统计，供 AI 区分同名/相似习惯
 * （例如「去重习惯，保留记录多的那个」时，AI 依据 [completionCount] 决策）。
 */
data class HabitBrief(
    val id: String,
    val title: String,
    val summary: String,
    val completionCount: Int = 0,
    val lastCompletedDate: Long? = null,
    val createdDate: Long = 0L,
    val completedToday: Boolean = false
)

data class HabitSearchData(
    val habits: List<HabitBrief>
)

/** `edit_habit` 返回：前往编辑卡（跳到 Route.EditHabit.createRoute(id)）。 */
data class HabitEditData(
    val habitId: String,
    val title: String
)

/** `delete_habit` 返回：删除确认卡。 */
data class HabitDeleteData(
    val habitId: String,
    val title: String
)

/** 一条可控制设置的名称与状态。 */
data class SettingPair(
    val key: String,
    val labelRes: Int,
    val value: Boolean
)

data class SettingsStatusData(
    val pairs: List<SettingPair>
)

/** `update_setting` 返回：变更确认卡（可撤销）。 */
data class SettingChangeData(
    val key: String,
    val labelRes: Int,
    val oldValue: Boolean,
    val newValue: Boolean
)

/** `open_settings_page` 返回：打开设置子页导航卡。 */
data class SettingsNavData(
    val page: String,
    val activityLabel: String
)

/**
 * 新版对话引擎向 UI 输出的事件。UI 将其映射为 [ChatMessageUIItem]（气泡 / 卡片）。
 */
sealed class ChatEvent {
    /** 流式正文增量。 */
    data class AssistantStreaming(val text: String) : ChatEvent()

    /** 完整正文（流式结束或非流式）。 */
    data class AssistantText(val text: String, val thoughts: String = "") : ChatEvent()

    data class ThinkingStarted(val messageId: String) : ChatEvent()
    data class ThinkingUpdated(val thoughts: String) : ChatEvent()
    data class ThinkingEnded(val messageId: String) : ChatEvent()

    /** 工具执行成功，携带结果数据（UI 渲染对应卡片）。 */
    data class ToolExecuted(val toolName: String, val data: Any?) : ChatEvent()

    /** 工具执行失败。 */
    data class ToolError(val toolName: String, val message: String) : ChatEvent()

    /** 到达暂停点（ask_question / create_habit / delete_habit），等待 UI「继续」信号。 */
    data class PauseForUser(val toolName: String) : ChatEvent()

    data class Usage(val usage: SessionUsage) : ChatEvent()

    data class Error(val message: String) : ChatEvent()

    /** 思考超预算 / 模型空回复：多次尝试后仍未产出正文或工具调用。 */
    object EmptyResponse : ChatEvent()

    object GuardBlocked : ChatEvent()

    object Stopped : ChatEvent()
}

/** 会话级 Token 精确统计（仅提供商返回的 usage）。 */
data class SessionUsage(
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val totalTokens: Long = 0L,
    val completedCalls: Int = 0
) {
    fun add(prompt: Int, completion: Int, total: Int): SessionUsage = copy(
        promptTokens = promptTokens + prompt,
        completionTokens = completionTokens + completion,
        totalTokens = totalTokens + total,
        completedCalls = completedCalls + 1
    )
}

/** 新建习惯的内存卡（确认时才插库）。 */
data class CreatedHabitCard(
    val cardId: String,
    val habit: PartialHabit,
    val confirmed: Boolean = false
)
