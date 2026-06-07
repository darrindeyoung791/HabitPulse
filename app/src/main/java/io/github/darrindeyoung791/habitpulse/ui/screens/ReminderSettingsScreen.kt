package io.github.darrindeyoung791.habitpulse.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val nextAlarmTime by userPreferences.nextAlarmTimeFlow.collectAsStateWithLifecycle(initialValue = null)
    val reminderSentData by userPreferences.reminderSentCountFlow.collectAsStateWithLifecycle(initialValue = Pair("", 0L))

    val isServiceRunning = remember {
        isForegroundServiceRunning(context, ForegroundNotificationService::class.java)
    }
    val hasNotificationPermission = remember {
        NotificationHelper.hasNotificationPermission(context)
    }

    var testSent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        testSent = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.reminder_settings_title))
                },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Keep-alive status
            item {
                StatusCard(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(id = R.string.reminder_settings_keepalive_status),
                    status = if (isServiceRunning) {
                        stringResource(id = R.string.reminder_settings_service_running)
                    } else {
                        stringResource(id = R.string.reminder_settings_service_stopped)
                    },
                    isPositive = isServiceRunning
                )
            }

            // Next reminder time
            item {
                StatusCard(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(id = R.string.reminder_settings_next_alarm),
                    status = if (reminderEnabled && nextAlarmTime != null) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.format(Date(nextAlarmTime!!))
                    } else {
                        stringResource(id = R.string.reminder_settings_reminder_disabled)
                    },
                    isPositive = reminderEnabled && nextAlarmTime != null
                )
            }

            // Notification permission
            item {
                StatusCard(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(id = R.string.reminder_settings_notification_permission),
                    status = if (hasNotificationPermission) {
                        stringResource(id = R.string.reminder_settings_permission_granted)
                    } else {
                        stringResource(id = R.string.reminder_settings_permission_denied)
                    },
                    isPositive = hasNotificationPermission
                )
            }

            // Today's reminder count
            item {
                StatusCard(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(id = R.string.reminder_settings_today_count),
                    status = "${reminderSentData.second}",
                    isPositive = reminderSentData.second > 0
                )
            }

            // Send test notification
            item {
                Button(
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
                            testSent = true
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
                    Text(text = stringResource(id = R.string.reminder_settings_test_notification))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    status: String,
    isPositive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPositive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (isPositive) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
