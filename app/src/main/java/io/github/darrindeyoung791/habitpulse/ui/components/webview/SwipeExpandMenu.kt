package io.github.darrindeyoung791.habitpulse.ui.components.webview

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import io.github.darrindeyoung791.habitpulse.R

@Composable
fun WebViewMenuButton(
    onRefresh: () -> Unit,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        androidx.compose.material3.IconButton(
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.webview_menu)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 0.dp)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webview_menu_refresh)) },
                onClick = {
                    expanded = false
                    onRefresh()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webview_menu_open_in_browser)) },
                onClick = {
                    expanded = false
                    onOpenInBrowser()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.OpenInBrowser,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
