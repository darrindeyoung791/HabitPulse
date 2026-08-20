package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo

/**
 * 第四步：完成。绿色圆形对勾 + 「一切就绪」+ 一句鼓励语 + 「进入 HabitPulse」。
 */
@Composable
fun WelcomeDoneStep(
    onEnter: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)

    WelcomeStepLayout(
        isPhoneLandscape = deviceForm.isPhoneLandscape,
        maxWidth = maxWidth,
        modifier = modifier,
        body = {
            Spacer(modifier = Modifier.height(40.dp))
            WelcomeRiseIn(delayMs = 160) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    WelcomeCheckIcon()
                }
            }
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

@Composable
private fun WelcomeCheckIcon() {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(400, delayMillis = 200)) +
            scaleIn(tween(400, delayMillis = 200), initialScale = 0.4f),
        label = "welcomeCheckScale"
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(52.dp)
        )
    }
}