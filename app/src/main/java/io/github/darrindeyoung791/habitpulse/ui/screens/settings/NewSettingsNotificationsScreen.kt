package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.WebViewActivity
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsTextLinkButton
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.NotificationPermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsNotificationsScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onNavigateReminder: () -> Unit,
    onNavigateTemplate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val persistentNotification by userPreferences.persistentNotificationFlow.collectAsStateWithLifecycle(initialValue = false)

    var hasNotificationPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
        }
    }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                userPreferences.setPersistentNotification(true)
                NotificationHelper.createNotificationChannel(context)
                ForegroundNotificationService.toggleService(context, true)
            }
        }
    }

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_notifications),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            if (hasNotificationPermission) {
                SettingsSegmentedSwitch(
                    index = 0,
                    count = 3,
                    headline = stringResource(id = R.string.settings_persistent_notification),
                    supportingText = stringResource(id = R.string.settings_persistent_notification_description),
                    leadingIcon = Icons.Outlined.Notifications,
                    checked = persistentNotification,
                    onCheckedChange = { isChecked ->
                        if (!isChecked) {
                            scope.launch {
                                userPreferences.setPersistentNotification(false)
                                ForegroundNotificationService.toggleService(context, false)
                            }
                        } else {
                            val application = context.applicationContext as HabitPulseApplication
                            val isLimitedMode = application.habitViewModel.isLimitedMode.value
                            if (isLimitedMode) {
                                scope.launch {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                scope.launch {
                                    userPreferences.setPersistentNotification(true)
                                    NotificationHelper.createNotificationChannel(context)
                                    ForegroundNotificationService.toggleService(context, true)
                                }
                            }
                        }
                    }
                )
            } else {
                SettingsSegmentedItem(
                    index = 0,
                    count = 3,
                    headline = stringResource(id = R.string.settings_persistent_notification),
                    supportingText = stringResource(id = R.string.settings_persistent_notification_authorize),
                    showArrow = false,
                    leadingIcon = Icons.Outlined.Notifications,
                    onClick = {
                        val activity = context as? ComponentActivity
                        val shouldShowRationale = activity?.let {
                            NotificationHelper.shouldShowPermissionRationale(it)
                        } ?: true
                        if (shouldShowRationale) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationPermissionHelper.openAppSettings(context)
                        }
                    }
                )
            }
            SettingsSegmentedItem(
                index = 1,
                count = 3,
                headline = stringResource(id = R.string.settings_reminder),
                supportingText = stringResource(id = R.string.settings_reminder_description),
                leadingIcon = Icons.Outlined.Alarm,
                onClick = onNavigateReminder
            )
            SettingsSegmentedItem(
                index = 2,
                count = 3,
                headline = stringResource(id = R.string.notification_template_settings_title),
                supportingText = stringResource(id = R.string.notification_template_settings_desc),
                leadingIcon = Icons.AutoMirrored.Outlined.Article,
                onClick = onNavigateTemplate
            )
        }

        SettingsTextLinkButton(
            text = stringResource(id = R.string.reminder_settings_system_settings),
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }
        )
        SettingsTextLinkButton(
            text = stringResource(id = R.string.reminder_settings_background_keepalive),
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.REMINDER_HELP_URL)
                }
                context.startActivity(intent)
            }
        )
    }
}