package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberNavigationGuard
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * TimePicker 对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    currentTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = false
    )
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onConfirmRequest(selectedTime)
                }
            ) {
                Text(text = stringResource(id = R.string.create_habit_time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(id = R.string.create_habit_time_picker_dismiss))
            }
        },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                        TimePickerDisplayMode.Input
                    } else {
                        TimePickerDisplayMode.Picker
                    }
                },
                displayMode = displayMode
            )
        }
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }
    }
}

/**
 * 习惯编辑模式
 */
enum class EditMode {
    CREATE,  // 新建习惯模式
    EDIT     // 编辑习惯模式
}

/**
 * 习惯重复周期
 */
enum class RepeatCycle {
    DAILY,   // 每日
    WEEKLY   // 每周
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreationScreen(
    onNavigateBack: () -> Unit,
    editMode: EditMode = EditMode.CREATE,
    navController: androidx.navigation.NavHostController? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var habitName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var repeatCycle by remember { mutableStateOf(RepeatCycle.DAILY) }
    var reminderTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var currentTimePickerTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    var isReminderExpanded by remember { mutableStateOf(false) }

    // 防重复点击处理器，防止快速连续点击导致多次导航
    val clickHandler = rememberDebounceClickHandler()
    // 导航保护器，防止返回到主页以上
    val navigationGuard = navController?.let { rememberNavigationGuard(it) }

    val titleRes = when (editMode) {
        EditMode.CREATE -> R.string.create_habit_title
        EditMode.EDIT -> R.string.edit_habit_title
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    val scope = rememberCoroutineScope()
                    TextButton(
                        onClick = {
                            if (clickHandler.isEnabled) {
                                scope.launch {
                                    clickHandler.processClick {
                                        // 优先使用导航保护器进行安全返回
                                        if (navigationGuard != null) {
                                            navigationGuard.safePopBackStack()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = clickHandler.isEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_cancel_button),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    TextButton(
                        onClick = {
                            if (clickHandler.isEnabled && habitName.isNotBlank() && !isSaving) {
                                scope.launch {
                                    clickHandler.processClick {
                                        // TODO: Implement save logic
                                        isSaving = true
                                        // 优先使用导航保护器进行安全返回
                                        if (navigationGuard != null) {
                                            navigationGuard.safePopBackStack()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = habitName.isNotBlank() && !isSaving && clickHandler.isEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_save_button),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Repeat cycle selection - Button group with custom shapes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((-1).dp)
            ) {
                RepeatCycle.values().forEachIndexed { index, cycle ->
                    val isSelected = repeatCycle == cycle
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    
                    // 按压时圆角变大动画
                    val cornerSize = animateDpAsState(
                        targetValue = if (isPressed) 24.dp else 8.dp,
                        label = "cornerSize"
                    )
                    
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { repeatCycle = cycle },
                        shape = RoundedCornerShape(
                            topStart = if (index == 0) cornerSize.value else 0.dp,
                            topEnd = if (index == RepeatCycle.values().lastIndex) cornerSize.value else 0.dp,
                            bottomStart = if (index == 0) cornerSize.value else 0.dp,
                            bottomEnd = if (index == RepeatCycle.values().lastIndex) cornerSize.value else 0.dp
                        ),
                        color = backgroundColor,
                        contentColor = contentColor
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (cycle) {
                                    RepeatCycle.DAILY -> stringResource(id = R.string.create_habit_daily_option)
                                    RepeatCycle.WEEKLY -> stringResource(id = R.string.create_habit_weekly_option)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Habit name input field
            OutlinedTextField(
                value = habitName,
                onValueChange = { habitName = it },
                label = {
                    Text(text = stringResource(id = R.string.create_habit_habit_name_label))
                },
                placeholder = {
                    Text(text = stringResource(id = R.string.create_habit_habit_name_placeholder))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Close keyboard when done is pressed
                    }
                ),
            )

            // Reminder time section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header row with add button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_reminder_time_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = {
                                currentTimePickerTime = java.time.LocalTime.now()
                                showTimePicker = true
                            }
                        ) {
                            Text(text = stringResource(id = R.string.create_habit_add_reminder_button))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Reminder status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (reminderTimes.isEmpty()) {
                                stringResource(id = R.string.create_habit_no_reminder_set)
                            } else if (isReminderExpanded) {
                                stringResource(id = R.string.create_habit_reminder_set_count, reminderTimes.size)
                            } else {
                                stringResource(id = R.string.create_habit_reminder_set_count, reminderTimes.size)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (reminderTimes.isNotEmpty()) {
                            TextButton(
                                onClick = { isReminderExpanded = !isReminderExpanded }
                            ) {
                                Text(
                                    text = if (isReminderExpanded) {
                                        stringResource(id = R.string.create_habit_collapse_button)
                                    } else {
                                        stringResource(id = R.string.create_habit_expand_button)
                                    }
                                )
                                Icon(
                                    imageVector = if (isReminderExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = if (isReminderExpanded) "收起" else "展开",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Expandable reminder times list
                    AnimatedVisibility(
                        visible = isReminderExpanded && reminderTimes.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderTimes.forEachIndexed { index, time ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            reminderTimes = reminderTimes.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "删除提醒",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TimePicker Dialog
            if (showTimePicker) {
                TimePickerDialog(
                    currentTime = currentTimePickerTime,
                    onDismissRequest = { showTimePicker = false },
                    onConfirmRequest = { selectedTime ->
                        val timeString = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                        if (reminderTimes.contains(timeString)) {
                            android.widget.Toast.makeText(context, context.getString(R.string.create_habit_duplicate_time), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            reminderTimes = reminderTimes + timeString
                        }
                        showTimePicker = false
                    }
                )
            }

            // TODO: Add more habit creation fields here
            // - Reminder times
            // - Notes
            // - Supervision settings

            Spacer(modifier = Modifier.weight(1f))

            // Helper text
            Text(
                text = "更多设置将在此处添加...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenPreview() {
    HabitPulseTheme {
        HabitCreationScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenDarkPreview() {
    HabitPulseTheme(darkTheme = true) {
        HabitCreationScreen(onNavigateBack = {})
    }
}