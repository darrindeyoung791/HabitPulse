package io.github.darrindeyoung791.habitpulse.data.model

import com.google.gson.annotations.SerializedName

/**
 * 一条可独立使用 / 编辑 / 删除的 LLM 配置。
 *
 * 以 JSON 数组形式整体存储在 DataStore 的 [io.github.darrindeyoung791.habitpulse.data.preferences.PreferencesKeys.LLM_AI_CONFIGS] 键下，
 * 字段用 [SerializedName] 固定序列化名，避免 R8 混淆后字段名漂移导致数据不可读。
 */
data class AIConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("apiEndpoint")
    val apiEndpoint: String,
    @SerializedName("apiKey")
    val apiKey: String,
    @SerializedName("modelName")
    val modelName: String,
    @SerializedName("streamingEnabled")
    val streamingEnabled: Boolean = true,
    @SerializedName("thinkingEnabled")
    val thinkingEnabled: Boolean = false
) {
    fun isValid(): Boolean {
        return name.isNotBlank() && apiEndpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }

    fun toLLMConfig(): io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig {
        return io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig(
            apiEndpoint = apiEndpoint,
            apiKey = apiKey,
            modelName = modelName,
            streamingEnabled = streamingEnabled,
            thinkingEnabled = thinkingEnabled
        )
    }
}
