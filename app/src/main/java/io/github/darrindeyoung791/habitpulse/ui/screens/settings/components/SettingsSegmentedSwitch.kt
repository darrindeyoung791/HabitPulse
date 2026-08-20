package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.darrindeyoung791.habitpulse.R

/**
 * A switch list item. The whole row toggles the switch; the switch uses the
 * theme accent color and shares the row's interaction source so pressing the
 * row also shows the switch's pressed state.
 */
@Composable
fun SettingsSegmentedSwitch(
    index: Int,
    count: Int,
    headline: String,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    tintIndex: Int = index,
    onCheckedChange: (Boolean) -> Unit
) {
    val effectiveSupporting = supportingText ?: if (!enabled) stringResource(R.string.settings_coming_soon) else null
    val tint = rememberAccentTint(tintIndex)
    val interactionSource = remember { MutableInteractionSource() }

    SettingsListSurface(
        index = index,
        count = count,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        interactionSource = interactionSource,
        leading = {
            if (leadingIcon != null) {
                SettingsIconChip(
                    icon = leadingIcon,
                    tint = tint,
                    twoLine = effectiveSupporting != null,
                    enabled = enabled
                )
            }
        },
        headline = headline,
        supportingText = effectiveSupporting,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                interactionSource = interactionSource,
                colors = ThemeSwitchColors()
            )
        }
    )
}

@Composable
internal fun ThemeSwitchColors(): SwitchColors {
    val scheme = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedThumbColor = scheme.onPrimary,
        checkedTrackColor = scheme.primary,
        checkedBorderColor = scheme.primary,
        uncheckedThumbColor = scheme.outline,
        uncheckedTrackColor = scheme.surfaceContainerHighest,
        uncheckedBorderColor = scheme.outline,
        disabledCheckedThumbColor = scheme.onPrimary.copy(alpha = 0.38f),
        disabledCheckedTrackColor = scheme.primary.copy(alpha = 0.38f),
        disabledUncheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledUncheckedTrackColor = scheme.surfaceContainerHighest.copy(alpha = 0.6f)
    )
}