## ADDED Requirements

### Requirement: 系统从用户输入中提取习惯数量
The system SHALL extract the number of habits the user intends to create from their input.

#### Scenario: 用户明确说明数量
- **WHEN** user says "创建两个习惯" or "帮我创建 3 个习惯" or "3 habits please"
- **THEN** system extracts count = 2 (or 3) and sets `pendingHabitCount = N`

#### Scenario: 用户按顺序列举多个习惯
- **WHEN** user describes multiple habits in sequence: "每天跑步、每周读书、每天喝水"
- **THEN** system counts the habit descriptions and sets `pendingHabitCount = 3`

#### Scenario: 用户只描述单个习惯
- **WHEN** user says "提醒我每天早上喝水" or "I want to build a habit"
- **THEN** system sets `pendingHabitCount = null` (unknown, single habit mode)
- **AND** after first `create_habit`, system sets `pendingHabitCount = 1`

#### Scenario: 中途追加习惯
- **WHEN** user mid-conversation says "再加一个习惯" or "and also a habit for reading"
- **THEN** system increments `pendingHabitCount++` (if already set) or sets to the new total

### Requirement: 系统强制完成检测
When AI attempts to show the confirmation dialog before all habits are collected, the system SHALL intervene.

#### Scenario: AI 提前触发确认
- **WHEN** `pendingHabitCount = N > 0` AND AI outputs `confirm` while `collectedHabits.size < N`
- **THEN** system:
  1. Does NOT render the AI's `confirm` as UI
  2. Injects a correction message to the conversation: "用户描述了 N 个习惯，已收集 M 个，请继续收集剩余的 N-M 个习惯。"
  3. Continues conversation with AI

#### Scenario: AI 未调用 create_habit 就输出 confirm
- **WHEN** AI outputs `confirm` without any prior `create_habit` tool call
- **THEN** same intervention as Scenario 1 (treat as premature confirm)

#### Scenario: 所有习惯收集完成
- **WHEN** `collectedHabits.size == pendingHabitCount` AND AI outputs `confirm`
- **THEN** system renders the confirmation UI as normal

#### Scenario: pendingHabitCount = null 且 AI 触发 confirm
- **WHEN** `pendingHabitCount = null` (unknown count mode) AND AI outputs first `confirm`
- **THEN** system sets `pendingHabitCount = 1` and renders confirm as normal
- **AND** if more `create_habit` calls follow after this confirm, system treats them as new habits

### Requirement: 系统分类用户输入为习惯相关/非习惯相关
The system SHALL classify user input to determine if it can generate a habit.

#### Scenario: 分类为习惯相关
- **WHEN** user input contains any habit-related keyword
- **THEN** `isHabitRelated = true`
- **AND** full text is sent to AI for processing

#### Scenario: 分类为非习惯相关
- **WHEN** user input contains no habit-related keywords
- **THEN** `isHabitRelated = false`
- **AND** system invokes fallback reply instead of sending to AI

### Habit-Related Keyword List

**中文关键词** (partial match, case-insensitive):
习惯, 提醒, 每天, 每周, 每天早上, 每天晚上, 每天中午, 运动, 跑步, 走路, 健身, 瑜伽, 喝水, 读书, 阅读, 写作, 写日记, 学习, 练字, 背单词, 早起, 早睡, 冥想, 戒烟, 戒酒, 减肥, 饮食, 饮食控制, 睡眠, 午休, 休息, 计划, 目标, 任务, todo, 打卡, 完成, 坚持, 养成

**英文关键词** (partial match, case-insensitive):
habit, remind, daily, weekly, every day, every week, exercise, run, jog, walk, gym, yoga, water, read, reading, write, journaling, journal, study, learn, meditation,早起, sleep, diet, weight, goal, task, check-in, checkin, complete, finish, build habit, create habit, start habit

---

## Implementation Details

### HabitCountExtractor

