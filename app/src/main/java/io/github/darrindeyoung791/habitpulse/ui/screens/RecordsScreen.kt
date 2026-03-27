package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.viewmodel.RecordsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

/**
 * 重置加载状态以确保切换时显示加载指示器过渡
 * 使用 LaunchedEffect 在每次进入 Records 屏幕时触发
 */
@Composable
private fun ResetLoadingStateOnEnter(viewModel: RecordsViewModel) {
    LaunchedEffect(Unit) {
        viewModel.resetLoadingState()
    }
}

/**
 * 格式化相对日期
 * 返回如：今天、昨天、2 天前、上周三、3 周前、2 个月前、1 年前等
 */
@Composable
fun formatRelativeDate(date: Date): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }

    // 清除时间部分，只比较日期
    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)
    then.set(Calendar.HOUR_OF_DAY, 0)
    then.set(Calendar.MINUTE, 0)
    then.set(Calendar.SECOND, 0)
    then.set(Calendar.MILLISECOND, 0)

    // 先计算月份差异（更准确）
    var yearDiff = now.get(Calendar.YEAR) - then.get(Calendar.YEAR)
    var monthDiff = now.get(Calendar.MONTH) - then.get(Calendar.MONTH)
    
    // 调整：如果当前日期的日期小于目标日期，需要借一个月
    if (now.get(Calendar.DAY_OF_MONTH) < then.get(Calendar.DAY_OF_MONTH)) {
        monthDiff -= 1
        if (monthDiff < 0) {
            monthDiff += 12
            yearDiff -= 1
        }
    }
    
    val totalMonths = yearDiff * 12 + monthDiff
    
    // 计算天数差异（用于短期显示）
    val daysDiff = ((now.timeInMillis - then.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

    return when {
        // 短期：按天显示
        daysDiff == 0 -> stringResource(id = R.string.records_date_today)
        daysDiff == 1 -> stringResource(id = R.string.records_date_yesterday)
        daysDiff in 2..6 -> stringResource(id = R.string.records_date_days_ago, daysDiff)
        daysDiff == 7 -> stringResource(id = R.string.records_date_last_week)
        daysDiff in 8..13 -> {
            val dayOfWeek = then.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                ?: then.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
            dayOfWeek
        }
        daysDiff < 28 -> {
            val weeks = daysDiff / 7
            stringResource(id = R.string.records_date_weeks_ago, weeks)
        }
        // 长期：按月/年显示（优先使用月份计算）
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
 * 记录页面内容组件（由 HomeScreen 嵌入使用）
 * 不包含 Scaffold 和 TopAppBar，由父组件管理
 *
 * @param modifier 修饰符
 * @param application 应用程序实例
 * @param scrollBehavior 滚动行为（用于 TopAppBar 联动）
 * @param listState 列表滚动状态（用于保存滚动位置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreenContent(
    modifier: Modifier = Modifier,
    application: HabitPulseApplication? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    listState: LazyListState = remember { LazyListState() }
) {
    // 获取 ViewModel - 使用 Application 中的单例
    val viewModel: RecordsViewModel = if (application != null) {
        application.recordsViewModel
    } else {
        // Preview mode
        remember {
            val fakeHabitDao = FakeHabitDaoForRecords()
            val fakeCompletionDao = FakeHabitCompletionDaoForRecords()
            val fakeRepository = io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository(
                fakeHabitDao,
                fakeCompletionDao
            )
            RecordsViewModel(fakeRepository)
        }
    }

    // 每次进入屏幕时重置加载状态，确保显示加载指示器过渡
    ResetLoadingStateOnEnter(viewModel)

    // 收集状态
    val groupedRecords by viewModel.groupedRecordsFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val selectedHabitId by viewModel.selectedHabitId.collectAsStateWithLifecycle()
    val dropdownExpanded by viewModel.dropdownExpanded.collectAsStateWithLifecycle()
    val habitOptions by viewModel.habitOptionsFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)

    // 获取当前选中的习惯显示名称
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

    // 格式化日期
    val context = LocalContext.current
    val dateFormatPattern = stringResource(id = R.string.records_date_format)
    val timeFormatPattern = stringResource(id = R.string.records_time_format)
    val displayDateFormat = remember(dateFormatPattern) {
        SimpleDateFormat(dateFormatPattern, Locale.getDefault())
    }
    val timeFormat = remember(timeFormatPattern) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault())
    }

    // 应用 nested scroll
    val nestedScrollModifier = scrollBehavior?.let { modifier.nestedScroll(it.nestedScrollConnection) } ?: modifier

    if (isLoading) {
        // 加载状态
        Box(
            modifier = nestedScrollModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (groupedRecords.isEmpty()) {
        // 空状态
        EmptyRecordsContent(
            modifier = nestedScrollModifier.fillMaxSize()
        )
    } else {
        // 记录列表（按日期分组）
        LazyColumn(
            modifier = nestedScrollModifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 顶部添加下拉菜单区域
            item(key = "dropdown_header") {
                Spacer(modifier = Modifier.height(8.dp))
                // 习惯选择下拉菜单
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { viewModel.setDropdownExpanded(it) }
                ) {
                    Row(
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { viewModel.setDropdownExpanded(true) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedHabitName,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(id = R.string.records_select_habit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { viewModel.setDropdownExpanded(false) }
                    ) {
                        habitOptions.forEach { option ->
                            val isSelected = when (option) {
                                is RecordsViewModel.HabitOption.AllHabits -> selectedHabitId == null
                                is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.id == selectedHabitId
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = when (option) {
                                                is RecordsViewModel.HabitOption.AllHabits ->
                                                    stringResource(id = R.string.records_all_habits)
                                                is RecordsViewModel.HabitOption.SpecificHabit ->
                                                    option.habit.title
                                            },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    val habitId = when (option) {
                                        is RecordsViewModel.HabitOption.AllHabits -> null
                                        is RecordsViewModel.HabitOption.SpecificHabit -> option.habit.id
                                    }
                                    viewModel.selectHabit(habitId)
                                    viewModel.setDropdownExpanded(false)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedRecords.forEach { dateGroup ->
                // 日期头部
                item(key = "header_${dateGroup.date}") {
                    DateSectionHeader(
                        date = dateGroup.date,
                        dateFormat = displayDateFormat,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 该日期的记录
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

            // 底部添加空白防止最后一条记录被遮挡
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * 日期分组头部
 */
@Composable
fun DateSectionHeader(
    date: String, // yyyy-MM-dd format
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
        // 相对日期（醒目）
        Text(
            text = dateObj?.let { formatRelativeDate(it) } ?: date,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        // 完整日期（次要）
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habitTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.records_completion_sequence, completionSequence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeFormat.format(Date(completion.completedDate)),
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
fun EmptyRecordsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.records_no_completions),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.records_no_completions_description),
            style = MaterialTheme.typography.bodyMedium,
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
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            title = "每天喝水",
            repeatCycle = RepeatCycle.DAILY,
            completionCount = 15,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
            title = "晨跑锻炼",
            repeatCycle = RepeatCycle.WEEKLY,
            completionCount = 8,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"),
            title = "阅读书籍",
            repeatCycle = RepeatCycle.DAILY,
            completionCount = 20,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        )
    )

    override fun getAllHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)

    override suspend fun getAllHabits(): List<Habit> = habits

    override fun getHabitByIdFlow(id: java.util.UUID): kotlinx.coroutines.flow.Flow<Habit?> =
        kotlinx.coroutines.flow.flowOf(habits.find { it.id == id })

    override suspend fun getHabitById(id: java.util.UUID): Habit? = habits.find { it.id == id }

    override fun getIncompleteHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getCompletedHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)

    override suspend fun insert(habit: Habit): Long = 0

    override suspend fun update(habit: Habit) {}

    override suspend fun delete(habit: Habit) {}

    override suspend fun deleteAll() {}

    override suspend fun updateCompletionStatus(id: java.util.UUID, completed: Boolean, timestamp: Long) {}

    override suspend fun undoCompletionStatus(id: java.util.UUID, timestamp: Long) {}

    override suspend fun incrementCompletionCount(id: java.util.UUID, timestamp: Long) {}

    override suspend fun resetAllCompletionStatus(timestamp: Long) {}

    override fun getHabitCount(): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(habits.size)
}

@Suppress("unused")
private class FakeHabitCompletionDaoForRecords : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    private val completions = mutableListOf<HabitCompletion>()

    init {
        // 添加一些示例数据
        val calendar = Calendar.getInstance()
        val habit1Id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
        val habit2Id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
        val habit3Id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")

        // 过去 7 天的记录
        for (i in 0 until 7) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val timestamp = calendar.timeInMillis
            val dateStr = String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH))

            // 随机添加一些记录
            if (i % 2 == 0) {
                completions.add(
                    HabitCompletion(
                        habitId = habit1Id,
                        completedDate = timestamp + 10 * 60 * 1000, // 10:00
                        completedDateLocal = dateStr
                    )
                )
            }
            if (i % 3 == 0) {
                completions.add(
                    HabitCompletion(
                        habitId = habit2Id,
                        completedDate = timestamp + 7 * 60 * 60 * 1000, // 07:00
                        completedDateLocal = dateStr
                    )
                )
            }
            completions.add(
                HabitCompletion(
                    habitId = habit3Id,
                    completedDate = timestamp + 21 * 60 * 60 * 1000, // 21:00
                    completedDateLocal = dateStr
                )
            )
        }
    }

    override fun getCompletionsByHabitIdFlow(habitId: java.util.UUID): kotlinx.coroutines.flow.Flow<List<HabitCompletion>> =
        kotlinx.coroutines.flow.flowOf(completions.filter { it.habitId == habitId })

    override suspend fun getCompletionsByHabitId(habitId: java.util.UUID): List<HabitCompletion> =
        completions.filter { it.habitId == habitId }

    override suspend fun getCompletionsByHabitIdAndDate(
        habitId: java.util.UUID,
        date: String
    ): List<HabitCompletion> = completions.filter { it.habitId == habitId && it.completedDateLocal == date }

    override suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: java.util.UUID,
        startDate: String,
        endDate: String
    ): List<HabitCompletion> =
        completions.filter { it.habitId == habitId && it.completedDateLocal in startDate..endDate }

    override suspend fun getCompletionsByDate(date: String): List<HabitCompletion> =
        completions.filter { it.completedDateLocal == date }

    override suspend fun getTodayCompletionCount(habitId: java.util.UUID, date: String): Int =
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

    override suspend fun deleteByHabitId(habitId: java.util.UUID) {
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

    override suspend fun getCompletionCountByHabitId(habitId: java.util.UUID): Int =
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
fun CompletionRecordCardPreview() {
    HabitPulseTheme {
        CompletionRecordCard(
            completion = HabitCompletion(
                habitId = java.util.UUID.randomUUID(),
                completedDate = System.currentTimeMillis(),
                completedDateLocal = "2026-03-26"
            ),
            habitTitle = "每天喝水",
            timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA),
            completionSequence = 15
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateSectionHeaderPreview() {
    HabitPulseTheme {
        DateSectionHeader(
            date = "2026-03-26",
            dateFormat = SimpleDateFormat("yyyy 年 MM 月 dd 日", Locale.CHINA),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyRecordsContentPreview() {
    HabitPulseTheme {
        EmptyRecordsContent()
    }
}
