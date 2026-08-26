package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsExpandableListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsGroupItemGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.ThemeSwitchColors

/**
 * 第三步：通知设置。提醒 / 免打扰 / 常驻通知 三个分段开关；
 * 免打扰开启且提醒开启时，其下方展开两个时间选择项（点击弹出 TimePicker 对话框）。
 * 底部仅「完成」主按钮（原「跳过」已移除，跳过与完成行为等价）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeNotificationsStep(
    reminderEnabled: Boolean,
    dndEnabled: Boolean,
    dndStart: String,
    dndEnd: String,
    persistentEnabled: Boolean,
    onReminderChanged: (Boolean) -> Unit,
    onDndChanged: (Boolean) -> Unit,
    onDndStartChanged: (String) -> Unit,
    onDndEndChanged: (String) -> Unit,
    onPersistentChanged: (Boolean) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)
    val dndBoxVisible = dndEnabled && reminderEnabled
    val context = LocalContext.current

    var showDndStartTimePicker by remember { mutableStateOf(false) }
    var showDndEndTimePicker by remember { mutableStateOf(false) }

    // ── 时间校验：开始 == 结束时自动回退结束时间 ──
    fun validateDndTimes(start: String, end: String) {
        if (start == end) {
            val parts = start.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val newM = if (m == 0) 59 else m - 1
            val newH = if (m == 0) (if (h == 0) 23 else h - 1) else h
            val adjusted = String.format("%02d:%02d", newH, newM)
            onDndEndChanged(adjusted)
            Toast.makeText(context, context.getString(R.string.reminder_settings_dnd_same_time), Toast.LENGTH_SHORT).show()
        }
    }

    WelcomeStepLayout(
        isPhoneLandscape = deviceForm.isPhoneLandscape,
        maxWidth = maxWidth,
        modifier = modifier,
        top = {
            WelcomeRiseIn(delayMs = 40) {
                WelcomeTopBar(onBack = onBack)
            }
        },
        body = {
            WelcomeRiseIn(delayMs = 160) {
                Text(
                    text = stringResource(R.string.onboarding_step_notification_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeRiseIn(delayMs = 280) {
                Text(
                    text = stringResource(R.string.onboarding_step_notification_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            WelcomeRiseIn(delayMs = 400) {
                SettingsSegmentedGroup(modifier = Modifier.widthIn(max = maxWidth)) {
                    SettingsSegmentedSwitch(
                        index = 0,
                        count = 2,
                        headline = stringResource(R.string.settings_reminder),
                        supportingText = stringResource(R.string.settings_reminder_description),
                        leadingIcon = Icons.Outlined.Alarm,
                        checked = reminderEnabled,
                        tintIndex = 0,
                        onCheckedChange = onReminderChanged
                    )
                    val dndInteractionSource = remember { MutableInteractionSource() }
                    SettingsExpandableListSurface(
                        index = 1,
                        count = 2,
                        expanded = dndBoxVisible,
                        onToggle = { if (reminderEnabled) onDndChanged(!dndEnabled) },
                        headline = stringResource(R.string.settings_reminder_dnd),
                        supportingText = stringResource(R.string.settings_reminder_dnd_description),
                        leadingIcon = Icons.Outlined.Bedtime,
                        tintIndex = 5,
                        enabled = reminderEnabled,
                        interactionSource = dndInteractionSource,
                        trailing = {
                            Switch(
                                checked = dndBoxVisible,
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
                                supportingText = dndStart,
                                showArrow = false,
                                onClick = { showDndStartTimePicker = true }
                            )
                            SettingsSegmentedItem(
                                index = 1,
                                count = 2,
                                headline = stringResource(id = R.string.settings_reminder_dnd_end),
                                supportingText = dndEnd,
                                showArrow = false,
                                onClick = { showDndEndTimePicker = true }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        },
        bottom = {
            WelcomeRiseIn(delayMs = 520) {
                WelcomeBottomBar(maxWidth = maxWidth) {
                    WelcomePrimaryButton(
                        text = stringResource(R.string.welcome_step_done),
                        onClick = onFinish
                    )
                }
            }
        }
    )

    // ── 免打扰开始时间 TimePicker（12小时制 + 键盘输入切换） ──
    if (showDndStartTimePicker) {
        val parts = dndStart.split(":")
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
                    onDndStartChanged(newTime)
                    validateDndTimes(newTime, dndEnd)
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
        val parts = dndEnd.split(":")
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
                    onDndEndChanged(newTime)
                    validateDndTimes(dndStart, newTime)
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
