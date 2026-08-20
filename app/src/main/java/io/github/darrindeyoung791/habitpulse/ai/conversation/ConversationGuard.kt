package io.github.darrindeyoung791.habitpulse.ai.conversation

class ConversationGuard(
    private val maxQuestions: Int = 20,
    private val maxConsecutiveSameQuestion: Int = 3,
    private val maxInvalidSettingTries: Int = 3
) {
    private var invalidSettingTries = 0

    data class GuardResult(
        val isBlocked: Boolean = false,
        val reason: String? = null,
        val shouldStop: Boolean = false
    )

    fun check(state: ConversationState): GuardResult {
        if (state.questionCount >= maxQuestions) {
            return GuardResult(
                isBlocked = true,
                reason = "已达到最大问题数量限制($maxQuestions)",
                shouldStop = true
            )
        }

        if (state.consecutiveSameQuestionCount >= maxConsecutiveSameQuestion) {
            return GuardResult(
                isBlocked = true,
                reason = "检测到重复提问，已自动停止",
                shouldStop = true
            )
        }

        if (invalidSettingTries >= maxInvalidSettingTries) {
            return GuardResult(
                isBlocked = true,
                reason = "涉及设置的操作请检查后重试",
                shouldStop = true
            )
        }

        return GuardResult()
    }

    /** 记录一次设置类工具（update_setting / open_settings_page）的报错。 */
    fun recordInvalidSetting() {
        invalidSettingTries++
    }

    /** update_setting 成功后重置计数。 */
    fun resetInvalidSettingTries() {
        invalidSettingTries = 0
    }

    fun shouldStopConversation(state: ConversationState): Boolean {
        return check(state).shouldStop
    }
}