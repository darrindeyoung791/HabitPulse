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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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
     * 所有习惯列表（按 sortOrder,  createdDate）
     * 收到第一个数据后会自动将 isLoading 设置为 false
     */
    val habitsFlow: Flow<List<Habit>> = repository.habitsBySortOrderFlow
        .onEach { _isLoading.value = false }

    /**
     * 是否需要 Home 列表滚动到顶部（用于多选排序保存后，统一回到顶部）
     */
    private val _scrollToTop = MutableStateFlow(0)
    val scrollToTop: StateFlow<Int> = _scrollToTop.asStateFlow()

    /**
     * 请求 Home 列表滚动到顶部
     * 每次调用都会增加计数器，确保触发收集器
     */
    fun requestScrollToTop() {
        _scrollToTop.value += 1
    }

    /**
     * 处理 Home 列表滚动到顶部请求
     */
    fun consumeScrollToTop() {
        _scrollToTop.value = 0
    }

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
     * 用于 debounce 的搜索词（用户输入时延迟处理）
     */
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: StateFlow<String> = _searchQuery
        .debounce(200)  // 200ms debounce to avoid excessive filtering during typing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /**
     * 搜索后的习惯列表（根据搜索关键词过滤）
     * 当搜索词为空时返回所有习惯，否则返回搜索结果
     * 使用 debounce 后的搜索词，避免用户输入时频繁过滤
     */
    val filteredHabitsFlow: Flow<List<Habit>> = repository.habitsBySortOrderFlow
        .combine(debouncedSearchQuery) { allHabits, query ->
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
        // 使用 distinctUntilChanged 避免重复发射相同数据
        .distinctUntilChanged()

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

    private suspend fun getTopSortOrder(): Int {
        val allHabits = repository.getAllHabits()
        return if (allHabits.isEmpty()) 0 else allHabits.minOf { it.sortOrder } - 1
    }

    /**
     * 保存习惯（新建或更新，都可通过这个接口）
     * 新建/编辑后的习惯会靠上展示。
     *
     * @param habit 要保存的习惯
     */
    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val topSort = getTopSortOrder()
                val habitToSave = habit.copy(sortOrder = topSort, modifiedDate = System.currentTimeMillis())
                val existing = repository.getHabitById(habit.id)

                if (existing == null) {
                    repository.insertHabit(habitToSave)
                } else {
                    repository.updateHabit(habitToSave)
                }

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
     * 更新习惯（仅更新，不自动移动至顶部）
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

    // ============= Multi-Select & Sort =============

    /**
     * 选中的习惯 ID 列表（用于多选模式）
     */
    private val _selectedHabitIds = MutableStateFlow<Set<UUID>>(emptySet())
    val selectedHabitIds: StateFlow<Set<UUID>> = _selectedHabitIds.asStateFlow()

    /**
     * 是否处于多选模式
     */
    private val _isMultiSelecting = MutableStateFlow(false)
    val isMultiSelecting: StateFlow<Boolean> = _isMultiSelecting.asStateFlow()

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(initialHabitId: UUID? = null) {
        _isMultiSelecting.value = true
        _selectedHabitIds.value = if (initialHabitId != null) setOf(initialHabitId) else emptySet()
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _isMultiSelecting.value = false
        _selectedHabitIds.value = emptySet()
    }

    /**
     * 切换习惯的选中状态
     */
    fun toggleHabitSelection(habitId: UUID) {
        val current = _selectedHabitIds.value
        _selectedHabitIds.value = if (habitId in current) {
            current - habitId
        } else {
            current + habitId
        }
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll(allHabitIds: List<UUID>) {
        _selectedHabitIds.value = if (_selectedHabitIds.value.size == allHabitIds.size) {
            emptySet()
        } else {
            allHabitIds.toSet()
        }
    }

    /**
     * 更新习惯的排序顺序
     */
    fun updateHabitSortOrder(habitId: UUID, newOrder: Int) {
        viewModelScope.launch {
            repository.updateHabitSortOrder(habitId, newOrder)
        }
    }

    /**
     * 批量更新多个习惯的排序顺序
     */
    fun updateMultipleHabitSortOrders(habitsWithOrder: List<Pair<UUID, Int>>) {
        viewModelScope.launch {
            repository.updateMultipleHabitSortOrders(habitsWithOrder)
        }
    }

    /**
     * 批量删除选中的习惯
     */
    fun deleteSelectedHabits() {
        viewModelScope.launch {
            val habitsToDelete = _selectedHabitIds.value
            repository.deleteHabitsByIds(habitsToDelete)
            _selectedHabitIds.value = emptySet()
            _isMultiSelecting.value = false
        }
    }

    /**
     * 获取按 sortOrder 排序的习惯列表 Flow
     */
    val habitsBySortOrderFlow: Flow<List<Habit>> = repository.habitsBySortOrderFlow
        .onEach { _isLoading.value = false }

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
