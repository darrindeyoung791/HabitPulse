package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.OpenSourceLicensesActivity
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.WebViewActivity
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsAboutDetailScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onNavigateDebug: () -> Unit
) {
    val context = LocalContext.current

    var versionTapCount by remember { mutableStateOf(0) }
    var versionTapStartTime by remember { mutableStateOf(0L) }

    val TAP_TIME_WINDOW = 5_000L
    val TAP_COUNT_THRESHOLD = 5

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = packageInfo.versionName ?: "1.0.0"
            val isDebug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val buildType = if (isDebug) " (Debug)" else " (Release)"
            "$version$buildType"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val appName = stringResource(id = R.string.app_name)

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_about),
        onBack = onBack,
        onHelp = onOpenHelp
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
                            onNavigateDebug()
                        }
                    }
                }
            )
            SettingsSegmentedItem(
                index = 1,
                count = 3,
                headline = stringResource(id = R.string.settings_developer),
                supportingText = stringResource(id = R.string.settings_developer_name),
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
                onClick = {
                    val intent = Intent(context, OpenSourceLicensesActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }

        TextButton(
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                    context.startActivity(intent)
                }
            },
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(text = stringResource(id = R.string.settings_app_info_button, appName), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.GITHUB_URL)
                }
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(stringResource(id = R.string.settings_github_button), style = MaterialTheme.typography.bodyMedium)
        }
    }
}