package io.github.darrindeyoung791.habitpulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.SlotCheckInEngine
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import io.github.darrindeyoung791.habitpulse.utils.ReminderNotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                handleReminder(context)
            } catch (e: Exception) {
                android.util.Log.e("ReminderReceiver", "Error handling reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context) {
        if (!NotificationHelper.hasNotificationPermission(context)) {
            ReminderManager.scheduleNextAlarm(context)
            return
        }

        if (ReminderManager.isWithinDnd(context)) {
            ReminderManager.scheduleNextAlarm(context)
            return
        }

        val app = context.applicationContext as HabitPulseApplication
        val repository = app.repository

        val todayCompletions = repository.getTodayCompletions()
        val allHabits = repository.getAllHabits()
        val now = System.currentTimeMillis()

        val aboutToStartLines = allHabits.mapNotNull { habit ->
            if (!SlotCheckInEngine.isApplicableToday(habit, now)) return@mapNotNull null

            val habitCompletions = todayCompletions.filter { it.habitId == habit.id }
            val completedSlots = habitCompletions
                .filter { it.slotTime.isNotEmpty() }
                .map { it.slotTime }
                .toSet()
            val allSlots = habit.getReminderTimesList()
            val incompleteSlots = allSlots.filter { it !in completedSlots }
            if (incompleteSlots.isEmpty()) return@mapNotNull null

            val nextSlot = incompleteSlots.firstOrNull { slot ->
                val parts = slot.split(":")
                if (parts.size != 2) return@firstOrNull false
                val slotCal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val slotTime = slotCal.timeInMillis
                slotTime > now && slotTime <= now + 3_600_000L
            }

            if (nextSlot != null) "${nextSlot}  ${habit.title}" else null
        }

        if (aboutToStartLines.isNotEmpty()) {
            val notification = ReminderNotificationBuilder.buildReminderNotification(
                context, aboutToStartLines
            )
            if (notification != null) {
                ReminderNotificationBuilder.sendNotification(context, notification)
            }
        }

        ReminderManager.scheduleNextAlarm(context)

        val userPrefs = UserPreferences.getInstance(context)
        val todayDateStr = LocalDate.now().toString()
        userPrefs.incrementReminderSentCount(todayDateStr)
    }
}
