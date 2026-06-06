package io.github.darrindeyoung791.habitpulse.data.model

import org.junit.Assert.*
import org.junit.Test

class HabitCompletionTest {

    @Test
    fun `getTodayDate returns yyyy-MM-dd format`() {
        val date = HabitCompletion.getTodayDate()
        assertTrue("should match yyyy-MM-dd", date.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun `getTodayDate month and day are zero-padded`() {
        // We can't control the system clock in a plain JUnit test,
        // but we can verify the format invariants.
        val parts = HabitCompletion.getTodayDate().split("-")
        assertEquals(3, parts.size)
        assertEquals(4, parts[0].length) // year
        assertEquals(2, parts[1].length) // month, zero-padded
        assertEquals(2, parts[2].length) // day, zero-padded
    }

    @Test
    fun `getFormattedDate returns completedDateLocal`() {
        val c = HabitCompletion(habitId = java.util.UUID.randomUUID(), completedDateLocal = "2026-03-26")
        assertEquals("2026-03-26", c.getFormattedDate())
    }

    @Test
    fun `new completion has empty slotTime by default`() {
        val c = HabitCompletion(habitId = java.util.UUID.randomUUID(), completedDateLocal = "2026-01-05")
        assertEquals("", c.slotTime)
    }

    @Test
    fun `new completion is not late by default`() {
        val c = HabitCompletion(habitId = java.util.UUID.randomUUID(), completedDateLocal = "2026-01-05")
        assertFalse(c.isLate)
    }
}
