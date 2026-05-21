package io.github.darrindeyoung791.habitpulse.ai.prompt

import android.content.Context
import java.nio.charset.StandardCharsets

object SystemPrompt {
    private var cachedPrompt: String? = null

    fun getSystemPrompt(context: Context): String {
        cachedPrompt?.let { return it }
        val prompt = context.assets.open("prompts/system_prompt.md")
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        cachedPrompt = prompt
        return prompt
    }
}
