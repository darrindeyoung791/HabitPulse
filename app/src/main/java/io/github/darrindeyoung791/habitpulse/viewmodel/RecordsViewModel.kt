package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
     * 合并后的打卡记录 Flow（按日期分组）
     * 根据选中的习惯 ID 过滤记录
     */
    val groupedRecordsFlow: Flow<List<DateGroup>> = combine(
        habitsFlow,
        _selectedHabitId
    ) { habits, selectedId ->
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
        val filteredCompletions = if (selectedId == null) {
            sortedCompletions
        } else {
            sortedCompletions.filter { it.habitId == selectedId }
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
        hasLoadedData = true
    }

    /**
     * 重置加载状态（在屏幕切换时调用，确保显示加载指示器过渡）
     */
    fun resetLoadingState() {
        _isLoading.value = true
        hasLoadedData = false
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
