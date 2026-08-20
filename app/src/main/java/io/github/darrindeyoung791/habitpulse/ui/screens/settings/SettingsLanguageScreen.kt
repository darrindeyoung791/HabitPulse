package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.utils.AppLocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLanguageScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val currentTag = AppLocaleManager.getCurrentAppLocale(context)?.toLanguageTag()

    SettingsScaffold(
        title = stringResource(id = R.string.settings_language),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 1,
                headline = stringResource(id = R.string.settings_language_system_default),
                supportingText = if (AppLocaleManager.isSystemLanguageSupported(context)) {
                    AppLocaleManager.systemLocaleLabelRes(context)
                        ?.let { stringResource(id = it) }
                        ?: AppLocaleManager.systemLocaleSelfName(context)
                } else {
                    stringResource(
                        id = R.string.language_follow_system_unsupported,
                        stringResource(id = R.string.app_name)
                    )
                },
                enabled = AppLocaleManager.isSystemLanguageSupported(context),
                showArrow = false,
                onClick = {
                    onBack()
                    AppLocaleManager.setAppLanguage(context, null)
                },
                trailing = {
                    RadioButton(
                        selected = currentTag == null,
                        onClick = null
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
        SettingsSegmentedGroup {
            AppLocaleManager.supportedTags.filterNotNull().forEachIndexed { index, tag ->
                SettingsSegmentedItem(
                    index = index,
                    count = AppLocaleManager.supportedTags.size - 1,
                    headline = stringResource(id = AppLocaleManager.labelRes(tag)),
                    showArrow = false,
                    onClick = {
                        onBack()
                        AppLocaleManager.setAppLanguage(context, tag)
                    },
                    trailing = {
                        RadioButton(
                            selected = tag == currentTag,
                            onClick = null
                        )
                    }
                )
            }
        }
    }
}
