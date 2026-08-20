package io.github.darrindeyoung791.habitpulse.ai.llm

data class LLMConfig(
    val apiEndpoint: String,
    val apiKey: String,
    val modelName: String,
    val streamingEnabled: Boolean = true,
    val thinkingEnabled: Boolean = false,
    val timeoutMs: Int = 30000,
    val maxRetries: Int = 3,
    val maxOutputTokens: Int = 5000,
    val maxThinkingTokens: Int = 5000
) {
    fun isValid(): Boolean {
        return apiEndpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }

    /**
     * 思考预算参数。仅当配置了思考预算（[maxThinkingTokens] > 0）且开启深度思考时返回，
     * 否则返回 null 以保持请求体干净（避免不支持该字段的服务端报错）。
     */
    fun thinkingParam(): ThinkingParam? {
        if (!thinkingEnabled || maxThinkingTokens <= 0) return null
        return ThinkingParam(type = "enabled", budgetTokens = maxThinkingTokens)
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
