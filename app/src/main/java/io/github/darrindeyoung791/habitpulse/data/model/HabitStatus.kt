package io.github.darrindeyoung791.habitpulse.data.model

enum class HabitStatus {
    NO_STATUS,
    PENDING_TODAY,
    ABOUT_TO_START,
    COMPLETED_TODAY,
    OVERDUE
}

data class HabitWithStatus(
    val habit: Habit,
    val todayCompletions: List<HabitCompletion>,
    val status: Set<HabitStatus>
) {
    val pendingCount: Int
        get() = habit.getReminderTimesList().size - todayCompletions.size

    val isCompletelyOverdue: Boolean
        get() = status.contains(HabitStatus.OVERDUE)
}
