package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.HabitStatus
import io.github.darrindeyoung791.habitpulse.data.model.HabitWithStatus
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
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
    private val repository: HabitRepository,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    // ============= UI State Flows =============

    /**
     * 所有习惯列表（按 sortOrder,  createdDate）
     * 收到第一个数据后会自动将 isLoading 设置为 false
     */
    val habitsFlow: Flow<List<Habit>> = repository.habitsBySortOrderFlow
        .onEach {
            _isLoading.value = false
            loadTodayCompletions()
        }

    // ============= Status & Progress Tracking =============

    private val _todayCompletions = MutableStateFlow<List<HabitCompletion>>(emptyList())
    val todayCompletionsFlow: StateFlow<List<HabitCompletion>> = _todayCompletions.asStateFlow()

    private var lastCheckedDate = HabitCompletion.getTodayDate()

    init {
        loadTodayCompletions()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val today = HabitCompletion.getTodayDate()
                if (lastCheckedDate != today) {
                    lastCheckedDate = today
                    loadTodayCompletions()
                }
            }
        }
    }

    private fun loadTodayCompletions() {
        viewModelScope.launch {
            _todayCompletions.value = repository.getTodayCompletions()
        }
    }

    val habitsWithStatusFlow: Flow<List<HabitWithStatus>> = combine(
        repository.habitsBySortOrderFlow,
        todayCompletionsFlow
    ) { habits, completions ->
        habits.map { habit ->
            HabitWithStatus(
                habit = habit,
                todayCompletions = completions.filter { it.habitId == habit.id },
                status = calculateHabitStatus(habit, completions.filter { it.habitId == habit.id })
            )
        }
    }.onEach { list ->
        _pendingTodayCount.value = list.count { it.status.contains(HabitStatus.PENDING_TODAY) }
        _aboutToStartCount.value = list.count { it.status.contains(HabitStatus.ABOUT_TO_START) }
        _overdueCount.value = list.count { it.status.contains(HabitStatus.OVERDUE) }
    }

    private val _pendingTodayCount = MutableStateFlow(0)
    val pendingTodayCount: StateFlow<Int> = _pendingTodayCount.asStateFlow()

    private val _aboutToStartCount = MutableStateFlow(0)
    val aboutToStartCount: StateFlow<Int> = _aboutToStartCount.asStateFlow()

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount.asStateFlow()

    private fun calculateHabitStatus(habit: Habit, todayCompletions: List<HabitCompletion>): Set<HabitStatus> {
        val allSlots = habit.getReminderTimesList()
        val completedSlotTimes = todayCompletions
            .filter { it.slotTime.isNotEmpty() }
            .map { it.slotTime }
            .toSet()
        val incompleteSlots = allSlots.filter { it !in completedSlotTimes }

        if (incompleteSlots.isEmpty() && allSlots.isNotEmpty()) {
            return setOf(HabitStatus.COMPLETED_TODAY)
        }

        if (!isApplicableToday(habit)) {
            return setOf(HabitStatus.NO_STATUS)
        }

        val status = mutableSetOf(HabitStatus.PENDING_TODAY)
        val now = java.util.Calendar.getInstance()

        var hasOverdue = false
        var hasAboutToStart = false

        for (slot in incompleteSlots) {
            val parts = slot.split(":")
            if (parts.size == 2) {
                val slotCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(java.util.Calendar.MINUTE, parts[1].toInt())
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val slotTime = slotCal.timeInMillis
                val oneHour = 60 * 60 * 1000L
                if (now.timeInMillis >= slotTime - oneHour && now.timeInMillis <= slotTime + oneHour) {
                    hasAboutToStart = true
                }
                if (now.timeInMillis > slotTime + oneHour) {
                    hasOverdue = true
                }
            }
        }

        if (hasOverdue) status.add(HabitStatus.OVERDUE)
        if (hasAboutToStart) status.add(HabitStatus.ABOUT_TO_START)

        return status
    }

    private fun isApplicableToday(habit: Habit): Boolean {
        if (habit.repeatCycle == RepeatCycle.DAILY) return true
        val todayIndex = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        return todayIndex in habit.getRepeatDaysList()
    }

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

    val habitsWithStatusForDisplay: StateFlow<List<HabitWithStatus>> = habitsWithStatusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredHabitsWithStatus: StateFlow<List<HabitWithStatus>> = combine(
        habitsWithStatusFlow,
        debouncedSearchQuery
    ) { allWithStatus, query ->
        if (query.isNullOrBlank()) {
            allWithStatus
        } else {
            val searchQuery = query.trim()
            allWithStatus.filter { ws ->
                ws.habit.title.contains(searchQuery, ignoreCase = true) ||
                ws.habit.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ============= Slot-based Check-in =============

    sealed class CheckInResult {
        data class Success(val isLate: Boolean, val isAllCompleted: Boolean) : CheckInResult()
        data class AlreadyCompleted(val maxCount: Int) : CheckInResult()
        data class TooEarly(val earliestSlotTime: String) : CheckInResult()
    }

    enum class CheckInFeedbackType { NONE, LATE_CHECK_IN, ALREADY_COMPLETED, TOO_EARLY, CHECK_IN_SUCCESS }

    private val _checkInFeedbackType = MutableStateFlow(CheckInFeedbackType.NONE)
    val checkInFeedbackType: StateFlow<CheckInFeedbackType> = _checkInFeedbackType.asStateFlow()

    private val _tooEarlyEarliestSlot = MutableStateFlow("")
    val tooEarlyEarliestSlot: StateFlow<String> = _tooEarlyEarliestSlot.asStateFlow()

    fun dismissCheckInFeedback() {
        _checkInFeedbackType.value = CheckInFeedbackType.NONE
    }

    fun performSlotCheckIn(habit: Habit) {
        viewModelScope.launch {
            val todayCompletions = _todayCompletions.value.filter { it.habitId == habit.id }
            val result = executeSlotCheckIn(habit, todayCompletions)
            when (result) {
                is CheckInResult.Success -> {
                    loadTodayCompletions()
                    if (result.isLate) {
                        _checkInFeedbackType.value = CheckInFeedbackType.LATE_CHECK_IN
                    } else if (result.isAllCompleted) {
                        showRewardSheet(habit)
                    } else {
                        _checkInFeedbackType.value = CheckInFeedbackType.CHECK_IN_SUCCESS
                    }
                }
                is CheckInResult.AlreadyCompleted -> {
                    _checkInFeedbackType.value = CheckInFeedbackType.ALREADY_COMPLETED
                }
                is CheckInResult.TooEarly -> {
                    _tooEarlyEarliestSlot.value = result.earliestSlotTime
                    _checkInFeedbackType.value = CheckInFeedbackType.TOO_EARLY
                }
            }
        }
    }

    private suspend fun executeSlotCheckIn(habit: Habit, todayCompletions: List<HabitCompletion>): CheckInResult {
        val allSlots = habit.getReminderTimesList()
        val completedSlotTimes = todayCompletions
            .filter { it.slotTime.isNotEmpty() }
            .map { it.slotTime }
            .toSet()
        val incompleteSlots = allSlots.filter { it !in completedSlotTimes }

        if (incompleteSlots.isEmpty()) {
            return CheckInResult.AlreadyCompleted(maxCount = allSlots.size)
        }

        if (allSlots.size == 1) {
            val slotTime = allSlots.first()
            val now = System.currentTimeMillis()
            val slotCal = java.util.Calendar.getInstance().apply {
                val parts = slotTime.split(":")
                set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(java.util.Calendar.MINUTE, parts[1].toInt())
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val isLate = now > slotCal.timeInMillis + 60 * 60 * 1000L
            val isAllCompleted = true
            repository.performSlotCheckIn(habit, slotTime, isLate, isAllCompleted)
            return CheckInResult.Success(isLate = isLate, isAllCompleted = true)
        }

        val now = java.util.Calendar.getInstance()
        for (slot in incompleteSlots) {
            val parts = slot.split(":")
            val slotCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(java.util.Calendar.MINUTE, parts[1].toInt())
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val slotTime = slotCal.timeInMillis
            val oneHour = 60 * 60 * 1000L

            if (now.timeInMillis >= slotTime - oneHour) {
                val isLate = now.timeInMillis > slotTime + oneHour
                val remainingAfterThis = incompleteSlots.size - 1
                val isAllCompleted = remainingAfterThis == 0
                repository.performSlotCheckIn(habit, slot, isLate, isAllCompleted)
                return CheckInResult.Success(isLate = isLate, isAllCompleted = isAllCompleted)
            }
        }

        val earliestSlot = incompleteSlots.first()
        val parts = earliestSlot.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val earliest = String.format("%02d:%02d", (hour - 1).coerceAtLeast(0), minute)
        return CheckInResult.TooEarly(earliestSlotTime = earliest)
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
     * 清空所有选中项，但保持多选模式
     */
    fun clearAllSelections() {
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

    // ============= Onboarding =============

    /**
     * 用户是否已完成初次使用引导（同意协议）
     * 从 SharedPreferences 读取初始值
     */
    private val _hasCompletedOnboarding = MutableStateFlow(onboardingPreferences.hasCompletedOnboarding)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    /**
     * 用户是否处于受限模式（不同意协议但仍使用应用）
     * 从 SharedPreferences 读取初始值
     */
    private val _isLimitedMode = MutableStateFlow(onboardingPreferences.isLimitedMode)
    val isLimitedMode: StateFlow<Boolean> = _isLimitedMode.asStateFlow()

    // ============= Reward Bottom Sheet =============

    /**
     * 当前显示奖励弹窗的习惯
     */
    private val _rewardSheetHabit = MutableStateFlow<Habit?>(null)
    val rewardSheetHabit: StateFlow<Habit?> = _rewardSheetHabit.asStateFlow()

    /**
     * 是否显示奖励底部弹窗
     */
    private val _showRewardSheet = MutableStateFlow(false)
    val showRewardSheet: StateFlow<Boolean> = _showRewardSheet.asStateFlow()

    /**
     * 显示奖励底部弹窗
     * @param habit 刚刚完成打卡的习惯
     */
    fun showRewardSheet(habit: Habit) {
        _rewardSheetHabit.value = habit
        _showRewardSheet.value = true
    }

    /**
     * 关闭奖励底部弹窗
     */
    fun dismissRewardSheet() {
        _showRewardSheet.value = false
        _rewardSheetHabit.value = null
    }

    /**
     * 标记用户已完成引导（同意协议）
     */
    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        _isLimitedMode.value = false
        onboardingPreferences.hasCompletedOnboarding = true
        onboardingPreferences.isLimitedMode = false
    }

    /**
     * 标记用户进入受限模式（不同意协议）
     */
    fun enterLimitedMode() {
        _hasCompletedOnboarding.value = true
        _isLimitedMode.value = true
        onboardingPreferences.hasCompletedOnboarding = true
        onboardingPreferences.isLimitedMode = true
    }

    /**
     * ViewModel Provider Factory
     */
    class Factory(private val application: HabitPulseApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
                return HabitViewModel(application.repository, application.onboardingPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
