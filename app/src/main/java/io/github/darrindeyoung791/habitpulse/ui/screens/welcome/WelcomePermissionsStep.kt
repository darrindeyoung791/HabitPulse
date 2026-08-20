package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem

/**
 * 第二步：权限声明。两条权限图标行 + 隐私政策/服务条款占位链接；
 * 「我同意，继续」主按钮 + 「不同意」次按钮。
 */
@Composable
fun WelcomePermissionsStep(
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    onLinkClick: () -> Unit,
    onBack: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    val maxWidth = welcomeContentMaxWidth(deviceForm)

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
                    text = stringResource(R.string.welcome_permissions_heading),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeRiseIn(delayMs = 280) {
                Text(
                    text = stringResource(
                        R.string.welcome_permissions_title,
                        stringResource(R.string.app_name)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            WelcomeRiseIn(delayMs = 400) {
                SettingsSegmentedGroup(modifier = Modifier.widthIn(max = maxWidth)) {
                    SettingsSegmentedItem(
                        index = 0,
                        count = 2,
                        headline = stringResource(R.string.welcome_permission_notification_title),
                        supportingText = stringResource(R.string.welcome_permission_notification_desc),
                        leadingIcon = Icons.Outlined.Notifications,
                        tintIndex = 0,
                        showArrow = false,
                        clickable = false,
                        onClick = {}
                    )
                    SettingsSegmentedItem(
                        index = 1,
                        count = 2,
                        headline = stringResource(R.string.welcome_permission_background_title),
                        supportingText = stringResource(R.string.welcome_permission_background_desc),
                        leadingIcon = Icons.Outlined.Autorenew,
                        tintIndex = 3,
                        showArrow = false,
                        clickable = false,
                        onClick = {}
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            WelcomeRiseIn(delayMs = 520) {
                WelcomeConsentLinks(onLinkClick = onLinkClick)
            }
            Spacer(modifier = Modifier.height(24.dp))
        },
        bottom = {
            WelcomeRiseIn(delayMs = 640) {
                WelcomeBottomBar(maxWidth = maxWidth) {
                    WelcomePrimaryButton(
                        text = stringResource(R.string.welcome_agree_button),
                        onClick = onAgree
                    )
                    WelcomeSecondaryButton(
                        text = stringResource(R.string.welcome_disagree_button),
                        onClick = onDisagree
                    )
                }
            }
        }
    )
}

/**
 * 「继续即代表你同意 隐私政策 与 服务条款」占位链接行。
 */
@Composable
private fun WelcomeConsentLinks(onLinkClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val prefix = stringResource(R.string.welcome_consent_prefix)
    val privacy = stringResource(R.string.onboarding_privacy_link)
    val and = stringResource(R.string.welcome_consent_and)
    val terms = stringResource(R.string.welcome_terms_of_service)

    val linkStyle = SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)
    val annotated = remember(prefix, privacy, and, terms, primary) {
        buildAnnotatedString {
            append(prefix)
            withLink(LinkAnnotation.Clickable(tag = "privacy") { onLinkClick() }) {
                withStyle(linkStyle) { append(privacy) }
            }
            append(and)
            withLink(LinkAnnotation.Clickable(tag = "terms") { onLinkClick() }) {
                withStyle(linkStyle) { append(terms) }
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}