package io.github.darrindeyoung791.habitpulse.ai.prompt

import android.content.Context
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SystemPrompt {
    private var cachedTemplate: String? = null

    private fun getTemplate(context: Context): String {
        cachedTemplate?.let { return it }
        val template = context.assets.open("prompts/system_prompt.md")
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        cachedTemplate = template
        return template
    }

    fun getSystemPrompt(context: Context): String {
        val template = getTemplate(context)
        val now = LocalDate.now()
        return template
            .replace("{current_date}", now.toString())
            .replace("{current_time}", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
            .replace("{current_day_of_week}", getChineseDayOfWeek(now.dayOfWeek))
    }

    private fun getChineseDayOfWeek(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
