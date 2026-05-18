package io.github.darrindeyoung791.habitpulse.ai.conversation

class FallbackReply {
    private val helpReplies = mapOf(
        "zh" to "告诉我你想养成的习惯，比如\"每天早上跑步\"或\"每周读一本书\"，我来帮你创建。",
        "en" to "Just tell me about a habit you want to build, like \"run every morning\" or \"read a book every week\" — I'll help you create it."
    )

    private val greetingReplies = mapOf(
        "zh" to "你好！想养成什么习惯？告诉我，比如\"每天早起\"或\"每天喝水\"。",
        "en" to "Hi! What habit do you want to build? Just tell me, like \"wake up early\" or \"drink water daily\"."
    )

    private val redirectReplies = mapOf(
        "zh" to "这个功能帮你创建习惯。告诉我你想坚持的事，比如\"每天跑步\"。",
        "en" to "This feature creates habits. Tell me what you want to do daily, like \"run every morning\"."
    )

    fun generate(userInput: String, language: String = "zh"): String {
        val lower = userInput.lowercase()

        return when {
            lower.contains("怎么用") || lower.contains("help") || lower.contains("how to") ||
            lower.contains("使用") || lower.contains("功能") -> helpReplies[language] ?: helpReplies["zh"]!!

            lower.contains("你好") || lower.contains("hi") || lower.contains("hello") ||
            lower.contains("嗨") || lower.contains("hey") -> greetingReplies[language] ?: greetingReplies["zh"]!!

            else -> redirectReplies[language] ?: redirectReplies["zh"]!!
        }
    }
}