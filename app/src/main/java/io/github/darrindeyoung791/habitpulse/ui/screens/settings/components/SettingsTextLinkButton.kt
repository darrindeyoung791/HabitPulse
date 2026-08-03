package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * A compact text link button used on settings pages. Unlike the default
 * [TextButton], the minimum interactive component size (48dp) is removed so
 * stacked links sit tightly together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        TextButton(
            onClick = onClick,
            modifier = modifier,
            shape = RectangleShape,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * A compact text link button with a string resource id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextLinkButtonRes(
    textRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsTextLinkButton(
        text = stringResource(id = textRes),
        onClick = onClick,
        modifier = modifier
    )
}
