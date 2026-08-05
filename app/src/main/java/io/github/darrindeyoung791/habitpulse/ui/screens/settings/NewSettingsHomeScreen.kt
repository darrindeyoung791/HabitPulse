package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsHomeScreen(
    onBack: () -> Unit,
    onNavigateAI: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateGeneral: () -> Unit,
    onNavigateAbout: () -> Unit,
    onOpenHelp: () -> Unit
) {
    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_title),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 5,
                headline = stringResource(id = R.string.settings_category_ai),
                supportingText = stringResource(id = R.string.settings_category_ai_description),
                leadingIcon = Icons.Outlined.AutoAwesome,
                onClick = onNavigateAI
            )
            SettingsSegmentedItem(
                index = 1,
                count = 5,
                headline = stringResource(id = R.string.settings_category_lan),
                enabled = false,
                showArrow = false,
                leadingIcon = Icons.Outlined.Lan,
                onClick = {}
            )
            SettingsSegmentedItem(
                index = 2,
                count = 5,
                headline = stringResource(id = R.string.settings_notifications),
                supportingText = stringResource(id = R.string.settings_notifications_description),
                leadingIcon = Icons.Outlined.Notifications,
                onClick = onNavigateNotifications
            )
            SettingsSegmentedItem(
                index = 3,
                count = 5,
                headline = stringResource(id = R.string.settings_category_general),
                supportingText = stringResource(id = R.string.settings_category_general_description),
                leadingIcon = Icons.Outlined.Settings,
                onClick = onNavigateGeneral
            )
            SettingsSegmentedItem(
                index = 4,
                count = 5,
                headline = stringResource(id = R.string.settings_about),
                supportingText = stringResource(id = R.string.settings_about_description),
                leadingIcon = Icons.Outlined.Info,
                onClick = onNavigateAbout
            )
        }
        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
        SettingsSegmentedGroup(tintOffset = 5) {
            SettingsSegmentedItem(
                index = 0,
                count = 1,
                headline = stringResource(id = R.string.settings_help_button),
                supportingText = stringResource(id = R.string.settings_help_button_description),
                leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onOpenHelp
            )
        }
    }
}
