package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.WebViewActivity
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSectionHeader
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsTextLinkButton
import io.github.darrindeyoung791.habitpulse.utils.AccessibilityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutDetailScreen(
    onBack: () -> Unit,
    // 平板双栏：右侧面板的子页面根页隐藏返回按钮（无处可返回）
    showBack: Boolean = true,
    onOpenHelp: () -> Unit,
    onNavigateDebug: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    val context = LocalContext.current

    var versionTapCount by remember { mutableStateOf(0) }
    var versionTapStartTime by remember { mutableStateOf(0L) }
    var showDebugConfirm by remember { mutableStateOf(false) }

    val TAP_TIME_WINDOW = 5_000L
    val TAP_COUNT_THRESHOLD = 5

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = packageInfo.versionName ?: "1.0.0"
            val versionCode = packageInfo.longVersionCode
            val isDebug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val buildType = if (isDebug) "Debug" else "Release"
            "$version ($versionCode) ($buildType)"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val appName = stringResource(id = R.string.app_name)

    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val showSplashAd by userPreferences.showSplashAdFlow.collectAsStateWithLifecycle(initialValue = false)

    var isTalkBackEnabled by remember { mutableStateOf(AccessibilityUtils.isTalkBackEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled(context)
        }
    }

    SettingsScaffold(
        title = stringResource(id = R.string.settings_about),
        onBack = onBack,
        onHelp = onOpenHelp,
        showBack = showBack
    ) {
        Text(
            text = stringResource(id = R.string.settings_privacy_notice, appName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 3,
                headline = stringResource(id = R.string.settings_app_version_label),
                supportingText = versionName,
                showArrow = false,
                leadingIcon = Icons.Outlined.Info,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - versionTapStartTime > TAP_TIME_WINDOW) {
                        versionTapCount = 1
                        versionTapStartTime = currentTime
                    } else {
                        versionTapCount++
                        if (versionTapCount >= TAP_COUNT_THRESHOLD) {
                            versionTapCount = 0
                            versionTapStartTime = 0
                            showDebugConfirm = true
                        }
                    }
                }
            )
            SettingsSegmentedItem(
                index = 1,
                count = 3,
                headline = stringResource(id = R.string.settings_developer),
                supportingText = stringResource(id = R.string.settings_developer_name, stringResource(R.string.app_name)),
                leadingIcon = Icons.Outlined.Person,
                onClick = {
                    val intent = Intent(context, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_INITIAL_URL, "https://darrindeyoung791.github.io/HabitPulse/team")
                    }
                    context.startActivity(intent)
                }
            )
            SettingsSegmentedItem(
                index = 2,
                count = 3,
                headline = stringResource(id = R.string.settings_open_source_licenses),
                supportingText = stringResource(id = R.string.settings_open_source_licenses_description),
                leadingIcon = Icons.AutoMirrored.Outlined.Article,
                onClick = onOpenLicenses
            )
        }

        SettingsSectionHeader(text = stringResource(id = R.string.settings_support_habitpulse, appName))
        SettingsSegmentedGroup(tintOffset = 3) {
            SettingsSegmentedSwitch(
                index = 0,
                count = 1,
                headline = stringResource(id = R.string.settings_support_habitpulse_switch),
                supportingText = stringResource(
                    id = if (isTalkBackEnabled) {
                        R.string.settings_support_habitpulse_switch_description_talkback
                    } else {
                        R.string.settings_support_habitpulse_switch_description
                    }
                ),
                leadingIcon = Icons.Outlined.FavoriteBorder,
                checked = showSplashAd && !isTalkBackEnabled,
                enabled = !isTalkBackEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked && isTalkBackEnabled) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.accessibility_talkback_splash_ad_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        scope.launch {
                            userPreferences.setShowSplashAd(isChecked)
                        }
                    }
                }
            )
        }

        SettingsTextLinkButton(
            text = stringResource(id = R.string.settings_app_info_button, appName),
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )
        SettingsTextLinkButton(
            text = stringResource(id = R.string.settings_github_button),
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.GITHUB_URL)
                }
                context.startActivity(intent)
            }
        )
    }

    if (showDebugConfirm) {
        AlertDialog(
            onDismissRequest = { showDebugConfirm = false },
            title = { Text(stringResource(id = R.string.settings_debug_confirm_title)) },
            text = { Text(stringResource(id = R.string.settings_debug_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDebugConfirm = false
                    onNavigateDebug()
                }) {
                    Text(stringResource(id = R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebugConfirm = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }
}