package io.github.darrindeyoung791.habitpulse.ai.llm

data class LLMConfig(
    val apiEndpoint: String,
    val apiKey: String,
    val modelName: String,
    val streamingEnabled: Boolean = true,
    val timeoutMs: Int = 30000,
    val maxRetries: Int = 3
) {
    fun isValid(): Boolean {
        return apiEndpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }

    fun getChatCompletionsUrl(): String {
        return ensureChatCompletionsUrl(apiEndpoint)
    }

    companion object {
        /**
         * Normalize an API endpoint URL to always point to /chat/completions.
         * Accepts:
         *   - Full URL: https://open.bigmodel.cn/api/paas/v4/chat/completions
         *   - Base v4:  https://open.bigmodel.cn/api/paas/v4
         *   - Base v4/: https://open.bigmodel.cn/api/paas/v4/
         *   - Other:    (returned as-is)
         */
        fun ensureChatCompletionsUrl(endpoint: String): String {
            val trimmed = endpoint.trim().trimEnd('/')
            return when {
                trimmed.endsWith("/chat/completions") -> endpoint.trim()
                trimmed.endsWith("v4") || trimmed.endsWith("v4/") -> "$trimmed/chat/completions"
                trimmed.endsWith("/v4") -> "$trimmed/chat/completions"
                else -> endpoint.trim()
            }
        }
    }
}
