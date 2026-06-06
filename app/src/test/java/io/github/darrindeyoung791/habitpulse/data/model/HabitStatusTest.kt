package io.github.darrindeyoung791.habitpulse.data.model

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class HabitStatusTest {

    private fun habit(
        reminderTimes: List<String> = emptyList(),
        repeatDays: List<Int> = emptyList(),
        repeatCycle: RepeatCycle = RepeatCycle.DAILY
    ): Habit {
        val daysJson = repeatDays.joinToString(",", "[", "]") { it.toString() }
        val timesJson = reminderTimes.joinToString(",", "[", "]") { "\"$it\"" }
        return Habit(
            id = UUID.randomUUID(),
            title = "test",
            repeatCycle = repeatCycle,
            repeatDays = daysJson,
            reminderTimes = timesJson
        )
    }

    private fun completion(habitId: UUID, slotTime: String = ""): HabitCompletion {
        return HabitCompletion(habitId = habitId, completedDateLocal = "2026-01-05", slotTime = slotTime)
    }

    @Test
    fun `pendingCount equals reminder count minus completed count`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00", "20:00"))
        val completions = listOf(completion(h.id, "08:00"))
        val ws = HabitWithStatus(h, completions, emptySet())
        assertEquals(2, ws.pendingCount)
    }

    @Test
    fun `pendingCount is zero when all slots completed`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val completions = listOf(completion(h.id, "08:00"))
        val ws = HabitWithStatus(h, completions, emptySet())
        assertEquals(0, ws.pendingCount)
    }

    @Test
    fun `pendingCount equals reminder count when no completions`() {
        val h = habit(reminderTimes = listOf("08:00", "20:00"))
        val ws = HabitWithStatus(h, emptyList(), emptySet())
        assertEquals(2, ws.pendingCount)
    }

    @Test
    fun `pendingCount is zero for habit with no reminders`() {
        val h = habit()
        val ws = HabitWithStatus(h, emptyList(), emptySet())
        assertEquals(0, ws.pendingCount)
    }

    @Test
    fun `isCompletelyOverdue is true when OVERDUE and not ABOUT_TO_START`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE))
        assertTrue(ws.isCompletelyOverdue)
    }

    @Test
    fun `isCompletelyOverdue is false when only ABOUT_TO_START`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.PENDING_TODAY, HabitStatus.ABOUT_TO_START))
        assertFalse(ws.isCompletelyOverdue)
    }

    @Test
    fun `isCompletelyOverdue is false when both OVERDUE and ABOUT_TO_START`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00"))
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE, HabitStatus.ABOUT_TO_START))
        assertFalse(ws.isCompletelyOverdue)
    }

    @Test
    fun `isCompletelyOverdue is false when COMPLETED_TODAY`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.COMPLETED_TODAY))
        assertFalse(ws.isCompletelyOverdue)
    }

    @Test
    fun `isCompletelyOverdue is false when NO_STATUS`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.NO_STATUS))
        assertFalse(ws.isCompletelyOverdue)
    }

    // ─── Edge cases ───────────────────────────────────────

    @Test
    fun `pendingCount can be negative when completions exceed reminders`() {
        // Possible when reminders are deleted after completions exist
        val h = habit(reminderTimes = listOf("08:00"))
        val completions = listOf(
            completion(h.id, "08:00"),
            completion(h.id, "08:00") // duplicate completion
        )
        val ws = HabitWithStatus(h, completions, emptySet())
        assertEquals(-1, ws.pendingCount)
    }

    @Test
    fun `pendingCount includes completions without slotTime`() {
        // Old-style completions (before slot system) have slotTime=""
        // but pendingCount counts ALL todayCompletions, not just slotted ones
        val h = habit(reminderTimes = listOf("08:00"))
        val completions = listOf(
            completion(h.id, "")        // old-style, no slotTime
        )
        val ws = HabitWithStatus(h, completions, emptySet())
        assertEquals(0, ws.pendingCount) // 1 reminder - 1 completion
    }

    @Test
    fun `isCompletelyOverdue is false when PENDING_TODAY only`() {
        val h = habit()
        val ws = HabitWithStatus(h, emptyList(), setOf(HabitStatus.PENDING_TODAY))
        assertFalse(ws.isCompletelyOverdue)
    }
}
