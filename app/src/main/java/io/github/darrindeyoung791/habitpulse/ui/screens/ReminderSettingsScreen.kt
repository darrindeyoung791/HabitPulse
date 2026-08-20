package io.github.darrindeyoung791.habitpulse.ui.screens

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import io.github.darrindeyoung791.habitpulse.WebViewActivity
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.dnd.DndRangeSlider
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import io.github.darrindeyoung791.habitpulse.utils.ReminderNotificationBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    onBackAction: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val reminderEnabled by userPreferences.reminderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val dndEnabled by userPreferences.dndEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val dndStartTime by userPreferences.dndStartTimeFlow.collectAsStateWithLifecycle(initialValue = "22:00")
    val dndEndTime by userPreferences.dndEndTimeFlow.collectAsStateWithLifecycle(initialValue = "07:00")
    val nextAlarmTime by userPreferences.nextAlarmTimeFlow.collectAsStateWithLifecycle(initialValue = null)
    val reminderSentData by userPreferences.reminderSentCountFlow.collectAsStateWithLifecycle(initialValue = Pair("", 0L))

    val isServiceRunning = isForegroundServiceRunning(context, ForegroundNotificationService::class.java)
    val hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.reminder_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.REMINDER_HELP_URL)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(id = R.string.webview_help)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Md3ScrollableColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            listState = listState
        ) {

            // Reminder master switch
            item {
                SettingsSwitchItem(
                    headline = stringResource(id = R.string.settings_reminder),
                    supportingText = stringResource(id = R.string.settings_reminder_description),
                    checked = reminderEnabled,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            userPreferences.setReminderEnabled(isChecked)
                            if (isChecked) {
                                ReminderNotificationBuilder.createNotificationChannel(context)
                                ReminderManager.scheduleNextAlarm(context)
                            } else {
                                ReminderManager.cancelAlarm(context)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null
                        )
                    }
                )
            }

            if (reminderEnabled) {
                // DND toggle
                item {
                    SettingsSwitchItem(
                        headline = stringResource(id = R.string.settings_reminder_dnd),
                        supportingText = stringResource(id = R.string.settings_reminder_dnd_description),
                        checked = dndEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { userPreferences.setDndEnabled(isChecked) }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = null
                            )
                        }
                    )
                }

                // DND range slider with animation
                item {
                    AnimatedVisibility(
                        visible = dndEnabled,
                        enter = expandVertically(expandFrom = Alignment.Top),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        DndRangeSlider(
                            startTime = dndStartTime,
                            endTime = dndEndTime,
                            onStartTimeChange = { time ->
                                scope.launch { userPreferences.setDndStartTime(time) }
                            },
                            onEndTimeChange = { time ->
                                scope.launch { userPreferences.setDndEndTime(time) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                // Section header: status
                item {
                    SectionHeader(text = stringResource(id = R.string.reminder_settings_keepalive_status))
                }

                item {
                    StatusRow(
                        icon = Icons.Outlined.Shield,
                        label = stringResource(id = R.string.reminder_settings_keepalive_status),
                        value = if (isServiceRunning)
                            stringResource(id = R.string.reminder_settings_service_running)
                        else
                            stringResource(id = R.string.reminder_settings_service_stopped),
                        isPositive = isServiceRunning
                    )
                }

                if (isServiceRunning) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        StatusRow(
                            icon = Icons.Outlined.Today,
                            label = stringResource(id = R.string.reminder_settings_today_count),
                            value = "${reminderSentData.second}",
                            isPositive = true
                        )
                    }
                }

                // Section header: schedule
                item {
                    SectionHeader(text = stringResource(id = R.string.settings_notifications))
                }

                item {
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
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    StatusRow(
                        icon = Icons.Outlined.Notifications,
                        label = stringResource(id = R.string.reminder_settings_notification_permission),
                        value = if (hasNotificationPermission)
                            stringResource(id = R.string.reminder_settings_permission_granted)
                        else
                            stringResource(id = R.string.reminder_settings_permission_denied),
                        isPositive = hasNotificationPermission
                    )
                }

                // Test notification - immediate
                item {
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
                }

                // Test notification - delayed 1 minute via AlarmManager
                item {
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

                // System notification settings - left-aligned text button
                item {
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.reminder_settings_system_settings),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Background keep-alive help - same style, opens help page
                item {
                    TextButton(
                        onClick = {
                            val intent = Intent(context, WebViewActivity::class.java).apply {
                                putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.REMINDER_HELP_URL)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.reminder_settings_background_keepalive, stringResource(R.string.app_name)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPositive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
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
                tint = if (isPositive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = if (isPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    headline: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Suppress("DEPRECATION")
private fun isForegroundServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getRunningServices(Integer.MAX_VALUE).any { service ->
        service.service?.className == serviceClass.name
    }
}
