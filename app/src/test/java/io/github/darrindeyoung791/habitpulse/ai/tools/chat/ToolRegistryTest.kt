package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 新版 AI 对话工具注册表：注册完整性 / 分发 / 依赖注入 / spec 生成。 */
class ToolRegistryTest {

    private fun registry() = ChatToolRegistry(
        listOf(
            CreateHabitChatTool,
            SearchHabitsChatTool,
            EditHabitChatTool,
            DeleteHabitChatTool,
            GetSettingsStatusChatTool,
            UpdateSettingChatTool,
            OpenSettingsPageChatTool,
            AskQuestionChatTool,
            ReplyChatTool
        )
    )

    @Test
    fun `registers all nine chat tools`() {
        val names = registry().allToolNames()
        assertEquals(
            setOf(
                "create_habit", "search_habits", "edit_habit", "delete_habit",
                "get_settings_status", "update_setting", "open_settings_page",
                "ask_question", "reply"
            ),
            names
        )
    }

    @Test
    fun `canExecute distinguishes known and unknown tools`() {
        val r = registry()
        assertTrue(r.canExecute("create_habit"))
        assertTrue(r.canExecute("reply"))
        assertFalse(r.canExecute("no_such_tool"))
        assertFalse(r.canExecute(""))
    }

    @Test
    fun `pause tools are registered for manager gating`() {
        val r = registry()
        for (name in listOf("ask_question", "create_habit", "delete_habit")) {
            assertTrue(r.canExecute(name))
        }
    }

    @Test
    fun `toolDefs generate function schema for every tool`() {
        val defs = registry().toolDefs()
        assertEquals(9, defs.size)
        val names = defs.map { it.function.name }.toSet()
        assertEquals(9, names.size)
        for (def in defs) {
            assertTrue(def.function.description.isNotBlank())
            val parameters = def.function.parameters
            assertTrue("parameters schema missing for ${def.function.name}", parameters != null)
            assertEquals("object", parameters!!["type"])
        }
    }

    @Test
    fun `create_habit spec declares required fields`() {
        val parameters = CreateHabitChatTool.spec().function.parameters
        val required = parameters?.get("required") as? List<*>
        assertTrue(required != null)
        assertTrue(required!!.contains("title"))
        assertTrue(required.contains("repeat_cycle"))
    }

    @Test
    fun `create_habit succeeds through registry dispatch`() = runBlocking {
        val result = registry().execute(
            "create_habit",
            mapOf(
                "title" to "阅读",
                "repeat_cycle" to "DAILY",
                "reminder_times" to listOf("08:00", "20:30")
            )
        )
        assertTrue(result is ChatToolResult.Success)
        val payload = (result as ChatToolResult.Success).data as CreateHabitPayload
        assertEquals("阅读", payload.habit.title)
        assertEquals(listOf("08:00", "20:30"), payload.habit.reminderTimes)
    }

    @Test
    fun `registry returns error for unknown tool`() = runBlocking {
        val result = registry().execute("mystery_tool", emptyMap())
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `registry propagates tool validation error`() = runBlocking {
        val result = registry().execute(
            "create_habit",
            mapOf("title" to "x", "repeat_cycle" to "WEEKLY", "reminder_times" to listOf("09:00"))
        )
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `search_habits without repo injection fails safely`() = runBlocking {
        val result = registry().execute("search_habits", mapOf("keyword" to "读书"))
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `update_setting without prefs injection fails safely`() = runBlocking {
        val result = registry().execute(
            "update_setting",
            mapOf("key" to "reminder_enabled", "value" to true)
        )
        assertTrue(result is ChatToolResult.Error)
    }

    @Test
    fun `buildArguments injects repo and prefs only when provided`() {
        val repo = Any()
        val prefs = Any()
        val args = ChatToolRegistry.buildArguments(mapOf("a" to 1), repo = repo, prefs = prefs)
        assertEquals(1, args["a"])
        assertTrue(args["__repo"] === repo)
        assertTrue(args["__prefs"] === prefs)
    }

    @Test
    fun `buildArguments without dependencies leaves injection keys absent`() {
        val args = ChatToolRegistry.buildArguments(mapOf("a" to 1))
        assertFalse(args.containsKey("__repo"))
        assertFalse(args.containsKey("__prefs"))
    }
}
