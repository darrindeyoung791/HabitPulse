package io.github.darrindeyoung791.habitpulse.ai.tools

data class PendingQuestionData(
    val questionId: String,
    val type: String,
    val prompt: String,
    val options: List<String> = emptyList(),
    val allowCustomInput: Boolean = false
)