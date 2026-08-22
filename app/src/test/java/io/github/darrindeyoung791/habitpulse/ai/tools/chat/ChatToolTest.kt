package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatToolTest {

    @Test
    fun `create_habit validates HHmm and dedupes times`() = runBlocking {
        val result = CreateHabitChatTool.execute(
            mapOf(
                "title" to "阅读",
                "repeat_cycle" to "DAILY",
                "reminder_times" to listOf("08:00", "08:00", "20:30"),
                "repeat_days" to emptyList<Int>(),
                "notes" to ""
            )
        )
        assertEquals(ChatToolResult.Success::class, result::class)
        val payload = (result as ChatToolResult.Success).data as CreateHabitPayload
        assertEquals(listOf("08:00", "20:30"), payload.habit.reminderTimes)
        assertTrue(!payload.truncated)
    }

    @Test
    fun `create_habit rejects invalid time`() = runBlocking {
        val result = CreateHabitChatTool.execute(
            mapOf(
                "title" to "跑步",
                "repeat_cycle" to "DAILY",
                "reminder_times" to listOf("25:00"),
            )
        )
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `create_habit weekly without days returns error suggestion`() = runBlocking {
        val result = CreateHabitChatTool.execute(
            mapOf(
                "title" to "中文课",
                "repeat_cycle" to "WEEKLY",
                "reminder_times" to listOf("09:00"),
                "repeat_days" to listOf<Int>()
            )
        )
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `create_habit dedup and range check repeat_days`() = runBlocking {
        val result = CreateHabitChatTool.execute(
            mapOf(
                "title" to "健身",
                "repeat_cycle" to "WEEKLY",
                "reminder_times" to listOf("07:00"),
                "repeat_days" to listOf(1, 1, 3, 9)
            )
        )
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `create_habit truncates long title and notes with flag`() = runBlocking {
        val longTitle = "x".repeat(40)
        val longNotes = "y".repeat(300)
        val result = CreateHabitChatTool.execute(
            mapOf(
                "title" to longTitle,
                "repeat_cycle" to "DAILY",
                "reminder_times" to listOf("08:00"),
                "notes" to longNotes
            )
        )
        val payload = (result as ChatToolResult.Success).data as CreateHabitPayload
        assertEquals(30, payload.habit.title.length)
        assertEquals(200, payload.habit.notes.length)
        assertTrue(payload.truncated)
    }

    @Test
    fun `create_habit validates cycle`() = runBlocking {
        val result = CreateHabitChatTool.execute(mapOf("title" to "x", "repeat_cycle" to "YEARLY"))
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `create_habit requires title`() = runBlocking {
        val result = CreateHabitChatTool.execute(mapOf("repeat_cycle" to "DAILY"))
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `controllable setting byKey resolves whitelist entries`() {
        assertEquals(ControllableSetting.REMINDER_ENABLED, ControllableSetting.byKey("reminder_enabled"))
        assertEquals(ControllableSetting.DND_ENABLED, ControllableSetting.byKey("dnd_enabled"))
        assertEquals(ControllableSetting.byKey("ai_configs"), null)
        assertEquals(ControllableSetting.values().size, 7)
    }

    @Test
    fun `open_settings_page rejects invalid page`() = runBlocking {
        val result = OpenSettingsPageChatTool.execute(mapOf("page" to "hacker_site"))
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `open_settings_page accepts valid pages`() = runBlocking {
        for (page in listOf("notifications", "general", "about", "ai", "home")) {
            val result = OpenSettingsPageChatTool.execute(mapOf("page" to page))
            assertEquals(ChatToolResult.Success::class, result::class)
        }
    }

    @Test
    fun `ask_question validates type and prompt`() = runBlocking {
        val result = AskQuestionChatTool.execute(mapOf("type" to "choice", "prompt" to "选一个", "options" to listOf("a", "b")))
        assertTrue(result is ChatToolResult.Success)
        val data = (result as ChatToolResult.Success).data as io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData
        assertEquals(listOf("a", "b"), data.options)

        val bad = AskQuestionChatTool.execute(mapOf("type" to "weird", "prompt" to "x"))
        assertTrue(bad is ChatToolResult.Error)
    }

    @Test
    fun `reply validates length`() = runBlocking {
        val ok = ReplyChatTool.execute(mapOf("text" to "你好"))
        assertTrue(ok is ChatToolResult.Success)
        val tooLong = ReplyChatTool.execute(mapOf("text" to "x".repeat(600)))
        assertTrue(tooLong is ChatToolResult.Error)
    }
}