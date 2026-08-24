package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.dnd.DndRangeSlider
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsExpandableListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.ThemeSwitchColors

/**
 * 第三步：通知设置。提醒 / 免打扰 / 常驻通知 三个分段开关；
 * 免打扰开启且提醒开启时，其下方展开内嵌 DND 时段滑块（与新设置一致）。
 * 底部仅「完成」主按钮（原「跳过」已移除，跳过与完成行为等价）。
 */
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
                        DndRangeSlider(
                            startTime = dndStart,
                            endTime = dndEnd,
                            onStartTimeChange = onDndStartChanged,
                            onEndTimeChange = onDndEndChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
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
}