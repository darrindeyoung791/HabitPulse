package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.content.Context
import android.content.res.Configuration
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsGeneralScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearCookiesDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    val isTabletDevice = smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletLandscape = isTabletDevice && isLandscape
    val showForceTabletLandscapeSwitch = !isTabletLandscape

    val forceTabletLandscape by userPreferences.forceTabletLandscapeFlow.collectAsStateWithLifecycle(initialValue = false)

    var showForceTabletLandscapeDialog by remember { mutableStateOf(false) }
    var pendingForceTabletLandscapeValue by remember { mutableStateOf(false) }

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_category_general),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SectionHeader(text = stringResource(id = R.string.settings_storage))
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.settings_clear_webview_cache),
                supportingText = stringResource(id = R.string.settings_clear_webview_cache_description),
                showArrow = false,
                leadingIcon = Icons.Outlined.DeleteSweep,
                onClick = { showClearCacheDialog = true }
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_clear_webview_cookies),
                supportingText = stringResource(id = R.string.settings_clear_webview_cookies_description),
                showArrow = false,
                leadingIcon = Icons.Outlined.DeleteSweep,
                onClick = { showClearCookiesDialog = true }
            )
        }
        Text(
            text = stringResource(id = R.string.settings_storage_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )

        SectionHeader(text = stringResource(id = R.string.settings_ui_display))
        if (showForceTabletLandscapeSwitch) {
            SettingsSegmentedGroup {
                SettingsSegmentedSwitch(
                    index = 0,
                    count = 1,
                    headline = stringResource(id = R.string.settings_force_tablet_landscape),
                    supportingText = stringResource(id = R.string.settings_force_tablet_landscape_description),
                    leadingIcon = Icons.Outlined.Tablet,
                    checked = forceTabletLandscape,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            pendingForceTabletLandscapeValue = true
                            showForceTabletLandscapeDialog = true
                        } else {
                            scope.launch {
                                userPreferences.setForceTabletLandscape(false)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(id = R.string.settings_clear_webview_cache_dialog_title)) },
            text = { Text(stringResource(id = R.string.settings_clear_webview_cache_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    clearWebViewCache(context)
                }) { Text(stringResource(id = R.string.settings_clear_cache)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text(stringResource(id = R.string.settings_cancel)) }
            }
        )
    }

    if (showClearCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showClearCookiesDialog = false },
            title = { Text(stringResource(id = R.string.settings_clear_webview_cookies_dialog_title)) },
            text = { Text(stringResource(id = R.string.settings_clear_webview_cookies_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCookiesDialog = false
                    clearWebViewAll(context)
                }) { Text(stringResource(id = R.string.settings_clear_cache)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesDialog = false }) { Text(stringResource(id = R.string.settings_cancel)) }
            }
        )
    }

    if (showForceTabletLandscapeDialog) {
        AlertDialog(
            onDismissRequest = { showForceTabletLandscapeDialog = false },
            title = { Text(stringResource(id = R.string.settings_force_tablet_landscape_warning_title)) },
            text = { Text(stringResource(id = R.string.settings_force_tablet_landscape_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showForceTabletLandscapeDialog = false
                    scope.launch {
                        userPreferences.setForceTabletLandscape(pendingForceTabletLandscapeValue)
                    }
                }) { Text(stringResource(id = R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showForceTabletLandscapeDialog = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

private fun clearWebViewCache(context: Context) {
    try {
        deleteDir(context.cacheDir)
        context.externalCacheDir?.let { deleteDir(it) }
        Toast.makeText(context, context.getString(R.string.settings_clear_webview_cache_success), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
    }
}

private fun clearWebViewAll(context: Context) {
    try {
        deleteDir(context.cacheDir)
        context.externalCacheDir?.let { deleteDir(it) }
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(context, context.getString(R.string.settings_clear_webview_cookies_success), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
    }
}

private fun deleteDir(dir: File): Boolean {
    return if (dir.isDirectory) {
        dir.listFiles()?.forEach { child ->
            deleteDir(child)
        }
        dir.delete()
    } else {
        dir.delete()
    }
}
