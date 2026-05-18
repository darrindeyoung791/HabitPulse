package io.github.darrindeyoung791.habitpulse.ai.conversation

class HabitCountExtractor {
    private val explicitCountPatterns = listOf(
        Regex("""(\d+)\s*个\s*习惯"""),
        Regex("""(\d+)\s*habits?"""),
        Regex("""帮我?\s*创建\s*(\d+)\s*个?"""),
        Regex("""create\s*(\d+)\s*habits?"""),
        Regex("""(\d+)\s*个\s*每日"""),
        Regex("""(\d+)\s*个\s*每周""")
    )

    sealed class HabitCountResult {
        data class Explicit(val count: Int) : HabitCountResult()
        data class Sequential(val count: Int) : HabitCountResult()
        data object Unknown : HabitCountResult()
    }

    fun extract(text: String, language: String = "auto"): HabitCountResult {
        for (pattern in explicitCountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val countStr: String = match.groupValues[1]
                return HabitCountResult.Explicit(countStr.toInt())
            }
        }

        val sequentialCount = countSequentialHabits(text, language)
        if (sequentialCount >= 2) {
            return HabitCountResult.Sequential(sequentialCount)
        }

        return HabitCountResult.Unknown
    }

    private fun countSequentialHabits(text: String, language: String): Int {
        val delimiters: List<String> = if (language.startsWith("en")) {
            listOf(", ", " and ", "、", "，", "。", ". ", "\n")
        } else {
            listOf("、", "，", "。", "\n", ", ")
        }

        var count = 0
        var remaining = text

        for (delimiter in delimiters) {
            val parts = remaining.split(delimiter)
            if (parts.size > count) {
                count = parts.size
                remaining = parts.last()
            }
        }

        val habitIndicators: List<String> = if (language.startsWith("en")) {
            listOf("habit", "daily", "weekly", "every day", "every week", "run", "read", "exercise", "workout", "drink", "walk")
        } else {
            listOf("习惯", "每天", "每周", "跑步", "读书", "运动", "喝水", "健身", "冥想", "早睡", "早起", "学习", "练字", "背单词")
        }

        var indicatorCount = 0
        for (indicator in habitIndicators) {
            if (remaining.contains(indicator, ignoreCase = true)) {
                indicatorCount++
            }
        }

        return if (indicatorCount > 0) indicatorCount else count
    }

    fun isHabitRelated(text: String): Boolean {
        val lower = text.lowercase()

        val zhKeywords = listOf(
            "习惯", "提醒", "每天", "每周", "运动", "跑步", "走路", "健身", "瑜伽",
            "喝水", "读书", "写作", "学习", "早起", "冥想", "睡眠", "减肥",
            "目标", "打卡", "养成", "坚持", "重复", "完成", "todo", "task"
        )

        val enKeywords = listOf(
            "habit", "remind", "daily", "weekly", "exercise", "run", "gym",
            "water", "read", "write", "study", "meditation", "sleep", "goal",
            "check-in", "create habit", "build habit", "workout", "walk"
        )

        return zhKeywords.any { lower.contains(it) } || enKeywords.any { lower.contains(it) }
    }
}