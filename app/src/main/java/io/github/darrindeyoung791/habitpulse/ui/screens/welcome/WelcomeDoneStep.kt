package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.AnimatedFeedbackIcon
import kotlinx.coroutines.delay

/**
 * 第四步：完成。12 角形徽章（对齐打卡奖励弹窗样式，旋转放大动画）+ 「一切就绪」+ 一句鼓励语 + 「进入 HabitPulse」。
 */
@Composable
fun WelcomeDoneStep(
    onEnter: () -> Unit,
    onBack: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)
    var iconAnimStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(160)
        iconAnimStarted = true
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
            Spacer(modifier = Modifier.height(40.dp))
            AnimatedFeedbackIcon(
                animationStarted = iconAnimStarted,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                icon = Icons.Outlined.Check,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(32.dp))
            WelcomeRiseIn(delayMs = 280) {
                Text(
                    text = stringResource(R.string.welcome_done_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeRiseIn(delayMs = 400) {
                Text(
                    text = stringResource(R.string.welcome_done_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        },
        bottom = {
            WelcomeRiseIn(delayMs = 520) {
                WelcomeBottomBar(maxWidth = maxWidth) {
                    WelcomePrimaryButton(
                        text = stringResource(R.string.welcome_enter, stringResource(R.string.app_name)),
                        onClick = onEnter
                    )
                }
            }
        }
    )
}