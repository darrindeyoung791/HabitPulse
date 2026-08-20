package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo

/**
 * 第一步：欢迎（Apple 式克制极简）。仅两行文本 + 单个「继续」按钮，
 * 无 Logo、无图标、无权限行。
 */
@Composable
fun WelcomeGreetingStep(
    animate: Boolean,
    onContinue: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)
    val titleStyle = if (deviceForm.isTabletLandscape) {
        MaterialTheme.typography.headlineLarge
    } else {
        MaterialTheme.typography.headlineMedium
    }

    WelcomeStepLayout(
        isPhoneLandscape = deviceForm.isPhoneLandscape,
        maxWidth = maxWidth,
        modifier = modifier,
        body = {
            WelcomeRiseIn(animate = animate, delayMs = 160) {
                Text(
                    text = stringResource(R.string.welcome_greeting_title, stringResource(R.string.app_name)),
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeRiseIn(animate = animate, delayMs = 280) {
                Text(
                    text = stringResource(R.string.welcome_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        },
        bottom = {
            WelcomeRiseIn(animate = animate, delayMs = 400) {
                WelcomeBottomBar(maxWidth = maxWidth) {
                    WelcomePrimaryButton(
                        text = stringResource(R.string.welcome_greeting_continue),
                        onClick = onContinue
                    )
                }
            }
        }
    )
}