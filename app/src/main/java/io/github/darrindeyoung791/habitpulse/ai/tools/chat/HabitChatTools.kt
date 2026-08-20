package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import com.google.gson.Gson
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

/** 按关键字模糊搜索既有习惯 → HabitPickerCard。 */
object SearchHabitsChatTool : ChatTool {
    override val name = "search_habits"

    override fun spec() = functionSpec(
        name = name,
        description = "按关键字模糊查询现有习惯（匹配标题或备注）。命中后返回选择卡片供用户编辑或删除。" +
            "无关键字时返回全部习惯（按自定义排序）。编辑或删除前必须先调用本工具定位习惯。\n" +
            "结果中每个习惯都带 id（主键）与打卡统计（completionCount 总打卡次数、completedToday 今日是否已打卡、" +
            "lastCompletedDate 最近打卡时间戳、createdDate 创建时间戳）。当存在多个同名或相似习惯时，" +
            "依据这些统计区分，例如用户要求「去重并保留记录多的那个」时应选择 completionCount 最大的习惯。",
        properties = mapOf(
            "keyword" to mapOf("type" to "string", "description" to "标题或备注包含的关键字；留空返回全部习惯"),
            "limit" to mapOf("type" to "integer", "description" to "有关键字时最多返回条数，默认 10，最大 20")
        )
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val keyword = arguments["keyword"]?.toString()?.trim().orEmpty()
        val limit = (arguments["limit"] as? Number)?.toInt()?.coerceIn(1, 20) ?: 10

        val repo = arguments["__repo"] as? HabitRepository
            ?: return ChatToolResult.Error("内部错误：缺少存储引用")

        val habits = if (keyword.isEmpty()) {
            repo.habitsBySortOrderFlow.first()
        } else {
            repo.searchHabitsFlow(keyword).first().take(limit)
        }

        val briefs = habits.map { habitToBrief(it) }
        return ChatToolResult.Success(HabitSearchData(briefs))
    }
}

/** 把库中习惯转换为给 AI 与用户选择卡使用的摘要（含主键与打卡统计）。 */
fun habitToBrief(h: Habit): HabitBrief {
    return HabitBrief(
        id = h.id.toString(),
        title = h.title,
        summary = buildHabitSummary(h),
        completionCount = h.completionCount,
        lastCompletedDate = h.lastCompletedDate,
        createdDate = h.createdDate,
        completedToday = h.completedToday
    )
}

private fun buildHabitSummary(h: Habit): String {
    val cycle = if (h.repeatCycle == RepeatCycle.DAILY) {
        "每日"
    } else {
        val days = h.getRepeatDaysList().joinToString("") { dayToChinese(it) }
        "每周${days}"
    }
    val time = h.getReminderTimesList().firstOrNull().orEmpty()
    val base = if (time.isNotEmpty()) "$cycle $time" else cycle
    val checkIn = if (h.completionCount > 0) {
        " 已打卡${h.completionCount}次"
    } else {
        " 未打卡"
    }
    return base + checkIn
}

private fun dayToChinese(day: Int): String = when (day) {
    0 -> "一"
    1 -> "二"
    2 -> "三"
    3 -> "四"
    4 -> "五"
    5 -> "六"
    6 -> "日"
    else -> ""
}

/** 校验 habit_id 存在且 title 与库中一致，返回「前往编辑」卡。 */
object EditHabitChatTool : ChatTool {
    override val name = "edit_habit"

    private val gson = Gson()

    override fun spec() = functionSpec(
        name = name,
        description = "用户要求编辑某个现有习惯时调用。参数为 habit_id 与 habit 的 title（须与库中一致），" +
            "返回一张「前往编辑」卡片，点击进入手动编辑页。编辑前必须先 search_habits 定位。",
        properties = mapOf(
            "habit_id" to mapOf("type" to "string", "description" to "目标习惯的 UUID"),
            "title" to mapOf("type" to "string", "description" to "习惯名称，用于校验回显")
        ),
        required = listOf("habit_id", "title")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val repo = arguments["__repo"] as? HabitRepository
            ?: return ChatToolResult.Error("内部错误：缺少查询仓库")
        val idStr = arguments["habit_id"]?.toString().orEmpty()
        val title = arguments["title"]?.toString()?.trim().orEmpty()

        val habitId = runCatching { UUID.fromString(idStr) }.getOrNull()
            ?: return ChatToolResult.Error("习惯 id 无效")
        val habit = repo.getHabitById(habitId)
            ?: return ChatToolResult.Error("未找到该习惯，可能已被删除，请先 search_habits 确认")
        if (habit.title != title) {
            return ChatToolResult.Error("习惯名称不匹配，请先 search_habits 确认后再编辑")
        }
        return ChatToolResult.Success(HabitEditData(habitId.toString(), habit.title))
    }
}

/** 删除既有习惯，返回删除确认卡（用户确认后才真正删除）。 */
object DeleteHabitChatTool : ChatTool {
    override val name = "delete_habit"

    override fun spec() = functionSpec(
        name = name,
        description = "用户要求删除某个现有习惯时调用，返回删除确认卡。用户确认后才真正删除，AI 不直接删除。" +
            "删除前必须先 search_habits 定位。",
        properties = mapOf(
            "habit_id" to mapOf("type" to "string", "description" to "目标习惯的 UUID"),
            "title" to mapOf("type" to "string", "description" to "习惯名称，用于校验回显")
        ),
        required = listOf("habit_id", "title")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val repo = arguments["__repo"] as? HabitRepository
            ?: return ChatToolResult.Error("内部错误：缺少查询仓库")
        val idStr = arguments["habit_id"]?.toString().orEmpty()
        val title = arguments["title"]?.toString()?.trim().orEmpty()

        val id = runCatching { UUID.fromString(idStr) }.getOrNull()
            ?: return ChatToolResult.Error("习惯 id 无效")
        val habit = repo.getHabitById(id)
            ?: return ChatToolResult.Error("未找到该习惯，可能已被删除，请先 search_habits 确认")
        if (habit.title != title) {
            return ChatToolResult.Error("习惯名称不匹配，请先 search_habits 确认后再删除")
        }
        return ChatToolResult.Success(HabitDeleteData(id.toString(), habit.title))
    }
}