package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.viewmodel.RecordsViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 重置加载状态
 */
@Composable
private fun ResetLoadingStateOnEnter(viewModel: RecordsViewModel) {
    LaunchedEffect(Unit) {
        viewModel.resetLoadingState()
    }
}

/**
 * 格式化相对日期
 */
@Composable
fun formatRelativeDate(date: Date): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }

    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)
    then.set(Calendar.HOUR_OF_DAY, 0)
    then.set(Calendar.MINUTE, 0)
    then.set(Calendar.SECOND, 0)
    then.set(Calendar.MILLISECOND, 0)

    var yearDiff = now.get(Calendar.YEAR) - then.get(Calendar.YEAR)
    var monthDiff = now.get(Calendar.MONTH) - then.get(Calendar.MONTH)

    if (now.get(Calendar.DAY_OF_MONTH) < then.get(Calendar.DAY_OF_MONTH)) {
        monthDiff -= 1
        if (monthDiff < 0) {
            monthDiff += 12
            yearDiff -= 1
        }
    }

    val totalMonths = yearDiff * 12 + monthDiff
    val daysDiff = ((now.timeInMillis - then.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

    return when {
        daysDiff == 0 -> stringResource(id = R.string.records_date_today)
        daysDiff == 1 -> stringResource(id = R.string.records_date_yesterday)
        daysDiff in 2..6 -> stringResource(id = R.string.records_date_days_ago, daysDiff)
        daysDiff == 7 -> stringResource(id = R.string.records_date_last_week)
        daysDiff in 8..13 -> {
            then.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                ?: then.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        }
        daysDiff < 28 -> {
            val weeks = daysDiff / 7
            stringResource(id = R.string.records_date_weeks_ago, weeks)
        }
        totalMonths <= 0 -> stringResource(id = R.string.records_date_last_month)
        totalMonths < 12 -> stringResource(id = R.string.records_date_months_ago, totalMonths)
        totalMonths == 12 -> stringResource(id = R.string.records_date_last_year)
        else -> {
            val years = totalMonths / 12
            stringResource(id = R.string.records_date_years_ago, years)
        }
    }
}

/**
 * 记录页面内容组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreenContent(
    modifier: Modifier = Modifier,
    application: HabitPulseApplication? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    listState: LazyListState = remember { LazyListState() }
) {
    val viewModel: RecordsViewModel = if (application != null) {
        application.recordsViewModel
    } else {
        remember {
            val fakeHabitDao = FakeHabitDaoForRecords()
            val fakeCompletionDao = FakeHabitCompletionDaoForRecords()
            val fakeRepository = HabitRepository(fakeHabitDao, fakeCompletionDao)
            RecordsViewModel(fakeRepository)
        }
    }

    ResetLoadingStateOnEnter(viewModel)

    // Collect ViewModel states
    val groupedRecords by viewModel.groupedRecordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedHabitId by viewModel.selectedHabitId.collectAsStateWithLifecycle()
    val habitOptions by viewModel.habitOptionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    // Get selected habit name for display
    val selectedHabitName = habitOptions.find { option ->
        when (option) {
            is RecordsViewModel.HabitOption.AllHabits -> selectedHabitId == null
            is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.id == selectedHabitId
        }
    }?.let { option ->
        when (option) {
            is RecordsViewModel.HabitOption.AllHabits -> stringResource(id = R.string.records_all_habits)
            is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.title
        }
    } ?: stringResource(id = R.string.records_all_habits)

    // Date formatters
    val dateFormatPattern = stringResource(id = R.string.records_date_format)
    val timeFormatPattern = stringResource(id = R.string.records_time_format)
    val displayDateFormat = remember(dateFormatPattern) {
        SimpleDateFormat(dateFormatPattern, Locale.getDefault())
    }
    val timeFormat = remember(timeFormatPattern) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault())
    }

    // Apply nested scroll
    val nestedScrollModifier = scrollBehavior?.let { modifier.nestedScroll(it.nestedScrollConnection) } ?: modifier

    Column(
        modifier = nestedScrollModifier.fillMaxSize()
    ) {
        // === Filter Bar Section - Always Visible ===
        FilterBarSection(
            selectedHabitName = selectedHabitName,
            habitOptions = habitOptions,
            selectedHabitId = selectedHabitId,
            onHabitSelected = { viewModel.selectHabit(it) }
        )

        // === Content Section ===
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            groupedRecords.isEmpty() -> {
                EmptyRecordsContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedRecords.forEach { dateGroup ->
                        item(key = "header_${dateGroup.date}") {
                            DateSectionHeader(
                                date = dateGroup.date,
                                dateFormat = displayDateFormat,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        items(
                            items = dateGroup.records,
                            key = { it.completion.id }
                        ) { record ->
                            CompletionRecordCard(
                                completion = record.completion,
                                habitTitle = record.habit.title,
                                timeFormat = timeFormat,
                                completionSequence = record.completionSequence,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

/**
 * 过滤栏区域 - 只包含习惯选择
 */
@Composable
fun FilterBarSection(
    selectedHabitName: String,
    habitOptions: List<RecordsViewModel.HabitOption>,
    selectedHabitId: UUID?,
    onHabitSelected: (UUID?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Habit Selector
        HabitSelectorChip(
            selectedHabitName = selectedHabitName,
            habitOptions = habitOptions,
            selectedHabitId = selectedHabitId,
            onHabitSelected = onHabitSelected
        )
    }
}

/**
 * 习惯选择器 Chip
 */
@Composable
fun HabitSelectorChip(
    selectedHabitName: String,
    habitOptions: List<RecordsViewModel.HabitOption>,
    selectedHabitId: UUID?,
    onHabitSelected: (UUID?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selectedHabitName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(id = R.string.records_select_habit),
                    modifier = Modifier.size(18.dp)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            habitOptions.forEach { option ->
                val isSelected = when (option) {
                    is RecordsViewModel.HabitOption.AllHabits -> selectedHabitId == null
                    is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.id == selectedHabitId
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (option) {
                                is RecordsViewModel.HabitOption.AllHabits ->
                                    stringResource(id = R.string.records_all_habits)
                                is RecordsViewModel.HabitOption.SpecificHabit ->
                                    option.habit.title
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        val habitId = when (option) {
                            is RecordsViewModel.HabitOption.AllHabits -> null
                            is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.id
                        }
                        onHabitSelected(habitId)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 日期过滤按钮 - 用于 AppBar
 * 使用 AnimatedContent 实现平滑过渡动画
 */
@Composable
fun DateFilterButton(
    selectedDate: LocalDate?,
    onDateSelected: () -> Unit,
    onDateCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDateFormat = stringResource(id = R.string.records_date_format_short)
    val dateFormatter = remember { DateTimeFormatter.ofPattern(shortDateFormat) }
    val dateStr = selectedDate?.format(dateFormatter)

    AnimatedContent(
        targetState = selectedDate != null,
        label = "dateFilterButtonAnimation",
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(220))
                .togetherWith(fadeOut(animationSpec = tween(220)))
        }
    ) { isSelected ->
        if (isSelected && dateStr != null) {
            // 已选择日期状态 - 圆角矩形按钮
            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onDateCleared),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // 右侧竖线分隔符
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.records_clear_date),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // 未选择日期状态 - 图标按钮
            IconButton(
                onClick = onDateSelected
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(id = R.string.records_date_filter_tooltip),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 日期分组头部
 */
@Composable
fun DateSectionHeader(
    date: String,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val dateObj = try {
        val parts = date.split("-")
        val calendar = Calendar.getInstance()
        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        calendar.time
    } catch (e: Exception) {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = dateObj?.let { formatRelativeDate(it) } ?: date,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dateObj?.let { dateFormat.format(it) } ?: date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 打卡记录卡片
 */
@Composable
fun CompletionRecordCard(
    completion: HabitCompletion,
    habitTitle: String,
    timeFormat: SimpleDateFormat,
    completionSequence: Int,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val formattedTime = timeFormat.format(Date(completion.completedDate))
    val relativeDate = formatRelativeDate(Date(completion.completedDate))
    val contentDescription = stringResource(
        id = R.string.records_card_content_description,
        habitTitle,
        completionSequence,
        relativeDate,
        formattedTime
    )

    Card(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {}
            )
            .semantics {
                this.contentDescription = contentDescription
                onClick { true }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.records_completion_sequence, completionSequence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 空记录状态
 */
@Composable
fun EmptyRecordsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Assessment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.records_no_completions),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.records_no_completions_description),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============= Fake DAOs for Preview =============

@Suppress("unused")
private class FakeHabitDaoForRecords : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = listOf(
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            title = "每天喝水",
            repeatCycle = RepeatCycle.DAILY,
            completionCount = 15,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            title = "晨跑锻炼",
            repeatCycle = RepeatCycle.WEEKLY,
            completionCount = 8,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
            title = "阅读书籍",
            repeatCycle = RepeatCycle.DAILY,
            completionCount = 20,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        )
    )

    override fun getAllHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)

    override suspend fun getAllHabits(): List<Habit> = habits
    override fun getHabitByIdFlow(id: UUID): kotlinx.coroutines.flow.Flow<Habit?> =
        kotlinx.coroutines.flow.flowOf(habits.find { it.id == id })

    override suspend fun getHabitById(id: UUID): Habit? = habits.find { it.id == id }
    override fun getIncompleteHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getCompletedHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)

    override suspend fun insert(habit: Habit): Long = 0
    override suspend fun update(habit: Habit) {}
    override suspend fun delete(habit: Habit) {}
    override suspend fun deleteAll() {}
    override suspend fun updateCompletionStatus(id: UUID, completed: Boolean, timestamp: Long) {}
    override suspend fun undoCompletionStatus(id: UUID, timestamp: Long) {}
    override suspend fun incrementCompletionCount(id: UUID, timestamp: Long) {}
    override suspend fun resetAllCompletionStatus(timestamp: Long) {}
    override fun getHabitCount(): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(habits.size)

    override fun searchHabitsFlow(query: String): kotlinx.coroutines.flow.Flow<List<Habit>> {
        val searchQuery = query.trim('%')
        return kotlinx.coroutines.flow.flowOf(
            habits.filter { habit ->
                habit.title.contains(searchQuery, ignoreCase = true)
            }
        )
    }
}

@Suppress("unused")
private class FakeHabitCompletionDaoForRecords : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    private val completions = mutableListOf<HabitCompletion>()

    init {
        val calendar = Calendar.getInstance()
        val habit1Id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val habit2Id = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val habit3Id = UUID.fromString("00000000-0000-0000-0000-000000000003")

        for (i in 0 until 7) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val timestamp = calendar.timeInMillis
            val dateStr = String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            if (i % 2 == 0) {
                completions.add(
                    HabitCompletion(
                        habitId = habit1Id,
                        completedDate = timestamp + 10 * 60 * 1000,
                        completedDateLocal = dateStr
                    )
                )
            }
            if (i % 3 == 0) {
                completions.add(
                    HabitCompletion(
                        habitId = habit2Id,
                        completedDate = timestamp + 7 * 60 * 60 * 1000,
                        completedDateLocal = dateStr
                    )
                )
            }
            completions.add(
                HabitCompletion(
                    habitId = habit3Id,
                    completedDate = timestamp + 21 * 60 * 60 * 1000,
                    completedDateLocal = dateStr
                )
            )
        }
    }

    override fun getCompletionsByHabitIdFlow(habitId: UUID): kotlinx.coroutines.flow.Flow<List<HabitCompletion>> =
        kotlinx.coroutines.flow.flowOf(completions.filter { it.habitId == habitId })

    override suspend fun getCompletionsByHabitId(habitId: UUID): List<HabitCompletion> =
        completions.filter { it.habitId == habitId }

    override suspend fun getCompletionsByHabitIdAndDate(
        habitId: UUID,
        date: String
    ): List<HabitCompletion> = completions.filter { it.habitId == habitId && it.completedDateLocal == date }

    override suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: UUID,
        startDate: String,
        endDate: String
    ): List<HabitCompletion> =
        completions.filter { it.habitId == habitId && it.completedDateLocal in startDate..endDate }

    override suspend fun getCompletionsByDate(date: String): List<HabitCompletion> =
        completions.filter { it.completedDateLocal == date }

    override suspend fun getTodayCompletionCount(habitId: UUID, date: String): Int =
        completions.count { it.habitId == habitId && it.completedDateLocal == date }

    override suspend fun insert(completion: HabitCompletion): Long {
        completions.add(completion)
        return 0
    }

    override suspend fun insertAll(completions: List<HabitCompletion>) {
        this.completions.addAll(completions)
    }

    override suspend fun delete(completion: HabitCompletion) {
        completions.remove(completion)
    }

    override suspend fun deleteByHabitId(habitId: UUID) {
        completions.removeAll { it.habitId == habitId }
    }

    override suspend fun deleteByDate(date: String) {
        completions.removeAll { it.completedDateLocal == date }
    }

    override suspend fun deleteAll() {
        completions.clear()
    }

    override fun getCompletionCount(): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(completions.size)

    override suspend fun getCompletionCountByHabitId(habitId: UUID): Int =
        completions.count { it.habitId == habitId }
}

// ============= Previews =============

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecordsScreenContentPreview() {
    HabitPulseTheme {
        RecordsScreenContent()
    }
}

@Preview(showBackground = true)
@Composable
fun DateFilterButtonPreview() {
    HabitPulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateFilterButton(
                selectedDate = null,
                onDateSelected = {},
                onDateCleared = {}
            )
            DateFilterButton(
                selectedDate = LocalDate.now(),
                onDateSelected = {},
                onDateCleared = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitSelectorChipPreview() {
    HabitPulseTheme {
        HabitSelectorChip(
            selectedHabitName = "全部习惯",
            habitOptions = listOf(
                RecordsViewModel.HabitOption.AllHabits
            ),
            selectedHabitId = null,
            onHabitSelected = {}
        )
    }
}
