package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsAIScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onNavigateProvider: () -> Unit
) {
    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_category_ai),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.settings_configure_provider),
                supportingText = stringResource(id = R.string.settings_configure_provider_description),
                leadingIcon = Icons.Outlined.AutoAwesome,
                onClick = onNavigateProvider
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_memory),
                supportingText = stringResource(id = R.string.settings_memory_description),
                enabled = false,
                showArrow = false,
                leadingIcon = Icons.Outlined.Memory,
                onClick = {}
            )
        }
    }
}