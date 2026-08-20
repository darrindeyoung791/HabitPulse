package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.receiver.ReminderReceiver
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSectionHeader
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderNotificationBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 调试 → 提醒调试子页面：
 * 展示保活状态、下次提醒、通知权限，并提供测试通知按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDebugReminderScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val nextAlarmTime by userPreferences.nextAlarmTimeFlow.collectAsStateWithLifecycle(initialValue = null)
    val reminderSentData by userPreferences.reminderSentCountFlow.collectAsStateWithLifecycle(initialValue = Pair("", 0L))

    val isServiceRunning = isForegroundServiceRunning(context, ForegroundNotificationService::class.java)
    val hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)

    SettingsScaffold(
        title = stringResource(id = R.string.settings_debug_reminder_title),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSectionHeader(text = stringResource(id = R.string.reminder_settings_keepalive_status))
        StatusRow(
            icon = Icons.Outlined.Shield,
            label = stringResource(id = R.string.reminder_settings_keepalive_status),
            value = if (isServiceRunning)
                stringResource(id = R.string.reminder_settings_service_running)
            else
                stringResource(id = R.string.reminder_settings_service_stopped),
            isPositive = isServiceRunning
        )
        if (isServiceRunning) {
            StatusRow(
                icon = Icons.Outlined.Today,
                label = stringResource(id = R.string.reminder_settings_today_count),
                value = "${reminderSentData.second}",
                isPositive = true
            )
        }

        SettingsSectionHeader(text = stringResource(id = R.string.settings_notifications))
        StatusRow(
            icon = Icons.Outlined.Schedule,
            label = stringResource(id = R.string.reminder_settings_next_alarm),
            value = if (nextAlarmTime != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextAlarmTime!!))
            } else {
                stringResource(id = R.string.reminder_settings_no_alarm_scheduled)
            },
            isPositive = nextAlarmTime != null
        )
        StatusRow(
            icon = Icons.Outlined.Notifications,
            label = stringResource(id = R.string.reminder_settings_notification_permission),
            value = if (hasNotificationPermission)
                stringResource(id = R.string.reminder_settings_permission_granted)
            else
                stringResource(id = R.string.reminder_settings_permission_denied),
            isPositive = hasNotificationPermission
        )

        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = {
                scope.launch {
                    if (!hasNotificationPermission) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.reminder_settings_permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    ReminderNotificationBuilder.createNotificationChannel(context)
                    val notif = ReminderNotificationBuilder.buildTestNotification(context)
                    ReminderNotificationBuilder.sendNotification(context, notif)
                    Toast.makeText(
                        context,
                        context.getString(R.string.reminder_settings_test_notification_sent),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = hasNotificationPermission
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.reminder_settings_test_notification))
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    if (!hasNotificationPermission) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.reminder_settings_permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    ReminderNotificationBuilder.createNotificationChannel(context)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val targetTime = System.currentTimeMillis() + 60_000L
                    val intent = Intent("io.github.darrindeyoung791.habitpulse.action.REMINDER_ALARM").apply {
                        setClass(context, ReminderReceiver::class.java)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        2002,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                    } else {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(targetTime, pendingIntent),
                            pendingIntent
                        )
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.reminder_settings_test_notification_scheduled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = hasNotificationPermission
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.reminder_settings_test_notification_delayed))
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Suppress("DEPRECATION")
private fun isForegroundServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getRunningServices(Integer.MAX_VALUE).any { service ->
        service.service?.className == serviceClass.name
    }
}