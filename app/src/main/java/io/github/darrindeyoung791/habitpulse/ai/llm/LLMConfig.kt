package io.github.darrindeyoung791.habitpulse.ai.llm

data class LLMConfig(
    val apiEndpoint: String,
    val apiKey: String,
    val modelName: String,
    val timeoutMs: Int = 30000,
    val maxRetries: Int = 3
) {
    fun isValid(): Boolean {
        return apiEndpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }
}