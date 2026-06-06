package io.github.darrindeyoung791.habitpulse.data.model

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class HabitTest {

    private fun habit(
        repeatDays: String = "[]",
        reminderTimes: String = "[]",
        supervisorEmails: String = "[]",
        supervisorPhones: String = "[]"
    ): Habit {
        return Habit(
            id = UUID.randomUUID(),
            title = "test",
            repeatDays = repeatDays,
            reminderTimes = reminderTimes,
            supervisorEmails = supervisorEmails,
            supervisorPhones = supervisorPhones
        )
    }

    // ─── getRepeatDaysList ────────────────────────────────

    @Test
    fun `getRepeatDaysList returns empty list for empty JSON`() {
        assertTrue(habit(repeatDays = "[]").getRepeatDaysList().isEmpty())
    }

    @Test
    fun `getRepeatDaysList parses single element`() {
        assertEquals(listOf(0), habit(repeatDays = "[0]").getRepeatDaysList())
    }

    @Test
    fun `getRepeatDaysList parses multiple elements`() {
        assertEquals(listOf(0, 2, 4), habit(repeatDays = "[0,2,4]").getRepeatDaysList())
    }

    @Test
    fun `getRepeatDaysList parses all weekdays`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            habit(repeatDays = "[0,1,2,3,4]").getRepeatDaysList()
        )
    }

    // ─── getReminderTimesList ─────────────────────────────

    @Test
    fun `getReminderTimesList returns empty for empty JSON`() {
        assertTrue(habit(reminderTimes = "[]").getReminderTimesList().isEmpty())
    }

    @Test
    fun `getReminderTimesList parses single time`() {
        assertEquals(listOf("08:00"), habit(reminderTimes = "[\"08:00\"]").getReminderTimesList())
    }

    @Test
    fun `getReminderTimesList parses multiple times`() {
        assertEquals(
            listOf("08:00", "12:00", "20:00"),
            habit(reminderTimes = "[\"08:00\",\"12:00\",\"20:00\"]").getReminderTimesList()
        )
    }

    // ─── getSupervisorEmailsList / getSupervisorPhonesList ─

    @Test
    fun `getSupervisorEmailsList parses emails`() {
        assertEquals(
            listOf("a@b.com", "c@d.com"),
            habit(supervisorEmails = "[\"a@b.com\",\"c@d.com\"]").getSupervisorEmailsList()
        )
    }

    @Test
    fun `getSupervisorPhonesList parses phones`() {
        assertEquals(
            listOf("+8613800000000", "+8613900000000"),
            habit(supervisorPhones = "[\"+8613800000000\",\"+8613900000000\"]").getSupervisorPhonesList()
        )
    }

    // ─── copyWithRepeatDays ───────────────────────────────

    @Test
    fun `copyWithRepeatDays updates repeatDays JSON`() {
        val h = habit()
        val updated = h.copyWithRepeatDays(listOf(1, 3, 5))
        assertEquals(listOf(1, 3, 5), updated.getRepeatDaysList())
    }

    // ─── copyWithReminderTimes ────────────────────────────

    @Test
    fun `copyWithReminderTimes updates reminderTimes JSON`() {
        val h = habit()
        val updated = h.copyWithReminderTimes(listOf("09:00", "21:00"))
        assertEquals(listOf("09:00", "21:00"), updated.getReminderTimesList())
    }

    // ─── copyWithSupervisorEmails / Phones ────────────────

    @Test
    fun `copyWithSupervisorEmails updates emails JSON`() {
        val h = habit()
        val updated = h.copyWithSupervisorEmails(listOf("x@y.com"))
        assertEquals(listOf("x@y.com"), updated.getSupervisorEmailsList())
    }

    @Test
    fun `copyWithSupervisorPhones updates phones JSON`() {
        val h = habit()
        val updated = h.copyWithSupervisorPhones(listOf("+861234567890"))
        assertEquals(listOf("+861234567890"), updated.getSupervisorPhonesList())
    }

    // ─── Edge cases ───────────────────────────────────────

    @Test
    fun `getRepeatDaysList parses all 7 days Sunday through Saturday`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6),
            habit(repeatDays = "[0,1,2,3,4,5,6]").getRepeatDaysList()
        )
    }

    @Test
    fun `getRepeatDaysList preserves duplicate values`() {
        // JSON allows duplicates; production code does not deduplicate
        assertEquals(listOf(0, 0, 1), habit(repeatDays = "[0,0,1]").getRepeatDaysList())
    }

    @Test
    fun `getReminderTimesList handles empty string inside list`() {
        assertEquals(listOf(""), habit(reminderTimes = "[\"\"]").getReminderTimesList())
    }

    // ─── copyWithSortOrder ────────────────────────────────

    @Test
    fun `copyWithSortOrder updates sortOrder`() {
        val h = habit()
        val updated = h.copyWithSortOrder(42)
        assertEquals(42, updated.sortOrder)
    }
}
