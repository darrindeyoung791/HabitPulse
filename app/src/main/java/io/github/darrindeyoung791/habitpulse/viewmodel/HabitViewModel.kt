package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 习惯 ViewModel
 *
 * 管理习惯相关的 UI 状态和业务逻辑
 * 作为 UI 层和数据层之间的桥梁
 */
class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    // ============= UI State Flows =============

    /**
     * 所有习惯列表（按创建日期倒序）
     * 收到第一个数据后会自动将 isLoading 设置为 false
     */
    val habitsFlow: Flow<List<Habit>> = repository.allHabitsFlow
        .onEach { _isLoading.value = false }

    /**
     * 习惯总数
     */
    val habitCountFlow: Flow<Int> = repository.habitCountFlow

    /**
     * 打卡记录总数
     */
    val completionCountFlow: Flow<Int> = repository.completionCountFlow

    /**
     * 数据是否正在加载
     */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 当前正在编辑的习惯（用于编辑模式）
     */
    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    /**
     * 保存操作是否正在进行
     */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * 保存是否成功
     */
    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    /**
     * 新添加的习惯 ID（用于触发动画）
     */
    private val _newlyAddedHabitId = MutableStateFlow<UUID?>(null)
    val newlyAddedHabitId: StateFlow<UUID?> = _newlyAddedHabitId.asStateFlow()

    // ============= Search =============

    /**
     * 搜索关键词
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 搜索后的习惯列表（根据搜索关键词过滤）
     * 当搜索词为空时返回所有习惯，否则返回搜索结果
     */
    @OptIn(FlowPreview::class)
    val filteredHabitsFlow: Flow<List<Habit>> = _searchQuery
        .debounce(200)  // 200ms debounce to avoid excessive queries
        .combine(repository.allHabitsFlow) { query, allHabits ->
            if (query.isNullOrBlank()) {
                allHabits
            } else {
                // Client-side search for simplicity and responsiveness
                val searchQuery = query.trim()
                allHabits.filter { habit ->
                    habit.title.contains(searchQuery, ignoreCase = true) ||
                    habit.notes.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    // ============= Data Operations =============

    /**
     * 根据 ID 获取习惯（阻塞调用，用于编辑模式初始化）
     */
    fun getHabitById(id: UUID): Habit? {
        return kotlinx.coroutines.runBlocking { repository.getHabitById(id) }
    }

    /**
     * 获取指定习惯的所有打卡记录 Flow
     */
    fun getCompletionsByHabitIdFlow(habitId: UUID): Flow<List<HabitCompletion>> =
        repository.getCompletionsByHabitIdFlow(habitId)

    /**
     * 获取指定习惯的所有打卡记录（一次性）
     */
    suspend fun getCompletionsByHabitId(habitId: UUID): List<HabitCompletion> =
        repository.getCompletionsByHabitId(habitId)

    /**
     * 获取指定习惯在指定日期的打卡记录
     */
    suspend fun getCompletionsByHabitIdAndDate(
        habitId: UUID,
        date: String
    ): List<HabitCompletion> =
        repository.getCompletionsByHabitIdAndDate(habitId, date)

    /**
     * 获取指定习惯今天的打卡记录数量
     */
    suspend fun getTodayCompletionCount(habitId: UUID): Int =
        repository.getTodayCompletionCount(habitId)

    /**
     * 设置正在编辑的习惯
     */
    fun setEditingHabit(habit: Habit?) {
        _editingHabit.value = habit
    }

    /**
     * 保存习惯（新建或更新）
     *
     * @param habit 要保存的习惯
     */
    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.insertHabit(habit)
                // Set newly added habit ID for animation trigger
                _newlyAddedHabitId.value = habit.id
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 更新习惯
     */
    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateHabit(habit)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 删除习惯
     */
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    /**
     * 切换习惯的完成状态
     */
    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            repository.toggleCompletionStatus(habit)
        }
    }

    /**
     * 增加习惯的完成次数（打卡）
     * @return 新插入的打卡记录
     */
    fun incrementCompletionCount(habit: Habit): HabitCompletion? {
        var result: HabitCompletion? = null
        viewModelScope.launch {
            result = repository.incrementCompletionCount(habit)
        }
        return result
    }

    /**
     * 撤销习惯的完成状态（completionCount 减 1）
     * 适用于任何已完成次数大于 0 的习惯
     */
    fun undoHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            repository.undoCompletionStatus(habit)
        }
    }

    /**
     * 重置保存状态标志
     */
    fun resetSaveSuccess() {
        _saveSuccess.value = null
    }

    /**
     * 重置新添加习惯 ID（动画完成后调用）
     */
    fun resetNewlyAddedHabitId() {
        _newlyAddedHabitId.value = null
    }

    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 清除搜索关键词
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * ViewModel Provider Factory
     */
    class Factory(private val application: HabitPulseApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
                return HabitViewModel(application.repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
