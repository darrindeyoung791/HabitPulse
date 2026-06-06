package io.github.darrindeyoung791.habitpulse.data.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class SlotCheckInEngineTest {

    // ─── helpers ──────────────────────────────────────────

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun habit(
        repeatCycle: RepeatCycle = RepeatCycle.DAILY,
        repeatDays: List<Int> = emptyList(),
        reminderTimes: List<String> = emptyList()
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
        return HabitCompletion(
            habitId = habitId,
            completedDateLocal = "2026-01-05",
            slotTime = slotTime
        )
    }

    // ─── isApplicableToday ────────────────────────────────

    @Test
    fun `isApplicableToday returns true for daily habit`() {
        val h = habit(repeatCycle = RepeatCycle.DAILY)
        assertTrue(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 5, 0, 0)))
    }

    @Test
    fun `isApplicableToday returns true when today matches repeat day`() {
        // 2026-01-05 is Monday → todayIndex = 0
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(0))
        assertTrue(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 5, 0, 0)))
    }

    @Test
    fun `isApplicableToday returns false when today does not match repeat day`() {
        // 2026-01-05 is Monday → todayIndex = 0, repeatDays = [1] (Tuesday)
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(1))
        assertFalse(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 5, 0, 0)))
    }

    // ─── calculateHabitStatus ─────────────────────────────

    @Test
    fun `calculateHabitStatus returns COMPLETED_TODAY when all slots completed`() {
        val h = habit(reminderTimes = listOf("08:00", "20:00"))
        val completions = listOf(
            completion(h.id, "08:00"),
            completion(h.id, "20:00")
        )
        val status = SlotCheckInEngine.calculateHabitStatus(h, completions, timestamp(2026, 1, 5, 12, 0))
        assertEquals(setOf(HabitStatus.COMPLETED_TODAY), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY only when far before first slot`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 05:00 → 3 hours before slot, outside 1h window
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 5, 0))
        assertEquals(setOf(HabitStatus.PENDING_TODAY), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY plus ABOUT_TO_START within 1h window`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 07:30 → 30 min before slot, within 1h window
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 7, 30))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.ABOUT_TO_START), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY plus ABOUT_TO_START at exact slot time`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 8, 0))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.ABOUT_TO_START), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY plus OVERDUE when past slot plus 1h`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 09:30 → 90 min after slot, past 1h window
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 9, 30))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY at slot plus exactly 1h boundary`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 09:00 → exactly 1h after slot, still within ABOUT_TO_START window
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 9, 0))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.ABOUT_TO_START), status)
    }

    @Test
    fun `calculateHabitStatus returns PENDING_TODAY plus OVERDUE just past 1h boundary`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 09:01 → 1h 1min after slot
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 9, 1))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE), status)
    }

    @Test
    fun `calculateHabitStatus returns NO_STATUS when not applicable today`() {
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(1), reminderTimes = listOf("08:00"))
        // 2026-01-05 is Monday (index=0), repeatDays=[1] (Tuesday)
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 7, 30))
        assertEquals(setOf(HabitStatus.NO_STATUS), status)
    }

    @Test
    fun `calculateHabitStatus returns mixed status with multiple slots`() {
        val h = habit(reminderTimes = listOf("08:00", "20:00"))
        // 09:30 → first slot overdue, second slot far away
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 9, 30))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE), status)
    }

    @Test
    fun `calculateHabitStatus with one slot overdue and one about to start`() {
        val h = habit(reminderTimes = listOf("08:00", "10:00"))
        // 09:30 → first slot (08:00) overdue, second slot (10:00) within 1h
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 9, 30))
        assertEquals(
            setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE, HabitStatus.ABOUT_TO_START),
            status
        )
    }

    @Test
    fun `calculateHabitStatus with some slots completed`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00", "20:00"))
        val completions = listOf(completion(h.id, "08:00"))
        // 13:01 → first slot done, second slot (12:00) overdue (past 1h window), third slot (20:00) far away
        val status = SlotCheckInEngine.calculateHabitStatus(h, completions, timestamp(2026, 1, 5, 13, 1))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE), status)
    }

    // ─── executeSlotCheckIn ───────────────────────────────

    @Test
    fun `executeSlotCheckIn returns NotApplicableToday for wrong day`() {
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(1), reminderTimes = listOf("08:00"))
        // Monday (index=0), but repeatDays=[1] (Tuesday)
        var invoked = false
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 7, 0),
                onPerformCheckIn = { _, _, _ -> invoked = true }
            )
        }
        assertTrue(result is CheckInResult.NotApplicableToday)
        assertFalse(invoked)
    }

    @Test
    fun `executeSlotCheckIn returns AlreadyCompleted when all slots done`() {
        val h = habit(reminderTimes = listOf("08:00"))
        val completions = listOf(completion(h.id, "08:00"))
        var invoked = false
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = completions,
                currentTimeMillis = timestamp(2026, 1, 5, 12, 0),
                onPerformCheckIn = { _, _, _ -> invoked = true }
            )
        }
        assertTrue(result is CheckInResult.AlreadyCompleted)
        assertEquals(1, (result as CheckInResult.AlreadyCompleted).maxCount)
        assertFalse(invoked)
    }

    @Test
    fun `executeSlotCheckIn single slot on time`() {
        val h = habit(reminderTimes = listOf("08:00"))
        var capturedSlot = ""
        var capturedLate = false
        var capturedAllCompleted = false
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 8, 0),
                onPerformCheckIn = { slot, isLate, allCompleted ->
                    capturedSlot = slot
                    capturedLate = isLate
                    capturedAllCompleted = allCompleted
                }
            )
        }
        assertTrue(result is CheckInResult.Success)
        val success = result as CheckInResult.Success
        assertFalse(success.isLate)
        assertTrue(success.isAllCompleted)
        assertEquals("08:00", capturedSlot)
        assertFalse(capturedLate)
        assertTrue(capturedAllCompleted)
    }

    @Test
    fun `executeSlotCheckIn single slot late`() {
        val h = habit(reminderTimes = listOf("08:00"))
        var capturedLate = false
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 10, 0),
                onPerformCheckIn = { _, isLate, _ -> capturedLate = isLate }
            )
        }
        assertTrue(result is CheckInResult.Success)
        val success = result as CheckInResult.Success
        assertTrue(success.isLate)
        assertTrue(capturedLate)
    }

    @Test
    fun `executeSlotCheckIn multiple slots picks first incomplete within window`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00", "20:00"))
        val completions = listOf(completion(h.id, "08:00"))
        var capturedSlot = ""
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = completions,
                currentTimeMillis = timestamp(2026, 1, 5, 12, 30),
                onPerformCheckIn = { slot, _, _ -> capturedSlot = slot }
            )
        }
        assertTrue(result is CheckInResult.Success)
        assertEquals("12:00", capturedSlot)
        assertFalse((result as CheckInResult.Success).isAllCompleted)
    }

    @Test
    fun `executeSlotCheckIn last slot remaining returns isAllCompleted true`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00"))
        val completions = listOf(completion(h.id, "08:00"))
        var capturedAllCompleted = false
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = completions,
                currentTimeMillis = timestamp(2026, 1, 5, 12, 0),
                onPerformCheckIn = { _, _, allCompleted -> capturedAllCompleted = allCompleted }
            )
        }
        assertTrue(result is CheckInResult.Success)
        assertTrue(capturedAllCompleted)
        assertTrue((result as CheckInResult.Success).isAllCompleted)
    }

    @Test
    fun `executeSlotCheckIn returns TooEarly when before all slots window`() {
        val h = habit(reminderTimes = listOf("08:00", "20:00"))
        // 06:00 → both slots' windows start at 07:00, too early
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 6, 0),
                onPerformCheckIn = { _, _, _ -> }
            )
        }
        assertTrue(result is CheckInResult.TooEarly)
        assertEquals("07:00", (result as CheckInResult.TooEarly).earliestSlotTime)
    }

    @Test
    fun `executeSlotCheckIn single slot is never TooEarly because it checks in immediately`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // Single-slot habits always check in immediately regardless of time
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 5, 0),
                onPerformCheckIn = { _, _, _ -> }
            )
        }
        assertTrue(result is CheckInResult.Success)
    }

    @Test
    fun `executeSlotCheckIn multiple slots all completed returns AlreadyCompleted`() {
        val h = habit(reminderTimes = listOf("08:00", "12:00", "20:00"))
        val completions = listOf(
            completion(h.id, "08:00"),
            completion(h.id, "12:00"),
            completion(h.id, "20:00")
        )
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = completions,
                currentTimeMillis = timestamp(2026, 1, 5, 20, 30),
                onPerformCheckIn = { _, _, _ -> }
            )
        }
        assertTrue(result is CheckInResult.AlreadyCompleted)
        assertEquals(3, (result as CheckInResult.AlreadyCompleted).maxCount)
    }

    // ─── Edge cases ───────────────────────────────────────

    @Test
    fun `calculateHabitStatus with empty slots returns PENDING_TODAY for daily habit`() {
        val h = habit() // no reminder times
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 12, 0))
        // No slots → no COMPLETED_TODAY, no OVERDUE, just PENDING_TODAY
        assertEquals(setOf(HabitStatus.PENDING_TODAY), status)
    }

    @Test
    fun `calculateHabitStatus with empty slots returns NO_STATUS for non-applicable weekly`() {
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(1))
        // No slots + not applicable today → NO_STATUS
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 12, 0))
        assertEquals(setOf(HabitStatus.NO_STATUS), status)
    }

    @Test
    fun `executeSlotCheckIn with empty slots returns AlreadyCompleted with count 0`() {
        val h = habit() // no reminder times
        val result = runBlocking {
            SlotCheckInEngine.executeSlotCheckIn(
                habit = h,
                todayCompletions = emptyList(),
                currentTimeMillis = timestamp(2026, 1, 5, 12, 0),
                onPerformCheckIn = { _, _, _ -> fail("should not invoke callback") }
            )
        }
        assertTrue(result is CheckInResult.AlreadyCompleted)
        assertEquals(0, (result as CheckInResult.AlreadyCompleted).maxCount)
    }

    @Test
    fun `calculateHabitStatus at slotTime minus oneHour exactly is ABOUT_TO_START`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 07:00 → exactly 1h before slot, window starts at 07:00
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 7, 0))
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.ABOUT_TO_START), status)
    }

    @Test
    fun `calculateHabitStatus just before slotTime minus oneHour is not ABOUT_TO_START`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // 06:59 → 1h 1min before slot, before window
        val status = SlotCheckInEngine.calculateHabitStatus(h, emptyList(), timestamp(2026, 1, 5, 6, 59))
        assertEquals(setOf(HabitStatus.PENDING_TODAY), status)
    }

    @Test
    fun `calculateHabitStatus ignores old-style completions without slotTime`() {
        val h = habit(reminderTimes = listOf("08:00"))
        // completions without slotTime (from before slot system) should be ignored
        val completions = listOf(completion(h.id, ""))
        val status = SlotCheckInEngine.calculateHabitStatus(h, completions, timestamp(2026, 1, 5, 12, 0))
        // "08:00" is still incomplete because slotTime="" was filtered out
        assertEquals(setOf(HabitStatus.PENDING_TODAY, HabitStatus.OVERDUE), status)
    }

    @Test
    fun `isApplicableToday returns true for weekly with all 7 days`() {
        val h = habit(repeatCycle = RepeatCycle.WEEKLY, repeatDays = listOf(0, 1, 2, 3, 4, 5, 6))
        // Monday
        assertTrue(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 5, 0, 0)))
        // Tuesday
        assertTrue(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 6, 0, 0)))
        // Sunday
        assertTrue(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 11, 0, 0)))
    }

    @Test
    fun `isApplicableToday returns false for weekly with empty repeatDays`() {
        val h = habit(repeatCycle = RepeatCycle.WEEKLY)
        assertFalse(SlotCheckInEngine.isApplicableToday(h, timestamp(2026, 1, 5, 0, 0)))
    }
}
