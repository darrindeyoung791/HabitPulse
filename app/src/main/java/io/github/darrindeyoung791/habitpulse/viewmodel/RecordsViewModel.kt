package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.UUID

/**
 * 记录页面 ViewModel
 *
 * 管理习惯打卡记录的 UI 状态和业务逻辑
 */
class RecordsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    /**
     * 所有习惯列表
     */
    private val habitsFlow: Flow<List<Habit>> = repository.allHabitsFlow

    /**
     * 当前选中的习惯 ID，null 表示显示全部习惯
     */
    private val _selectedHabitId = MutableStateFlow<UUID?>(null)
    val selectedHabitId: StateFlow<UUID?> = _selectedHabitId.asStateFlow()

    /**
     * 下拉菜单是否展开
     */
    private val _dropdownExpanded = MutableStateFlow(false)
    val dropdownExpanded: StateFlow<Boolean> = _dropdownExpanded.asStateFlow()

    /**
     * 数据是否正在加载
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 数据是否已经加载过（用于避免切换页面时反复显示加载指示器）
     */
    private val _hasLoadedDataOnce = MutableStateFlow(false)
    val hasLoadedDataOnce: StateFlow<Boolean> = _hasLoadedDataOnce.asStateFlow()

    /**
     * 最后一次的非空数据（用于避免切换页面时闪现空状态）
     */
    private val _lastNonEmptyData = MutableStateFlow<List<DateGroup>>(emptyList())
    val lastNonEmptyData: StateFlow<List<DateGroup>> = _lastNonEmptyData.asStateFlow()

    /**
     * 当前选中的日期（用于过滤），null 表示不过滤日期
     */
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /**
     * 日期选择器是否展开
     */
    private val _datePickerExpanded = MutableStateFlow(false)
    val datePickerExpanded: StateFlow<Boolean> = _datePickerExpanded.asStateFlow()

    /**
     * 打卡记录列表（按时间倒序）
     * 包含 HabitCompletion 和对应的 Habit 信息
     */
    data class CompletionRecord(
        val completion: HabitCompletion,
        val habit: Habit,
        val completionSequence: Int // 该习惯的第几次打卡
    )

    /**
     * 按日期分组的打卡记录
     */
    data class DateGroup(
        val date: String, // yyyy-MM-dd format
        val records: List<CompletionRecord>
    )

    /**
     * 标记是否已加载过数据（用于避免每次切换都显示加载指示器）
     */
    private var hasLoadedData = false

    /**
     * 打卡天数统计 Flow - 根据选中的习惯 ID 计算有多少天有打卡记录
     * 选择全部习惯时统计所有有打卡的天数
     */
    val completionDaysCountFlow: StateFlow<Int>

    init {
        completionDaysCountFlow = combine(
            habitsFlow,
            _selectedHabitId
        ) { habits, selectedId ->
            // 获取所有打卡记录
            val allCompletions = habits.flatMap { habit ->
                repository.getCompletionsByHabitId(habit.id)
            }

            // 根据选中的习惯 ID 过滤
            val filteredCompletions = if (selectedId == null) {
                allCompletions
            } else {
                allCompletions.filter { it.habitId == selectedId }
            }

            // 统计不同的打卡天数（使用 completedDateLocal）
            filteredCompletions.map { it.completedDateLocal }.distinct().size
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = 0
        )
    }

    /**
     * 合并后的打卡记录 Flow（按日期分组）
     * 根据选中的习惯 ID 和日期过滤记录
     */
    val groupedRecordsFlow: Flow<List<DateGroup>> = combine(
        habitsFlow,
        _selectedHabitId,
        _selectedDate
    ) { habits, selectedId, selectedDate ->
        // 首次加载时设置 loading 状态
        if (!hasLoadedData) {
            _isLoading.value = true
        }

        // 获取所有打卡记录
        val allCompletions = habits.flatMap { habit ->
            repository.getCompletionsByHabitId(habit.id)
        }
        // 按日期倒序排序
        val sortedCompletions = allCompletions.sortedByDescending { it.completedDate }

        // 根据选中的习惯 ID 过滤
        val filteredByHabit = if (selectedId == null) {
            sortedCompletions
        } else {
            sortedCompletions.filter { it.habitId == selectedId }
        }

        // 根据选中的日期过滤
        val filteredCompletions = if (selectedDate == null) {
            filteredByHabit
        } else {
            val dateStr = selectedDate.toString() // yyyy-MM-dd format
            filteredByHabit.filter { it.completedDateLocal == dateStr }
        }

        // 合并 Habit 信息并按日期分组
        val records = filteredCompletions.mapNotNull { completion ->
            val habit = habits.find { it.id == completion.habitId }
            if (habit != null) {
                // 计算这是该习惯的第几次打卡（按时间排序）
                val habitCompletions = repository.getCompletionsByHabitId(habit.id)
                    .sortedBy { it.completedDate }
                val sequence = habitCompletions.indexOf(completion) + 1
                CompletionRecord(completion, habit, sequence)
            } else {
                null
            }
        }

        // 按日期分组
        records.groupBy { it.completion.completedDateLocal }
            .map { (date, recordsForDate) -> DateGroup(date, recordsForDate) }
            .sortedByDescending { it.date }
    }.onEach {
        _isLoading.value = false
        _hasLoadedDataOnce.value = true
        // 保存最后一次的非空数据
        if (it.isNotEmpty()) {
            _lastNonEmptyData.value = it
        }
    }

    /**
     * 重置加载状态（在屏幕切换时调用，确保显示加载指示器过渡）
     */
    fun resetLoadingState() {
        _isLoading.value = true
        // 注意：不重置 hasLoadedDataOnce，避免切换页面时反复显示加载指示器
    }

    /**
     * 获取用于下拉菜单的习惯列表
     * 第一个选项是"全部习惯"
     */
    val habitOptionsFlow: Flow<List<HabitOption>> = habitsFlow.map { habits ->
        val options = mutableListOf<HabitOption>()
        // 添加"全部习惯"选项
        options.add(HabitOption.AllHabits)
        // 添加各个习惯选项
        habits.forEach { habit ->
            options.add(HabitOption.SpecificHabit(habit))
        }
        options
    }

    /**
     * 习惯选项
     */
    sealed class HabitOption {
        object AllHabits : HabitOption()
        data class SpecificHabit(val habit: Habit) : HabitOption()
    }

    /**
     * 选择习惯
     * @param habitId 习惯 ID，null 表示显示全部
     */
    fun selectHabit(habitId: UUID?) {
        _selectedHabitId.value = habitId
    }

    /**
     * 设置下拉菜单展开状态
     */
    fun setDropdownExpanded(expanded: Boolean) {
        _dropdownExpanded.value = expanded
    }

    /**
     * 选择日期
     * @param date 日期，null 表示清除日期选择
     */
    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    /**
     * 清除日期选择
     */
    fun clearDate() {
        _selectedDate.value = null
    }

    /**
     * 设置日期选择器展开状态
     */
    fun setDatePickerExpanded(expanded: Boolean) {
        _datePickerExpanded.value = expanded
    }

    /**
     * ViewModel Provider Factory
     */
    class Factory(private val application: HabitPulseApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
                return RecordsViewModel(application.repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
