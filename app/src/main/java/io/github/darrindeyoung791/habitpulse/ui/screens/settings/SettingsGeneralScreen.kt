package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.content.Context
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.LargeCorner
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.PressedCorner
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSectionHeader
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SmallCorner
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import io.github.darrindeyoung791.habitpulse.utils.AppLocaleManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenFontScale: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearCookiesDialog by remember { mutableStateOf(false) }

    val isTabletLandscape = rememberDeviceFormInfo().isTabletLandscape
    val showForceTabletLandscapeSwitch = !isTabletLandscape

    val forceTabletLandscape by userPreferences.forceTabletLandscapeFlow.collectAsStateWithLifecycle(initialValue = false)
    val hapticsEnabled by userPreferences.hapticsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val darkMode by userPreferences.darkModeFlow.collectAsStateWithLifecycle(initialValue = 0)

    var showForceTabletLandscapeDialog by remember { mutableStateOf(false) }
    var pendingForceTabletLandscapeValue by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = stringResource(id = R.string.settings_category_general),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        // 语言组（独立一组，带段标题）
        SettingsSectionHeader(text = stringResource(id = R.string.settings_language))
        val showLanguageItem = AppLocaleManager.isPerAppLanguageSupported()
        if (showLanguageItem) {
            SettingsSegmentedGroup {
                val currentLanguageText = AppLocaleManager
                    .getCurrentAppLocale(context)
                    ?.toLanguageTag()
                    ?.let { AppLocaleManager.labelRes(it) }
                    ?.let { stringResource(id = it) }
                SettingsSegmentedItem(
                    index = 0,
                    count = 1,
                    headline = stringResource(id = R.string.settings_language),
                    supportingText = currentLanguageText
                        ?: if (!AppLocaleManager.isSystemLanguageSupported(context)) {
                            stringResource(
                                id = R.string.language_follow_system_unsupported,
                                stringResource(id = R.string.app_name)
                            )
                        } else {
                            AppLocaleManager.systemLocaleLabelRes(context)
                                ?.let { stringResource(id = it) }
                                ?: stringResource(id = R.string.settings_language_system_default)
                        },
                    leadingIcon = Icons.Outlined.Translate,
                    showArrow = true,
                    onClick = onOpenLanguage
                )
            }
        }

        // 显示与触感组
        SettingsSectionHeader(text = stringResource(id = R.string.settings_ui_display))
        val uiDisplayItemCount =
            1 + (if (showForceTabletLandscapeSwitch) 1 else 0) + 1
        if (uiDisplayItemCount > 0) {
            SettingsSegmentedGroup {
                // 深色模式按钮组
                DarkModeButtonGroup(
                    index = 0,
                    count = uiDisplayItemCount,
                    selectedMode = darkMode,
                    onModeSelected = { mode ->
                        scope.launch {
                            userPreferences.setDarkMode(mode)
                        }
                    }
                )
                SettingsSegmentedItem(
                    index = 1,
                    count = uiDisplayItemCount,
                    headline = stringResource(id = R.string.settings_font_scale),
                    supportingText = stringResource(id = R.string.settings_font_scale_description),
                    leadingIcon = Icons.Outlined.FormatSize,
                    showArrow = true,
                    onClick = onOpenFontScale
                )
                if (showForceTabletLandscapeSwitch) {
                    SettingsSegmentedSwitch(
                        index = 2,
                        count = uiDisplayItemCount,
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
                val disableVibrations = !hapticsEnabled
                SettingsSegmentedSwitch(
                    index = uiDisplayItemCount - 1,
                    count = uiDisplayItemCount,
                    headline = stringResource(id = R.string.settings_haptic_feedback),
                    supportingText = stringResource(id = R.string.settings_haptic_feedback_description),
                    leadingIcon = Icons.Outlined.Vibration,
                    checked = disableVibrations,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            userPreferences.setHapticsEnabled(!isChecked)
                        }
                    }
                )
            }
        }

        SettingsSectionHeader(text = stringResource(id = R.string.settings_storage))
        SettingsSegmentedGroup(tintOffset = 4) {
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
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

/**
 * 深色模式 MD3 按钮组列表项
 *
 * 第一行：标题「深色模式」+ 前置图标
 * 第二行：三个 MD3 风格按钮（关闭 / 开启 / 跟随系统），左对齐
 *
 * 按钮交互：
 * - 按下时圆角从 LargeCorner(16dp) 动画到 PressedCorner(20dp)
 * - 松手回弹
 * - 遵循列表项按压震动规范（25ms）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DarkModeButtonGroup(
    index: Int,
    count: Int,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    val topStart by animateDpAsState(if (index == 0) LargeCorner else SmallCorner)
    val topEnd by animateDpAsState(if (index == 0) LargeCorner else SmallCorner)
    val bottomStart by animateDpAsState(if (index == count - 1) LargeCorner else SmallCorner)
    val bottomEnd by animateDpAsState(if (index == count - 1) LargeCorner else SmallCorner)
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val headlineColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 前置图标 chip
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = headlineColor
                )
                // MD3 按钮组（允许换行）
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkModeButton(
                        label = stringResource(id = R.string.settings_dark_mode_off),
                        selected = selectedMode == 2,
                        onClick = { onModeSelected(2) }
                    )
                    DarkModeButton(
                        label = stringResource(id = R.string.settings_dark_mode_on),
                        selected = selectedMode == 1,
                        onClick = { onModeSelected(1) }
                    )
                    DarkModeButton(
                        label = stringResource(id = R.string.settings_dark_mode_follow_system),
                        selected = selectedMode == 0,
                        onClick = { onModeSelected(0) }
                    )
                }
            }
        }
    }
}

/**
 * 单个切换按钮
 *
 * - 未选中态：pill 胶囊形（100% 圆角）+ surfaceContainerHighest 底色
 * - 选中态：8dp 圆角 + primary 底色（与开关强调色一致）+ 勾号图标
 * - 按下时圆角缩小（未选中 100%→16dp，选中 8dp→4dp）
 * - 遵循列表项按压震动规范
 */
@Composable
private fun DarkModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isPressed = pressed

    PressVibrationFeedback(interactionSource = interactionSource)

    // 未选中默认 pill 形，选中默认 8dp；按下时都缩小到 4dp
    val defaultCorner = if (selected) 8.dp else 100.dp
    val pressedCorner = 4.dp
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) pressedCorner else defaultCorner,
        label = "buttonCorner"
    )
    val shape = RoundedCornerShape(cornerRadius)

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
