package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import java.util.UUID

/** 创建/校验新习惯（补强校验，纯逻辑无 IO）。 */
object CreateHabitChatTool : ChatTool {
    override val name = "create_habit"

    private val timePattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    override fun spec() = functionSpec(
        name = name,
        description = "创建新习惯。先询问用户获取习惯名称、重复周期（DAILY 每日 / WEEKLY 每周）、" +
            "提醒时间（HH:mm）、每周重复日期（仅 WEEKLY）、备注。WEEKLY 必须提供星期，否则应先用 ask_question 澄清。",
        properties = mapOf(
            "title" to mapOf("type" to "string", "description" to "习惯名称，≤30 字符"),
            "repeat_cycle" to mapOf("type" to "string", "enum" to listOf("DAILY", "WEEKLY")),
            "repeat_days" to mapOf("type" to "array", "items" to mapOf("type" to "integer"), "description" to "0=周一..6=周日，仅 WEEKLY"),
            "reminder_times" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "HH:mm 格式提醒时间"),
            "notes" to mapOf("type" to "string", "description" to "备注，≤200 字符")
        ),
        required = listOf("title", "repeat_cycle")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val title = arguments["title"]?.toString()?.trim()
        if (title.isNullOrBlank()) {
            return ChatToolResult.Error("习惯名称不能为空，请先询问用户")
        }
        val cycle = arguments["repeat_cycle"]?.toString()?.uppercase()
        if (cycle != "DAILY" && cycle != "WEEKLY") {
            return ChatToolResult.Error("重复周期必须为 DAILY 或 WEEKLY")
        }

        var reminderTimes = parseStringList(arguments["reminder_times"]).map { it.trim() }.filter { it.isNotEmpty() }
        for (t in reminderTimes) {
            if (!gTimePattern.matches(t)) {
                return ChatToolResult.Error("提醒时间格式错误（应为 HH:mm），请重新提供")
            }
        }
        reminderTimes = reminderTimes.distinct()

        var repeatDays = parseIntList(arguments["repeat_days"]).distinct()
        if (repeatDays.any { it < 0 || it > 6 }) {
            return ChatToolResult.Error("重复日期取值应为 0（周一）到 6（周日）")
        }

        if (cycle == "WEEKLY" && reminderTimes.isNotEmpty() && repeatDays.isEmpty()) {
            return ChatToolResult.Error("每周重复的习惯需要指定重复日期（repeat_days），请询问用户选择星期几")
        }

        var effectiveTitle = title
        var truncated = false
        if (title.length > 30) {
            effectiveTitle = title.take(30)
            truncated = true
        }
        var notes = arguments["notes"]?.toString()?.trim().orEmpty()
        if (notes.length > 200) {
            notes = notes.take(200)
            truncated = true
        }

        if (reminderTimes.isEmpty()) {
            // 允许无固定时间（用户明确不提醒）
        }

        val habit = PartialHabit(
            title = effectiveTitle,
            repeatCycle = cycle,
            repeatDays = repeatDays,
            reminderTimes = reminderTimes,
            notes = notes
        )
        return ChatToolResult.Success(CreateHabitPayload(habit, truncated))
    }

    private interface RawArg {
        fun asStringList(arguments: Map<String, Any?>): List<String>
        fun asIntList(arguments: Map<String, Any?>): List<Int>
    }

    private val gson = Gson()

    private fun parseStringList(value: Any?): List<String> = when (value) {
        is List<*> -> value.mapNotNull { it?.toString() }
        is String -> try {
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        else -> emptyList()
    }

    private fun parseIntList(value: Any?): List<Int> = when (value) {
        is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }
        is String -> try {
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson(value, ArrayList::class.java) as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        else -> emptyList()
    }
}

/** create_habit 的结果负载。 */
data class CreateHabitPayload(
    val habit: PartialHabit,
    val truncated: Boolean = false
)

/** 解析参数用的简单工具（兼容旧 CreateHabitTool）。 */
internal val gTimePattern get() = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")