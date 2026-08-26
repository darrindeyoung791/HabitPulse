package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.WebViewActivity
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsExpandableListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsGroupItemGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsTextLinkButton
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.ThemeSwitchColors
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.NotificationPermissionHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import io.github.darrindeyoung791.habitpulse.utils.ReminderNotificationBuilder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
    onOpenHelp: () -> Unit,
    onNavigateTemplate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val persistentNotification by userPreferences.persistentNotificationFlow.collectAsStateWithLifecycle(initialValue = false)
    val reminderEnabled by userPreferences.reminderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val dndEnabled by userPreferences.dndEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val dndStartTime by userPreferences.dndStartTimeFlow.collectAsStateWithLifecycle(initialValue = "22:00")
    val dndEndTime by userPreferences.dndEndTimeFlow.collectAsStateWithLifecycle(initialValue = "07:00")

    var hasNotificationPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
        }
    }

    // 通知权限请求（常驻通知开启用）
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                userPreferences.setPersistentNotification(true)
                NotificationHelper.createNotificationChannel(context)
                ForegroundNotificationService.toggleService(context, true)
            }
        }
    }

    // 通知权限请求（习惯提醒授权用）
    val requestReminderPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 仅请求权限，不自动开启提醒 */ }

    var showDndStartTimePicker by remember { mutableStateOf(false) }
    var showDndEndTimePicker by remember { mutableStateOf(false) }

    // 免打扰时间校验：起止不能相同，相同则将结束时间往前调 1 分钟
    fun validateDndTimes(start: String, end: String) {
        if (start == end) {
            val parts = start.split(":")
            var h = parts[0].toInt()
            val m = parts[1].toInt()
            var totalMin = h * 60 + m - 1
            if (totalMin < 0) totalMin += 24 * 60
            val adjusted = String.format("%02d:%02d", totalMin / 60, totalMin % 60)
            scope.launch { userPreferences.setDndEndTime(adjusted) }
            Toast.makeText(context, context.getString(R.string.reminder_settings_dnd_same_time), Toast.LENGTH_SHORT).show()
        }
    }

    val showDndItems = dndEnabled && reminderEnabled

    SettingsScaffold(
        title = stringResource(id = R.string.settings_notifications),
        onBack = onBack,
        onHelp = onOpenHelp,
        showBack = showBack
    ) {
        // ── 习惯提醒组 ──
        SettingsSegmentedGroup {
            if (hasNotificationPermission) {
                SettingsSegmentedSwitch(
                    index = 0,
                    count = 2,
                    headline = stringResource(id = R.string.settings_reminder),
                    supportingText = stringResource(id = R.string.settings_reminder_description),
                    leadingIcon = Icons.Outlined.Alarm,
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
                    }
                )
            } else {
                SettingsSegmentedItem(
                    index = 0,
                    count = 2,
                    headline = stringResource(id = R.string.settings_reminder),
                    supportingText = stringResource(id = R.string.settings_persistent_notification_authorize),
                    showArrow = false,
                    leadingIcon = Icons.Outlined.Alarm,
                    onClick = {
                        val activity = context as? ComponentActivity
                        val shouldShowRationale = activity?.let {
                            NotificationHelper.shouldShowPermissionRationale(it)
                        } ?: true
                        if (shouldShowRationale) {
                            requestReminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationPermissionHelper.openAppSettings(context)
                        }
                    }
                )
            }

            // ── 免打扰时段（开关即展开变体） ──
            val dndInteractionSource = remember { MutableInteractionSource() }
            SettingsExpandableListSurface(
                index = 1,
                count = 2,
                expanded = showDndItems,
                onToggle = {
                    if (reminderEnabled) {
                        scope.launch { userPreferences.setDndEnabled(!dndEnabled) }
                    }
                },
                headline = stringResource(id = R.string.settings_reminder_dnd),
                supportingText = stringResource(id = R.string.settings_reminder_dnd_description),
                leadingIcon = Icons.Outlined.Bedtime,
                enabled = reminderEnabled,
                interactionSource = dndInteractionSource,
                trailing = {
                    Switch(
                        checked = showDndItems,
                        onCheckedChange = null,
                        enabled = reminderEnabled,
                        interactionSource = dndInteractionSource,
                        colors = ThemeSwitchColors()
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SettingsGroupItemGap)) {
                    SettingsSegmentedItem(
                        index = 0,
                        count = 2,
                        headline = stringResource(id = R.string.settings_reminder_dnd_start),
                        supportingText = dndStartTime,
                        showArrow = false,
                        onClick = { showDndStartTimePicker = true }
                    )
                    SettingsSegmentedItem(
                        index = 1,
                        count = 2,
                        headline = stringResource(id = R.string.settings_reminder_dnd_end),
                        supportingText = dndEndTime,
                        showArrow = false,
                        onClick = { showDndEndTimePicker = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        // ── 常驻通知组 ──
        SettingsSegmentedGroup(tintOffset = 2) {
            if (hasNotificationPermission) {
                SettingsSegmentedSwitch(
                    index = 0,
                    count = 2,
                    headline = stringResource(id = R.string.settings_persistent_notification),
                    supportingText = stringResource(id = R.string.settings_persistent_notification_description),
                    leadingIcon = Icons.Outlined.Notifications,
                    checked = persistentNotification,
                    onCheckedChange = { isChecked ->
                        if (!isChecked) {
                            scope.launch {
                                userPreferences.setPersistentNotification(false)
                                ForegroundNotificationService.toggleService(context, false)
                            }
                        } else {
                            val application = context.applicationContext as HabitPulseApplication
                            val isLimitedMode = application.habitViewModel.isLimitedMode.value
                            if (isLimitedMode) {
                                scope.launch {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                scope.launch {
                                    userPreferences.setPersistentNotification(true)
                                    NotificationHelper.createNotificationChannel(context)
                                    ForegroundNotificationService.toggleService(context, true)
                                }
                            }
                        }
                    }
                )
            } else {
                SettingsSegmentedItem(
                    index = 0,
                    count = 2,
                    headline = stringResource(id = R.string.settings_persistent_notification),
                    supportingText = stringResource(id = R.string.settings_persistent_notification_authorize),
                    showArrow = false,
                    leadingIcon = Icons.Outlined.Notifications,
                    onClick = {
                        val activity = context as? ComponentActivity
                        val shouldShowRationale = activity?.let {
                            NotificationHelper.shouldShowPermissionRationale(it)
                        } ?: true
                        if (shouldShowRationale) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationPermissionHelper.openAppSettings(context)
                        }
                    }
                )
            }
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.notification_template_settings_title),
                supportingText = stringResource(id = R.string.notification_template_settings_desc),
                leadingIcon = Icons.AutoMirrored.Outlined.Article,
                onClick = onNavigateTemplate
            )
        }

        SettingsTextLinkButton(
            text = stringResource(id = R.string.reminder_settings_system_settings),
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }
        )
        SettingsTextLinkButton(
            text = stringResource(id = R.string.reminder_settings_background_keepalive, stringResource(R.string.app_name)),
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.REMINDER_HELP_URL)
                }
                context.startActivity(intent)
            }
        )
    }

    // ── 免打扰开始时间 TimePicker（12小时制 + 键盘输入切换） ──
    if (showDndStartTimePicker) {
        val parts = dndStartTime.split(":")
        val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 22
        val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val state = rememberTimePickerState(initialHour = initialH, initialMinute = initialM, is24Hour = false)
        var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { showDndStartTimePicker = false },
            title = { Text(stringResource(id = R.string.settings_reminder_dnd_start)) },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = String.format("%02d:%02d", state.hour, state.minute)
                    scope.launch { userPreferences.setDndStartTime(newTime) }
                    validateDndTimes(newTime, dndEndTime)
                    showDndStartTimePicker = false
                }) { Text(stringResource(id = R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDndStartTimePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = displayMode
                )
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (displayMode == TimePickerDisplayMode.Picker) {
                    TimePicker(state = state)
                } else {
                    TimeInput(state = state)
                }
            }
        }
    }

    // ── 免打扰结束时间 TimePicker（12小时制 + 键盘输入切换） ──
    if (showDndEndTimePicker) {
        val parts = dndEndTime.split(":")
        val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val state = rememberTimePickerState(initialHour = initialH, initialMinute = initialM, is24Hour = false)
        var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { showDndEndTimePicker = false },
            title = { Text(stringResource(id = R.string.settings_reminder_dnd_end)) },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = String.format("%02d:%02d", state.hour, state.minute)
                    scope.launch { userPreferences.setDndEndTime(newTime) }
                    validateDndTimes(dndStartTime, newTime)
                    showDndEndTimePicker = false
                }) { Text(stringResource(id = R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDndEndTimePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = displayMode
                )
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (displayMode == TimePickerDisplayMode.Picker) {
                    TimePicker(state = state)
                } else {
                    TimeInput(state = state)
                }
            }
        }
    }
}
