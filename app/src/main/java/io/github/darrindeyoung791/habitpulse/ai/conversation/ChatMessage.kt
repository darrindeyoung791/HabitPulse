package io.github.darrindeyoung791.habitpulse.ai.conversation

sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class UserMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ChatMessage()

    data class AIMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String,
        val thoughts: String = "",
        val isStreaming: Boolean = false
    ) : ChatMessage()

    data class QuestionMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val questionId: String,
        val type: String,
        val prompt: String,
        val options: List<String> = emptyList(),
        val allowCustomInput: Boolean = false
    ) : ChatMessage()

    data class SystemMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ChatMessage()
}

private fun generateId(): String = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"