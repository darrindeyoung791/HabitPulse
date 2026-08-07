package io.github.darrindeyoung791.habitpulse.ai.conversation

import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SessionUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatConversationTest {

    @Test
    fun `session usage accumulates only provided values`() {
        var usage = SessionUsage()
        usage = usage.add(100, 50, 150)
        usage = usage.add(10, 5, 15)
        assertEquals(110L, usage.promptTokens)
        assertEquals(55L, usage.completionTokens)
        assertEquals(165L, usage.totalTokens)
        assertEquals(2, usage.completedCalls)
    }

    @Test
    fun `session usage starts empty`() {
        val usage = SessionUsage()
        assertEquals(0L, usage.totalTokens)
        assertEquals(0, usage.completedCalls)
    }

    @Test
    fun `guard blocks after three invalid setting tries`() {
        val guard = ConversationGuard()
        val check = { guard.check(ConversationState()) }

        assertFalse(check().shouldStop)
        guard.recordInvalidSetting()
        guard.recordInvalidSetting()
        assertFalse(check().shouldStop)
        guard.recordInvalidSetting()
        assertTrue(check().shouldStop)
        assertTrue(check().isBlocked)
    }

    @Test
    fun `update_setting success resets guard counter`() {
        val guard = ConversationGuard()
        guard.recordInvalidSetting()
        guard.recordInvalidSetting()
        guard.recordInvalidSetting()
        assertTrue(guard.check(ConversationState()).shouldStop)

        guard.resetInvalidSettingTries()
        assertFalse(guard.check(ConversationState()).shouldStop)
    }

    @Test
    fun `question limits still enforced`() {
        val guard = ConversationGuard()
        var state = ConversationState()
        repeat(20) {
            state = state.withIncrementedQuestionCount("q$it".takeIf { it.isNotEmpty() })
        }
        assertTrue(guard.check(state).shouldStop)
    }

    @Test
    fun `consecutive same question blocks`() {
        val guard = ConversationGuard()
        var state = ConversationState()
        repeat(4) {
            state = state.withIncrementedQuestionCount("same")
        }
        assertTrue(guard.check(state).shouldStop)
    }

    @Test
    fun `partial habit json serialization`() {
        val habit = io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit(
            title = "读书",
            repeatCycle = "DAILY",
            repeatDays = emptyList(),
            reminderTimes = listOf("08:00"),
            notes = "note"
        )
        val json = habit.toJson()
        assertTrue(json.contains("\"title\":\"读书\""))
        assertTrue(json.contains("\"repeat_cycle\":\"DAILY\""))
        assertTrue(json.contains("\"reminder_times\":[\"08:00\"]"))
    }
}