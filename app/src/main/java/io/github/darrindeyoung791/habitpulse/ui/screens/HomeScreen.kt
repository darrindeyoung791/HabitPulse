package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
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
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    application: HabitPulseApplication,
    onSplashScreenReady: () -> Unit = {}
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
    
    // 当数据加载完成时，通知 splash screen 可以消失了
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onSplashScreenReady()
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
                onToggleCompletion = { viewModel.toggleHabitCompletion(it) },
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
    onToggleCompletion: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(habits, key = { it.id.toString() }) { habit ->
            HabitCard(
                habit = habit,
                onClick = { onHabitClick(habit) },
                onToggleCompletion = { onToggleCompletion(habit) },
                onUndoCompletion = { onUndoCompletion(habit) },
                onEditHabit = { onHabitClick(habit) },
                onDeleteHabit = { onDeleteHabit(habit) }
            )
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    onClick: () -> Unit,
    onToggleCompletion: () -> Unit,
    onUndoCompletion: () -> Unit,
    onEditHabit: () -> Unit,
    onDeleteHabit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (habit.completedToday) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 完成状态复选框
                Checkbox(
                    checked = habit.completedToday,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 习惯信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (habit.completedToday) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 显示重复周期和提醒时间
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (habit.repeatCycle) {
                                io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle.DAILY ->
                                    stringResource(id = R.string.create_habit_daily_option)
                                io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle.WEEKLY ->
                                    stringResource(id = R.string.create_habit_weekly_option)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val reminderCount = habit.getReminderTimesList().size
                        if (reminderCount > 0) {
                            Text(
                                text = "• $reminderCount 个提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 完成次数徽章
                if (habit.completionCount > 0) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = habit.completionCount.toString(),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // 长按下拉菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 撤销打卡（仅当今天已打卡时显示）
            if (habit.completedToday && habit.completionCount > 0) {
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
            onSplashScreenReady = {}
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
                completionCount = 15
            ),
            onClick = {},
            onToggleCompletion = {},
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
                completionCount = 30
            ),
            onClick = {},
            onToggleCompletion = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}
