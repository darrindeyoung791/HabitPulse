package io.github.darrindeyoung791.habitpulse.ai.tools

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit

class CreateHabitTool : Tool {
    override val name = "create_habit"

    private val gson = Gson()

    override fun execute(arguments: Map<String, Any?>): ToolResult {
        val title = arguments["title"]?.toString()?.trim()
        if (title.isNullOrBlank()) {
            return ToolResult.Error("习惯名称不能为空")
        }

        val repeatCycleStr = arguments["repeat_cycle"]?.toString()?.uppercase()
        val repeatCycle = when (repeatCycleStr) {
            "DAILY", "WEEKLY" -> repeatCycleStr
            else -> return ToolResult.Error("重复周期必须为 DAILY 或 WEEKLY")
        }

        val reminderTimes = parseStringList(arguments["reminder_times"])
        val repeatDays = parseIntList(arguments["repeat_days"])

        if (repeatCycle == "WEEKLY" && repeatDays.isEmpty()) {
            return ToolResult.Error("每周习惯必须指定重复日期")
        }

        val habit = PartialHabit(
            title = title,
            repeatCycle = repeatCycle,
            repeatDays = repeatDays,
            reminderTimes = reminderTimes,
            notes = arguments["notes"]?.toString() ?: ""
        )

        return ToolResult.Success(habit)
    }

    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun parseIntList(value: Any?): List<Int> {
        return when (value) {
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }
            is String -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
}