```kotlin
class HabitCountExtractor {
    private val explicitCountPatterns = listOf(
        Regex("""(\d+)\s*个\s*习惯"""),
        Regex("""(\d+)\s*habits?"""),
        Regex("""帮我?\s*创建\s*(\d+)\s*个?"""),
        Regex("""create\s*(\d+)\s*habits?"""),
    )

    private val sequentialPatterns = Regex("""[、，,]\s*加上?\s*""")

    fun extract(text: String, detectedLang: String = "auto"): HabitCountResult {
        // 1. Try explicit count patterns
        for (pattern in explicitCountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return HabitCountResult.Explicit(match.groupValues[1].toInt())
            }
        }

        // 2. Try sequential habit description counting
        val habitPhrases = countSequentialHabits(text, detectedLang)
        if (habitPhrases >= 2) {
            return HabitCountResult.Sequential(habitPhrases)
        }

        // 3. Single habit
        return HabitCountResult.Unknown
    }

    fun isHabitRelated(text: String): Boolean {
        val lower = text.lowercase()
        val zhKeywords = listOf("习惯", "提醒", "每天", "每周", "运动", "跑步", "走路", "健身", "瑜伽", "喝水", "读书", "写作", "学习", "早起", "冥想", "睡眠", "减肥", "目标", "打卡", "养成")
        val enKeywords = listOf("habit", "remind", "daily", "weekly", "exercise", "run", "gym", "water", "read", "write", "study", "meditation", "sleep", "goal", "check-in", "create habit", "build habit")
        return zhKeywords.any { lower.contains(it) } || enKeywords.any { lower.contains(it) }
    }
}

sealed class HabitCountResult {
    data class Explicit(val count: Int) : HabitCountResult()
    data class Sequential(val count: Int) : HabitCountResult()
    data object Unknown : HabitCountResult()
}
```

### FallbackReply

```kotlin
class FallbackReply(private val lang: String = "zh") {
    private val helpReplies = mapOf(
        "zh" to "告诉我你想养成的习惯，比如"每天早上跑步"或"每周读一本书"，我来帮你创建。",
        "en" to "Just tell me about a habit you want to build, like "run every morning" or "read a book every week" — I'll help you create it."
    )
    private val greetingReplies = mapOf(
        "zh" to "你好！想养成什么习惯？告诉我，比如"每天早起"或"每天喝水"。",
        "en" to "Hi! What habit do you want to build? Just tell me, like "wake up early" or "drink water daily"."
    )
    private val redirectReplies = mapOf(
        "zh" to "这个功能帮你创建习惯。告诉我你想坚持的事，比如"每天跑步"。",
        "en" to "This feature creates habits. Tell me what you want to do daily, like "run every morning"."
    )

    fun generateHelpReply(): String = helpReplies[lang] ?: helpReplies["zh"]!!
    fun generateGreetingReply(): String = greetingReplies[lang] ?: greetingReplies["zh"]!!
    fun generateRedirectReply(): String = redirectReplies[lang] ?: redirectReplies["zh"]!!
}
```

### ConversationManager 中的强制完成检测

```kotlin
class ConversationManager(
    private val llmClient: LLMClient,
    private val toolRegistry: ToolRegistry,
    private val habitCountExtractor: HabitCountExtractor
) {
    private var pendingHabitCount: Int? = null
    private var collectedHabitCount = 0

    fun startConversation(userInput: String, lang: String) {
        val isHabitRelated = habitCountExtractor.isHabitRelated(userInput)
        if (!isHabitRelated) {
            val fallback = FallbackReply(lang)
            val reply = when {
                userInput.contains("怎么用", "help", "how to") -> fallback.generateHelpReply()
                userInput.contains("你好", "hi", "hello", "嗨") -> fallback.generateGreetingReply()
                else -> fallback.generateRedirectReply()
            }
            emitReply(reply)
            return
        }

        val countResult = habitCountExtractor.extract(userInput, lang)
        pendingHabitCount = when (countResult) {
            is HabitCountResult.Explicit -> countResult.count
            is HabitCountResult.Sequential -> countResult.count
            is HabitCountResult.Unknown -> null
        }

        sendToLLM(userInput)
    }

    fun onToolResult(toolCall: ToolCall): ToolResult? {
        when (toolCall.name) {
            "create_habit" -> {
                collectedHabitCount++
                val partial = parsePartialHabit(toolCall.arguments)
                emitQuestion(partial) // add to UI

                // 检测是否提前触发 confirm
                if (pendingHabitCount != null && collectedHabitCount < pendingHabitCount!!) {
                    // AI 会继续收集，不需要干预
                }
                return ToolResult(toolCall.id, "habit collected")
            }
            // ...
        }
    }

    fun onAIResponse(aiText: String, tools: List<ToolCall>) {
        for (tool in tools) {
            if (tool.name == "confirm") {
                val targetCount = pendingHabitCount ?: 1
                if (collectedHabitCount < targetCount) {
                    // 强制干预：AI 提前触发了 confirm
                    val correction = "用户描述了 $targetCount 个习惯，已收集 $collectedHabitCount 个，请继续收集剩余的 ${targetCount - collectedHabitCount} 个习惯。"
                    injectMessage(correction)
                    return // 不渲染 AI 的 confirm
                }
            }
        }
        // render AI output normally
    }
}
```