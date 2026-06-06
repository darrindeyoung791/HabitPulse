package io.github.darrindeyoung791.habitpulse.data.model

import java.util.Calendar

sealed class CheckInResult {
    data class Success(val isLate: Boolean, val isAllCompleted: Boolean) : CheckInResult()
    data class AlreadyCompleted(val maxCount: Int) : CheckInResult()
    data class TooEarly(val earliestSlotTime: String) : CheckInResult()
    data object NotApplicableToday : CheckInResult()
}

object SlotCheckInEngine {

    fun calculateHabitStatus(
        habit: Habit,
        todayCompletions: List<HabitCompletion>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Set<HabitStatus> {
        val allSlots = habit.getReminderTimesList()
        val completedSlotTimes = todayCompletions
            .filter { it.slotTime.isNotEmpty() }
            .map { it.slotTime }
            .toSet()
        val incompleteSlots = allSlots.filter { it !in completedSlotTimes }

        if (incompleteSlots.isEmpty() && allSlots.isNotEmpty()) {
            return setOf(HabitStatus.COMPLETED_TODAY)
        }

        if (!isApplicableToday(habit, currentTimeMillis)) {
            return setOf(HabitStatus.NO_STATUS)
        }

        val status = mutableSetOf(HabitStatus.PENDING_TODAY)
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        var hasOverdue = false
        var hasAboutToStart = false

        for (slot in incompleteSlots) {
            val parts = slot.split(":")
            if (parts.size == 2) {
                val slotCal = Calendar.getInstance().apply {
                    timeInMillis = currentTimeMillis
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val slotTime = slotCal.timeInMillis
                val oneHour = 60 * 60 * 1000L
                if (now.timeInMillis >= slotTime - oneHour && now.timeInMillis <= slotTime + oneHour) {
                    hasAboutToStart = true
                }
                if (now.timeInMillis > slotTime + oneHour) {
                    hasOverdue = true
                }
            }
        }

        if (hasOverdue) status.add(HabitStatus.OVERDUE)
        if (hasAboutToStart) status.add(HabitStatus.ABOUT_TO_START)

        return status
    }

    fun isApplicableToday(habit: Habit, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (habit.repeatCycle == RepeatCycle.DAILY) return true
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val todayIndex = (cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        return todayIndex in habit.getRepeatDaysList()
    }

    suspend fun executeSlotCheckIn(
        habit: Habit,
        todayCompletions: List<HabitCompletion>,
        currentTimeMillis: Long = System.currentTimeMillis(),
        onPerformCheckIn: suspend (slotTime: String, isLate: Boolean, isAllCompleted: Boolean) -> Unit
    ): CheckInResult {
        if (!isApplicableToday(habit, currentTimeMillis)) {
            return CheckInResult.NotApplicableToday
        }

        val allSlots = habit.getReminderTimesList()
        val completedSlotTimes = todayCompletions
            .filter { it.slotTime.isNotEmpty() }
            .map { it.slotTime }
            .toSet()
        val incompleteSlots = allSlots.filter { it !in completedSlotTimes }

        if (incompleteSlots.isEmpty()) {
            return CheckInResult.AlreadyCompleted(maxCount = allSlots.size)
        }

        if (allSlots.size == 1) {
            val slotTime = allSlots.first()
            val now = currentTimeMillis
            val parts = slotTime.split(":")
            val slotCal = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isLate = now > slotCal.timeInMillis + 60 * 60 * 1000L
            val isAllCompleted = true
            onPerformCheckIn(slotTime, isLate, isAllCompleted)
            return CheckInResult.Success(isLate = isLate, isAllCompleted = true)
        }

        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        for (slot in incompleteSlots) {
            val parts = slot.split(":")
            val slotCal = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val slotTime = slotCal.timeInMillis
            val oneHour = 60 * 60 * 1000L

            if (now.timeInMillis >= slotTime - oneHour) {
                val isLate = now.timeInMillis > slotTime + oneHour
                val remainingAfterThis = incompleteSlots.size - 1
                val isAllCompleted = remainingAfterThis == 0
                onPerformCheckIn(slot, isLate, isAllCompleted)
                return CheckInResult.Success(isLate = isLate, isAllCompleted = isAllCompleted)
            }
        }

        val earliestSlot = incompleteSlots.first()
        val parts = earliestSlot.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val earliest = String.format("%02d:%02d", (hour - 1).coerceAtLeast(0), minute)
        return CheckInResult.TooEarly(earliestSlotTime = earliest)
    }
}
