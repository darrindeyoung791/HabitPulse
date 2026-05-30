package io.github.darrindeyoung791.habitpulse.ai.conversation

class ConversationGuard(
    private val maxQuestions: Int = 20,
    private val maxConsecutiveSameQuestion: Int = 3
) {
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

        return GuardResult()
    }

    fun shouldStopConversation(state: ConversationState): Boolean {
        return check(state).shouldStop
    }
}