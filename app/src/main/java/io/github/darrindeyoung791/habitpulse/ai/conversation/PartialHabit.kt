package io.github.darrindeyoung791.habitpulse.ai.conversation

import java.util.UUID

data class PartialHabit(
    val tempId: UUID = UUID.randomUUID(),
    val title: String,
    val repeatCycle: String,
    val repeatDays: List<Int> = emptyList(),
    val reminderTimes: List<String> = emptyList(),
    val notes: String = ""
) {
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"title\":\"${escapeJson(title)}\",")
        sb.append("\"repeat_cycle\":\"$repeatCycle\"")
        if (repeatDays.isNotEmpty()) {
            sb.append(",\"repeat_days\":[${repeatDays.joinToString(",")}]")
        }
        if (reminderTimes.isNotEmpty()) {
            sb.append(",\"reminder_times\":[${reminderTimes.joinToString(",") { "\"${escapeJson(it)}\"" }}]")
        }
        if (notes.isNotEmpty()) {
            sb.append(",\"notes\":\"${escapeJson(notes)}\"")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun toSummaryString(): String {
        val cycle = when (repeatCycle) {
            "DAILY" -> "每日"
            "WEEKLY" -> "每周${repeatDays.joinToString("") { dayToChinese(it) }}"
            else -> repeatCycle
        }
        val time = reminderTimes.firstOrNull() ?: ""
        return "$cycle${if (time.isNotEmpty()) " $time" else ""} $title"
    }

    private fun dayToChinese(day: Int): String {
        return when (day) {
            0 -> "日"
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            else -> ""
        }
    }
}