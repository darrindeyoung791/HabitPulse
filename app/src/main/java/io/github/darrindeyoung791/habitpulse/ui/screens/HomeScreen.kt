package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    application: HabitPulseApplication,
    onHomeDataLoaded: () -> Unit = {}
) {
    // 防重复点击处理器
    val clickHandler = rememberDebounceClickHandler()
    val scope = rememberCoroutineScope()

    // 获取 ViewModel
    val viewModel: HabitViewModel = remember {
        HabitViewModel.Factory(application).create(HabitViewModel::class.java)
    }

    // 收集习惯列表状态（使用空列表作为初始值，但会通过 isLoading 控制显示）
    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // 收集加载状态
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)

    // 当数据加载完成时，通知调用方
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onHomeDataLoaded()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(id = R.string.main_calendar_view)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (clickHandler.isEnabled) {
                                scope.launch {
                                    clickHandler.processClick {
                                        onNavigateToSettings()
                                    }
                                }
                            }
                        },
                        enabled = clickHandler.isEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.main_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val newHabitLabel = stringResource(id = R.string.main_new_habit)
            ExtendedFloatingActionButton(
                onClick = {
                    if (clickHandler.isEnabled) {
                        scope.launch {
                            clickHandler.processClick {
                                onCreateHabit()
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = { Text(text = newHabitLabel) },
                modifier = Modifier
                    .semantics {
                        contentDescription = newHabitLabel
                    }
                    .alpha(if (clickHandler.isEnabled) 1f else 0.5f)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            // 加载状态：显示加载指示器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (habits.isEmpty()) {
            // 空状态：显示空状态内容
            EmptyStateContent(
                modifier = Modifier.padding(paddingValues),
                onCreateHabit = {
                    if (clickHandler.isEnabled) {
                        scope.launch {
                            clickHandler.processClick {
                                onCreateHabit()
                            }
                        }
                    }
                }
            )
        } else {
            // 有数据：显示习惯列表
            HabitListContent(
                modifier = Modifier.padding(paddingValues),
                habits = habits,
                onHabitClick = { onEditHabit(it) },
                onCheckIn = { viewModel.incrementCompletionCount(it) },
                onUndoCompletion = { viewModel.undoHabitCompletion(it) },
                onDeleteHabit = { viewModel.deleteHabit(it) }
            )
        }
    }
}

@Composable
fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onCreateHabit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.main_no_habits),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCreateHabit) {
            Text(
                text = stringResource(id = R.string.main_create_habit),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HabitListContent(
    modifier: Modifier = Modifier,
    habits: List<Habit>,
    onHabitClick: (Habit) -> Unit,
    onCheckIn: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val useStaggeredGrid = isLandscape && screenWidthDp >= 840

    if (useStaggeredGrid) {
        // Waterfall layout with synchronized scrolling for landscape tablets
        // Split habits into two columns: odd and even indices
        val column1Habits = habits.filterIndexed { index, _ -> index % 2 == 0 }
        val column2Habits = habits.filterIndexed { index, _ -> index % 2 == 1 }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    column1Habits.forEach { habit ->
                        HabitCard(
                            habit = habit,
                            onClick = { onHabitClick(habit) },
                            onCheckIn = { onCheckIn(habit) },
                            onUndoCompletion = { onUndoCompletion(habit) },
                            onEditHabit = { onHabitClick(habit) },
                            onDeleteHabit = { onDeleteHabit(habit) }
                        )
                    }
                }

                // Right column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    column2Habits.forEach { habit ->
                        HabitCard(
                            habit = habit,
                            onClick = { onHabitClick(habit) },
                            onCheckIn = { onCheckIn(habit) },
                            onUndoCompletion = { onUndoCompletion(habit) },
                            onEditHabit = { onHabitClick(habit) },
                            onDeleteHabit = { onDeleteHabit(habit) }
                        )
                    }
                }
            }
            // Add bottom spacer to prevent FAB from covering last item
            Spacer(modifier = Modifier.height(100.dp))
        }
    } else {
        // Single column layout for phones and portrait mode
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(habits, key = { it.id.toString() }) { habit ->
                HabitCard(
                    habit = habit,
                    onClick = { onHabitClick(habit) },
                    onCheckIn = { onCheckIn(habit) },
                    onUndoCompletion = { onUndoCompletion(habit) },
                    onEditHabit = { onHabitClick(habit) },
                    onDeleteHabit = { onDeleteHabit(habit) }
                )
            }
            // Add bottom spacer to prevent FAB from covering last item
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCard(
    habit: Habit,
    onClick: () -> Unit,
    onCheckIn: () -> Unit,
    onUndoCompletion: () -> Unit,
    onEditHabit: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    val reminderTimes = habit.getReminderTimesList()
    val repeatDays = habit.getRepeatDaysList()

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧内容：习惯信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 习惯名称（大字体）
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 已完成次数
                    Text(
                        text = stringResource(id = R.string.habit_card_completed_count, habit.completionCount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 重复周期和提醒时间
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Build day names map for reuse
                        val dayNames = listOf(
                            stringResource(id = R.string.habit_card_repeat_days_sunday),
                            stringResource(id = R.string.habit_card_repeat_days_monday),
                            stringResource(id = R.string.habit_card_repeat_days_tuesday),
                            stringResource(id = R.string.habit_card_repeat_days_wednesday),
                            stringResource(id = R.string.habit_card_repeat_days_thursday),
                            stringResource(id = R.string.habit_card_repeat_days_friday),
                            stringResource(id = R.string.habit_card_repeat_days_saturday)
                        )
                        val daySeparator = stringResource(id = R.string.habit_card_repeat_days_separator)
                        val repeatCycleText = when (habit.repeatCycle) {
                            RepeatCycle.DAILY -> stringResource(id = R.string.habit_card_repeat_daily)
                            RepeatCycle.WEEKLY -> {
                                val daysText = repeatDays.joinToString(daySeparator) { dayIndex ->
                                    dayNames.getOrElse(dayIndex) { "" }
                                }
                                stringResource(id = R.string.habit_card_repeat_days_format, daysText)
                            }
                        }
                        val reminderText = if (reminderTimes.isEmpty()) {
                            ""
                        } else {
                            val firstTime = reminderTimes.first()
                            if (reminderTimes.size == 1) {
                                stringResource(id = R.string.habit_card_reminder_single, firstTime)
                            } else {
                                stringResource(
                                    id = R.string.habit_card_reminder_count,
                                    firstTime,
                                    reminderTimes.size
                                )
                            }
                        }
                        val repeatInfoText = if (reminderText.isNotEmpty()) {
                            "$repeatCycleText · $reminderText"
                        } else {
                            repeatCycleText
                        }
                        Text(
                            text = repeatInfoText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.combinedClickable(
                                onClick = { showReminderDialog = true },
                                onLongClick = { showMenu = true }
                            )
                        )
                    }

                    // 备注信息（仅当有备注时显示）
                    if (habit.notes.isNotBlank()) {
                        Text(
                            text = formatNotesPreview(habit.notes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.combinedClickable(
                                onClick = { showNotesDialog = true },
                                onLongClick = { showMenu = true }
                            )
                        )
                    }
                }

                // 右侧：打卡按钮（垂直居中）
                CheckInButton(
                    onClick = onCheckIn
                )
            }
        }

        // 长按下拉菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 撤销打卡（当已完成次数大于 0 时显示）
            if (habit.completionCount > 0) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = R.string.habit_card_menu_undo))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        onUndoCompletion()
                        showMenu = false
                    }
                )
            }

            // 编辑
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.habit_card_menu_edit))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onEditHabit()
                    showMenu = false
                }
            )

            // 删除
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.habit_card_menu_delete))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onDeleteHabit()
                    showMenu = false
                }
            )
        }
    }

    // 提醒详情对话框
    if (showReminderDialog) {
        ReminderDetailDialog(
            reminderTimes = reminderTimes,
            repeatCycle = habit.repeatCycle,
            repeatDays = repeatDays,
            onDismiss = { showReminderDialog = false }
        )
    }

    // 备注详情对话框
    if (showNotesDialog) {
        NotesDetailDialog(
            notes = habit.notes,
            onDismiss = { showNotesDialog = false }
        )
    }
}

