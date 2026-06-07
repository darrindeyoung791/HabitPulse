package io.github.darrindeyoung791.habitpulse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.darrindeyoung791.habitpulse.MainActivity
import io.github.darrindeyoung791.habitpulse.R
import java.util.UUID

object ReminderNotificationBuilder {

    const val CHANNEL_ID = "habit_reminder"
    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val EXTRA_VALUE_ABOUT_TO_START = "about_to_start"

    private var notificationIdCounter = 2000L

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.reminder_channel_name)
            val description = context.getString(R.string.reminder_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildReminderNotification(
        context: Context,
        habitTitles: List<String>
    ): android.app.Notification? {
        if (habitTitles.isEmpty()) return null

        val count = habitTitles.size
        val title = context.getString(
            R.string.reminder_notification_title,
            count
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context))

        val inboxStyle = NotificationCompat.InboxStyle()
        val maxLines = 5
        val displayedTitles = if (count <= maxLines) {
            habitTitles
        } else {
            habitTitles.take(maxLines - 1) + listOf(
                context.getString(R.string.reminder_notification_more, count - (maxLines - 1))
            )
        }
        displayedTitles.forEach { inboxStyle.addLine(it) }
        builder.setStyle(inboxStyle)

        return builder.build()
    }

    fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, EXTRA_VALUE_ABOUT_TO_START)
        }
        return PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildTestNotification(context: Context): android.app.Notification {
        val title = context.getString(R.string.reminder_test_notification_title)
        val habitA = context.getString(R.string.reminder_test_habit_a)
        val habitB = context.getString(R.string.reminder_test_habit_b)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText("$habitA、$habitB")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context))

        val inboxStyle = NotificationCompat.InboxStyle()
        inboxStyle.addLine(habitA)
        inboxStyle.addLine(habitB)
        builder.setStyle(inboxStyle)

        return builder.build()
    }

    fun sendNotification(context: Context, notification: android.app.Notification) {
        val id = generateNotificationId()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun generateNotificationId(): Int {
        notificationIdCounter++
        return (notificationIdCounter % Int.MAX_VALUE).toInt()
    }
}
