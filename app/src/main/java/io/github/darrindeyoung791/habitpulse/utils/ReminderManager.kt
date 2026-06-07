package io.github.darrindeyoung791.habitpulse.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.receiver.ReminderReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.util.Calendar

object ReminderManager {

    private const val REQUEST_CODE_SCHEDULE = 2001
    private const val ACTION_REMINDER_ALARM = "io.github.darrindeyoung791.habitpulse.action.REMINDER_ALARM"

    fun scheduleNextAlarm(context: Context) {
        val nextTime = getNextSlotTime() ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_REMINDER_ALARM).apply {
            setClass(context, ReminderReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTime, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
        }

        runBlocking {
            UserPreferences.getInstance(context).setNextAlarmTime(nextTime)
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_REMINDER_ALARM).apply {
            setClass(context, ReminderReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        runBlocking {
            UserPreferences.getInstance(context).setNextAlarmTime(null)
        }
    }

    fun isAlarmScheduled(context: Context): Boolean {
        val intent = Intent(ACTION_REMINDER_ALARM).apply {
            setClass(context, ReminderReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    fun isWithinDnd(context: Context): Boolean {
        return try {
            runBlocking {
                val userPreferences = UserPreferences.getInstance(context)
                val dndEnabled = userPreferences.dndEnabledFlow.first()
                if (!dndEnabled) return@runBlocking false

                val startTimeStr = userPreferences.dndStartTimeFlow.first()
                val endTimeStr = userPreferences.dndEndTimeFlow.first()

                val now = LocalTime.now()
                val start = LocalTime.parse(startTimeStr)
                val end = LocalTime.parse(endTimeStr)

                if (start < end) {
                    !now.isBefore(start) && now.isBefore(end)
                } else {
                    !now.isBefore(start) || now.isBefore(end)
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getNextSlotTime(): Long? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
        }

        val minute = calendar.get(Calendar.MINUTE)

        if (minute < 30) {
            calendar.set(Calendar.MINUTE, 30)
        } else {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.MINUTE, 0)
        }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}