private fun formatNotesPreview(notes: String): String {
    val lines = notes.lines().take(3).joinToString("\n")
    return if (notes.lines().size > 3) {
        "$lines…"
    } else {
        lines
    }
}

@Composable
fun CheckInButton(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cornerRadius by animateFloatAsState(
        targetValue = if (isPressed) 20f else 12f,
        animationSpec = tween(200),
        label = "cornerRadius"
    )

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.changes.any { it.pressed }
                    }
                }
            },
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}

@Composable
fun ReminderDetailDialog(
    reminderTimes: List<String>,
    repeatCycle: RepeatCycle,
    repeatDays: List<Int>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.habit_card_reminder_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.75f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 重复周期
                // Build day names map for reuse
                val dayNames = listOf(
                    stringResource(id = R.string.habit_card_repeat_days_sunday),
                    stringResource(id = R.string.habit_card_repeat_days_monday),
                    stringResource(id = R.string.habit_card_repeat_days_tuesday),
                    stringResource(id = R.string.habit_card_repeat_days_wednesday),
                    stringResource(id = R.string.habit_card_repeat_days_thursday),
                    stringResource(id = R.string.habit_card_repeat_days_friday),
                    stringResource(id = R.string.habit_card_repeat_days_saturday)
                )
                val daySeparator = stringResource(id = R.string.habit_card_repeat_days_separator)
                val repeatCycleText = when (repeatCycle) {
                    RepeatCycle.DAILY -> stringResource(id = R.string.habit_card_repeat_daily)
                    RepeatCycle.WEEKLY -> {
                        val daysText = repeatDays.joinToString(daySeparator) { dayIndex ->
                            dayNames.getOrElse(dayIndex) { "" }
                        }
                        stringResource(id = R.string.habit_card_repeat_days_format, daysText)
                    }
                }
                Text(
                    text = repeatCycleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 提醒时间列表
                if (reminderTimes.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.create_habit_reminder_time_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        reminderTimes.forEach { time ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.create_habit_no_reminder_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.habit_card_reminder_dialog_dismiss))
            }
        }
    )
}

@Composable
fun NotesDetailDialog(
    notes: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.habit_card_notes_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.75f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.habit_card_notes_dialog_dismiss))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HabitPulseTheme {
        HomeScreen(
            onCreateHabit = {},
            onNavigateToSettings = {},
            onEditHabit = {},
            application = HabitPulseApplication(),
            onHomeDataLoaded = {}
        )
    }
}

@Preview
@Composable
fun HabitCardPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "每天喝水",
                completedToday = false,
                completionCount = 15,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"08:00\",\"12:00\",\"20:00\"]",
                notes = "记得每次喝水时要慢慢喝\n不要一口气喝完\n最好喝温水"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}

@Preview
@Composable
fun HabitCardCompletedPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "晨跑",
                completedToday = true,
                completionCount = 30,
                repeatCycle = RepeatCycle.WEEKLY,
                repeatDays = "[1,3,5]",
                reminderTimes = "[\"06:00\"]",
                notes = "跑步前记得热身\n跑完后要拉伸"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}

@Preview
@Composable
fun HabitCardWithNotesPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "阅读",
                completedToday = false,
                completionCount = 5,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"21:00\"]",
                notes = "每天至少读 30 分钟\n记录读书笔记\n分享读书心得"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}
