package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ============= Data Operations =============

    /**
     * 根据 ID 获取习惯（阻塞调用，用于编辑模式初始化）
     */
    fun getHabitById(id: UUID): Habit? {
        return kotlinx.coroutines.runBlocking { repository.getHabitById(id) }
    }

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
     */
    fun incrementCompletionCount(habit: Habit) {
        viewModelScope.launch {
            repository.incrementCompletionCount(habit)
        }
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
