package io.github.darrindeyoung791.habitpulse.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Today
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
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
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
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                // DND section card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.settings_reminder_dnd),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings_reminder_dnd_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dndEnabled,
                                    onCheckedChange = { isChecked ->
                                        scope.launch { userPreferences.setDndEnabled(isChecked) }
                                    }
                                )
                            }

                            if (dndEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(end = 12.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                DndRangeSlider(
                                    startTime = dndStartTime,
                                    endTime = dndEndTime,
                                    onStartTimeChange = { time ->
                                        scope.launch { userPreferences.setDndStartTime(time) }
                                    },
                                    onEndTimeChange = { time ->
                                        scope.launch { userPreferences.setDndEndTime(time) }
                                    }
                                )
                            }
                        }
                    }
                }

                // Status section card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            StatusRow(
                                icon = Icons.Outlined.Shield,
                                label = stringResource(id = R.string.reminder_settings_keepalive_status),
                                value = if (isServiceRunning)
                                    stringResource(id = R.string.reminder_settings_service_running)
                                else
                                    stringResource(id = R.string.reminder_settings_service_stopped),
                                isPositive = isServiceRunning
                            )
                            if (reminderSentData.second > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                StatusRow(
                                    icon = Icons.Outlined.Today,
                                    label = stringResource(id = R.string.reminder_settings_today_count),
                                    value = "${reminderSentData.second}",
                                    isPositive = true
                                )
                            }
                        }
                    }
                }

                // Next alarm & permission inline card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                    }
                }

                // Test notification button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
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
                        modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}

@Composable
private fun DndRangeSlider(
    startTime: String,
    endTime: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit
) {
    val toLinear: (String) -> Float = { time ->
        val parts = time.split(":")
        var h = parts[0].toInt()
        val m = parts[1].toInt()
        if (h < 12) h += 24
        (h * 60 + m).toFloat()
    }
    val fromLinear: (Float) -> String = { value ->
        var totalMin = value.toInt().coerceIn(1260, 1920)
        var h = totalMin / 60
        val m = ((totalMin % 60) / 30) * 30
        if (h >= 24) h -= 24
        String.format("%02d:%02d", h, m)
    }

    val rangeStart = 1260f
    val rangeEnd = 1920f
    val stepCount = ((rangeEnd - rangeStart) / 30f).toInt() - 1

    var curStart by remember(startTime) { mutableFloatStateOf(toLinear(startTime)) }
    var curEnd by remember(endTime) { mutableFloatStateOf(toLinear(endTime)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = fromLinear(curStart),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.settings_reminder_dnd_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(id = R.string.settings_reminder_dnd_to),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = fromLinear(curEnd),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.settings_reminder_dnd_end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        RangeSlider(
            value = curStart..curEnd,
            onValueChange = { range ->
                curStart = range.start
                curEnd = range.endInclusive
            },
            onValueChangeFinished = {
                onStartTimeChange(fromLinear(curStart))
                onEndTimeChange(fromLinear(curEnd))
            },
            valueRange = rangeStart..rangeEnd,
            steps = stepCount,
            modifier = Modifier.fillMaxWidth()
        )
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPositive) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
