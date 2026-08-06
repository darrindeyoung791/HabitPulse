package io.github.darrindeyoung791.habitpulse.data.model

import androidx.annotation.StringRes
import io.github.darrindeyoung791.habitpulse.R

/**
 * 一个开箱即用的 AI 提供商预置组合。
 *
 * [nameRes] 是本地化显示名；[modelName] 与 [apiEndpoint] 是技术值，不翻译。
 *
 * @param isFree 是否带「免费」角标（当前仅智谱 GLM 预置为 true）。
 */
data class AIPreset(
    val id: String,
    @StringRes val nameRes: Int,
    val modelName: String,
    val apiEndpoint: String,
    val isFree: Boolean = false,
    val defaultStreaming: Boolean = true,
    val defaultThinking: Boolean = false
)

object AIPresets {

    /** 三个预置组合。端点均以 `/chat/completions` 结尾，保证 `ensureChatCompletionsUrl` 原样透传。 */
    val ALL: List<AIPreset> = listOf(
        AIPreset(
            id = "deepseek",
            nameRes = R.string.ai_preset_deepseek,
            modelName = "deepseek-v4-flash",
            apiEndpoint = "https://api.deepseek.com/chat/completions"
        ),
        AIPreset(
            id = "xiaomi_mimo",
            nameRes = R.string.ai_preset_mimo,
            modelName = "mimo-v2.5",
            apiEndpoint = "https://api.xiaomimimo.com/v1/chat/completions"
        ),
        AIPreset(
            id = "glm",
            nameRes = R.string.ai_preset_glm,
            modelName = "glm-4.7-flash",
            apiEndpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            isFree = true
        )
    )
}