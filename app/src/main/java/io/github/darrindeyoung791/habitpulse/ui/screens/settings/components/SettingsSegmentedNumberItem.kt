package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A selectable list item whose leading shows the item's number order (1, 2, 3, ...)
 * as plain text instead of a colored icon chip. Selection is indicated by tinting
 * the whole row with the theme color ([MaterialTheme.colorScheme.primaryContainer])
 * rather than a radio button or accent chip.
 *
 * @param number 1-based number displayed in the leading
 * @param selected whether this item is currently selected
 */
@Composable
fun SettingsSegmentedNumberItem(
    index: Int,
    count: Int,
    number: Int,
    headline: String,
    supportingText: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val twoLine = supportingText != null

    SettingsListSurface(
        index = index,
        count = count,
        enabled = true,
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        leading = {
            Box(
                modifier = Modifier.size(if (twoLine) 40.dp else 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        headline = headline,
        supportingText = supportingText,
        trailing = trailing
    )
}
