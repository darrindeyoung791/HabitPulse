package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R

internal val SettingsGroupItemGap = 4.dp
internal val SettingsBetweenGroupGap = 16.dp
private val LargeCorner = 16.dp
private val SmallCorner = 4.dp
private val PressedCorner = 20.dp

/**
 * A list item with:
 * - a neutral container color matching the home habit cards (dynamic color driven)
 * - optional leading icon in a rounded-square colored chip (low-saturation accent, outlined icon in a darker shade)
 * - headline + supporting text, left-aligned
 * - grouped corner radii: start/end corners large, touching side small
 * - on press all four corners animate slightly larger than the edge corner radius
 * - ripple feedback on press
 * - trailing chevron shown only when [showArrow] is true (i.e. the item opens a subpage)
 *
 * RTL-safe: row content order and start/end padding follow the system layout direction.
 */
@Composable
fun SettingsSegmentedItem(
    index: Int,
    count: Int,
    headline: String,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    showArrow: Boolean = true,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
) {
    val effectiveSupporting = supportingText ?: if (!enabled) stringResource(R.string.settings_coming_soon) else null
    val tint = rememberAccentTint(index)

    SettingsListSurface(
        index = index,
        count = count,
        enabled = enabled,
        onClick = onClick,
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
        trailing = trailing
    )
}

@Composable
internal fun SettingsIconChip(
    icon: ImageVector,
    tint: AccentTint,
    twoLine: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val chipSize = if (twoLine) 40.dp else 28.dp
    val iconSize = if (twoLine) 24.dp else 18.dp
    Box(
        modifier = modifier
            .size(chipSize)
            .background(
                color = if (enabled) tint.container else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(if (twoLine) 12.dp else 9.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint.content else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun SettingsListSurface(
    index: Int,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    headline: String,
    supportingText: String?,
    trailing: @Composable () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val pressed = interactionSource.collectIsPressedAsState().value
    val isPressed = pressed && enabled

    val topStart by animateDpAsState(if (isPressed) PressedCorner else if (index == 0) LargeCorner else SmallCorner)
    val topEnd by animateDpAsState(if (isPressed) PressedCorner else if (index == 0) LargeCorner else SmallCorner)
    val bottomStart by animateDpAsState(if (isPressed) PressedCorner else if (index == count - 1) LargeCorner else SmallCorner)
    val bottomEnd by animateDpAsState(if (isPressed) PressedCorner else if (index == count - 1) LargeCorner else SmallCorner)
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing()
        }
    }
}

/**
 * A non-clickable segmented surface used to place arbitrary content (e.g. a slider)
 * into a [SettingsSegmentedGroup] with the same background / corner treatment as
 * [SettingsListSurface], but without ripple, press animation, or click handling.
 */
@Composable
internal fun SettingsSegmentedBox(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val topStart = if (index == 0) LargeCorner else SmallCorner
    val topEnd = if (index == 0) LargeCorner else SmallCorner
    val bottomStart = if (index == count - 1) LargeCorner else SmallCorner
    val bottomEnd = if (index == count - 1) LargeCorner else SmallCorner
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        content()
    }
}