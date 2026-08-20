package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.dnd.DndRangeSlider
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedBox
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch

/**
 * 第三步：通知设置。提醒 / 免打扰 / 常驻通知 三个分段开关；
 * 免打扰开启且提醒开启时，其下方展开内嵌 DND 时段滑块（与新设置一致）。
 * 「完成」与「跳过，稍后设置」都通过 [onFinish] / [onSkip] 回到调用方。
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
    onSkip: () -> Unit,
    onBack: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)
    val dndBoxVisible = dndEnabled && reminderEnabled
    val groupCount = if (dndBoxVisible) 3 else 2

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
                        count = groupCount,
                        headline = stringResource(R.string.settings_reminder),
                        supportingText = stringResource(R.string.settings_reminder_description),
                        leadingIcon = Icons.Outlined.Alarm,
                        checked = reminderEnabled,
                        tintIndex = 0,
                        onCheckedChange = onReminderChanged
                    )
                    SettingsSegmentedSwitch(
                        index = 1,
                        count = groupCount,
                        headline = stringResource(R.string.settings_reminder_dnd),
                        supportingText = stringResource(R.string.settings_reminder_dnd_description),
                        leadingIcon = Icons.Outlined.Bedtime,
                        checked = dndEnabled,
                        enabled = reminderEnabled,
                        tintIndex = 5,
                        onCheckedChange = onDndChanged
                    )
                    AnimatedVisibility(
                        visible = dndBoxVisible,
                        enter = expandVertically(expandFrom = Alignment.Top),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top),
                        label = "welcomeDndBox"
                    ) {
                        SettingsSegmentedBox(index = 2, count = 3) {
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
                    WelcomeSecondaryButton(
                        text = stringResource(R.string.welcome_skip),
                        onClick = onSkip
                    )
                }
            }
        }
    )
}