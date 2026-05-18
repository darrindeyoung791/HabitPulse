package io.github.darrindeyoung791.habitpulse.ai.conversation

data class ConversationState(
    val pendingHabitCount: Int? = null,
    val collectedHabitCount: Int = 0,
    val isHabitRelated: Boolean = true,
    val questionCount: Int = 0,
    val lastQuestionId: String? = null,
    val consecutiveSameQuestionCount: Int = 0,
    val isStopped: Boolean = false
) {
    fun withIncrementedHabitCount(): ConversationState {
        return copy(collectedHabitCount = collectedHabitCount + 1)
    }

    fun withIncrementedQuestionCount(questionId: String?): ConversationState {
        val newConsecutiveCount = if (questionId == lastQuestionId) {
            consecutiveSameQuestionCount + 1
        } else {
            0
        }
        return copy(
            questionCount = questionCount + 1,
            lastQuestionId = questionId,
            consecutiveSameQuestionCount = newConsecutiveCount
        )
    }

    fun withStopped(): ConversationState {
        return copy(isStopped = true)
    }

    fun shouldForceIntervention(): Boolean {
        return pendingHabitCount != null && collectedHabitCount < pendingHabitCount!!
    }

    fun isComplete(): Boolean {
        return pendingHabitCount == null || collectedHabitCount >= pendingHabitCount!!
    }
}