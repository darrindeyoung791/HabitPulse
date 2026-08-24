package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.ui.screens.dnd.DndRangeSlider
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsExpandableListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSectionHeader
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedBox
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedNumberItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsTextLinkButton
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.ThemeSwitchColors
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUICatalogScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    var firstSwitchOn by remember { mutableStateOf(true) }
    var secondSwitchOn by remember { mutableStateOf(false) }
    var expandableOpen by remember { mutableStateOf(false) }
    var selectedNumberIndex by remember { mutableIntStateOf(1) }
    var sliderExpanded by remember { mutableStateOf(true) }
    var sliderStart by remember { mutableStateOf("23:00") }
    var sliderEnd by remember { mutableStateOf("07:00") }
    var showDialog by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "UI catalog",
        onBack = onBack,
        onHelp = onOpenHelp,
        reserveFabSpace = true,
        floatingActionButton = {
            val fabInteractionSource = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                interactionSource = fabInteractionSource,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null
                    )
                },
                text = { Text(text = "Extended FAB") }
            )
            PressVibrationFeedback(interactionSource = fabInteractionSource)
        }
    ) {
        SettingsSectionHeader(text = "List items")
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = "Standard list item",
                supportingText = "Leading icon chip · headline · supporting text · trailing chevron",
                leadingIcon = Icons.Outlined.Info,
                onClick = {}
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = "Action item (no arrow)",
                supportingText = "showArrow = false — performs an action instead of navigating",
                leadingIcon = Icons.Outlined.TouchApp,
                showArrow = false,
                onClick = {}
            )
        }

        SettingsSectionHeader(text = "Grouping and corner behavior")
        SettingsSegmentedGroup(tintOffset = 2) {
            SettingsSegmentedItem(
                index = 0,
                count = 3,
                headline = "First item in group",
                supportingText = "Top corners 16dp; icon palette continues from the previous group (tintOffset)",
                leadingIcon = Icons.Outlined.Star,
                showArrow = false,
                onClick = {}
            )
            SettingsSegmentedItem(
                index = 1,
                count = 3,
                headline = "Middle item",
                supportingText = "All corners 4dp; pressing animates every corner to 20dp",
                leadingIcon = Icons.Outlined.Favorite,
                showArrow = false,
                onClick = {}
            )
            SettingsSegmentedItem(
                index = 2,
                count = 3,
                headline = "Last item in group",
                supportingText = "Bottom corners 16dp; ripple is clipped to the animated shape",
                leadingIcon = Icons.Outlined.CheckCircle,
                showArrow = false,
                onClick = {}
            )
        }

        SettingsSectionHeader(text = "Item states")
        SettingsSegmentedGroup(tintOffset = 5) {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = "Disabled placeholder",
                leadingIcon = Icons.Outlined.Lock,
                enabled = false,
                showArrow = false,
                onClick = {}
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = "Selected state",
                supportingText = "secondaryContainer surface with onSecondaryContainer content",
                selected = true,
                showArrow = false,
                onClick = {}
            )
        }

        SettingsSectionHeader(text = "Switch rows")
        SettingsSegmentedGroup {
            SettingsSegmentedSwitch(
                index = 0,
                count = 2,
                headline = "Switch row (on)",
                supportingText = "The whole row toggles; the switch shares the row's interaction source",
                checked = firstSwitchOn,
                onCheckedChange = { firstSwitchOn = it }
            )
            SettingsSegmentedSwitch(
                index = 1,
                count = 2,
                headline = "Switch row (off)",
                checked = secondSwitchOn,
                onCheckedChange = { secondSwitchOn = it }
            )
        }

        SettingsSectionHeader(text = "Number select rows")
        SettingsSegmentedGroup {
            SettingsSegmentedNumberItem(
                index = 0,
                count = 3,
                number = 1,
                headline = "Option one",
                supportingText = "Two-line layout keeps the number column aligned",
                selected = selectedNumberIndex == 0,
                onClick = { selectedNumberIndex = 0 },
                trailing = {
                    RadioButton(
                        selected = selectedNumberIndex == 0,
                        onClick = null
                    )
                }
            )
            SettingsSegmentedNumberItem(
                index = 1,
                count = 3,
                number = 2,
                headline = "Option two",
                supportingText = "Selected row uses secondaryContainer tint",
                selected = selectedNumberIndex == 1,
                onClick = { selectedNumberIndex = 1 },
                trailing = {
                    RadioButton(
                        selected = selectedNumberIndex == 1,
                        onClick = null
                    )
                }
            )
            SettingsSegmentedNumberItem(
                index = 2,
                count = 3,
                number = 3,
                headline = "Option three",
                supportingText = "Trailing radio button mirrors the selection state",
                selected = selectedNumberIndex == 2,
                onClick = { selectedNumberIndex = 2 },
                trailing = {
                    RadioButton(
                        selected = selectedNumberIndex == 2,
                        onClick = null
                    )
                }
            )
        }

        SettingsSectionHeader(text = "Expandable list item")
        SettingsSegmentedGroup {
            SettingsExpandableListSurface(
                index = 0,
                count = 1,
                expanded = expandableOpen,
                onToggle = { expandableOpen = !expandableOpen },
                headline = "Expandable list item",
                supportingText = "Header and body share one surfaceContainer surface",
                badgeContent = {
                    CatalogPillBadge(text = "badge")
                }
            ) {
                Text(
                    text = "The ripple and press animation span the whole surface, header and expanded body alike. The trailing chevron rotates 180 degrees while expanding; badgeContent stays visible in both states.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
                )
            }
        }

        SettingsSectionHeader(text = "Sliders")
        SettingsSegmentedGroup {
            val sliderInteractionSource = remember { MutableInteractionSource() }
            SettingsExpandableListSurface(
                index = 0,
                count = 1,
                expanded = sliderExpanded,
                onToggle = { sliderExpanded = !sliderExpanded },
                headline = "Switch-expanded slider",
                supportingText = "Do-not-disturb pattern: the range slider lives inside the same surface as its switch row — no gap between them",
                interactionSource = sliderInteractionSource,
                trailing = {
                    Switch(
                        checked = sliderExpanded,
                        onCheckedChange = null,
                        interactionSource = sliderInteractionSource,
                        colors = ThemeSwitchColors()
                    )
                }
            ) {
                DndRangeSlider(
                    startTime = sliderStart,
                    endTime = sliderEnd,
                    onStartTimeChange = { sliderStart = it },
                    onEndTimeChange = { sliderEnd = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        SettingsSectionHeader(text = "Non-clickable segmented surface")
        SettingsSegmentedGroup {
            SettingsSegmentedBox(index = 0, count = 1) {
                Text(
                    text = "SettingsSegmentedBox places non-interactive content inside a group with identical corners and surfaceContainer background — no ripple, no press animation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        SettingsSectionHeader(text = "Dialogs")
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 1,
                headline = "Show sample dialog",
                supportingText = "AlertDialog with confirm and dismiss actions; the extended FAB opens the same one",
                showArrow = false,
                onClick = { showDialog = true }
            )
        }

        SettingsSectionHeader(text = "Text link buttons")
        SettingsTextLinkButton(
            text = "Compact text link button (no 48dp minimum touch target)",
            onClick = {}
        )
        SettingsTextLinkButton(
            text = "Stacked links sit tightly together",
            onClick = {}
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Sample dialog") },
            text = {
                Text(
                    text = "Standard settings AlertDialog: title, message body and confirm / dismiss TextButtons."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CatalogPillBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
