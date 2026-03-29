package io.github.darrindeyoung791.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 联系人 ViewModel
 */
class ContactsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    /**
     * 所有习惯列表
     */
    private val _habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    val habitsFlow: StateFlow<List<Habit>> = _habitsFlow.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allHabitsFlow.collect { habits ->
                _habitsFlow.value = habits.sortedByDescending { it.createdDate }
            }
        }
    }

    /**
     * 联系人类型
     */
    enum class ContactType {
        EMAIL,
        PHONE
    }

    /**
     * 联系人信息数据类
     */
    data class ContactInfo(
        val type: ContactType,
        val value: String,
        val habitIds: List<UUID>
    )

    /**
     * 所有联系人列表
     */
    private val _allContactsFlow = MutableStateFlow<List<ContactInfo>>(emptyList())
    val allContactsFlow: StateFlow<List<ContactInfo>> = _allContactsFlow.asStateFlow()

    /**
     * 数据是否正在加载
     */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 数据是否已经加载过（用于避免切换页面时反复显示加载指示器）
     */
    private val _hasLoadedDataOnce = MutableStateFlow(false)
    val hasLoadedDataOnce: StateFlow<Boolean> = _hasLoadedDataOnce.asStateFlow()

    /**
     * 最后一次的非空数据（用于避免切换页面时闪现空状态）
     */
    private val _lastNonEmptyData = MutableStateFlow<List<ContactInfo>>(emptyList())
    val lastNonEmptyData: StateFlow<List<ContactInfo>> = _lastNonEmptyData.asStateFlow()

    init {
        viewModelScope.launch {
            habitsFlow.collect { habits ->
                val contactMap = mutableMapOf<String, ContactInfo>()

                habits.forEach { habit ->
                    // 收集邮箱
                    if (habit.supervisionMethod == SupervisionMethod.EMAIL) {
                        habit.getSupervisorEmailsList().forEach { email ->
                            val key = "email:$email"
                            if (contactMap.containsKey(key)) {
                                val existing = contactMap[key]!!
                                contactMap[key] = existing.copy(habitIds = existing.habitIds + habit.id)
                            } else {
                                contactMap[key] = ContactInfo(
                                    type = ContactType.EMAIL,
                                    value = email,
                                    habitIds = listOf(habit.id)
                                )
                            }
                        }
                    }

                    // 收集电话
                    if (habit.supervisionMethod == SupervisionMethod.SMS) {
                        habit.getSupervisorPhonesList().forEach { phone ->
                            val key = "phone:$phone"
                            if (contactMap.containsKey(key)) {
                                val existing = contactMap[key]!!
                                contactMap[key] = existing.copy(habitIds = existing.habitIds + habit.id)
                            } else {
                                contactMap[key] = ContactInfo(
                                    type = ContactType.PHONE,
                                    value = phone,
                                    habitIds = listOf(habit.id)
                                )
                            }
                        }
                    }
                }

                // 排序：先按类型，再按值
                val sortedContacts = contactMap.values.sortedWith(
                    compareBy({ it.type }, { it.value })
                )
                _allContactsFlow.value = sortedContacts
                _isLoading.value = false
                _hasLoadedDataOnce.value = true
                // 保存最后一次的非空数据
                if (sortedContacts.isNotEmpty()) {
                    _lastNonEmptyData.value = sortedContacts
                }
            }
        }
    }

    /**
     * 重置加载状态（在屏幕切换时调用，显示短暂加载过渡）
     */
    fun resetLoadingState() {
        viewModelScope.launch {
            _isLoading.value = true
            kotlinx.coroutines.delay(200)
            _isLoading.value = false
        }
    }

    /**
     * 当前选中的联系人
     */
    private val _selectedContact = MutableStateFlow<ContactInfo?>(null)
    val selectedContact: StateFlow<ContactInfo?> = _selectedContact.asStateFlow()

    /**
     * 是否显示 Bottom Sheet
     */
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

    /**
     * 是否显示删除确认对话框
     */
    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    /**
     * 删除确认的类型和关联的习惯 ID
     */
    data class DeleteConfirmContext(
        val type: DeleteConfirmType,
        val habitId: UUID? = null,
        val contact: ContactInfo? = null
    )

    private val _deleteConfirmContext = MutableStateFlow<DeleteConfirmContext?>(null)
    val deleteConfirmContext: StateFlow<DeleteConfirmContext?> = _deleteConfirmContext.asStateFlow()

    enum class DeleteConfirmType {
        FROM_HABIT,
        FROM_ALL_HABITS
    }

    /**
     * 选择联系人并显示 Bottom Sheet
     */
    fun selectContact(contact: ContactInfo) {
        _selectedContact.value = contact
        _showBottomSheet.value = true
    }

    /**
     * 关闭 Bottom Sheet
     */
    fun closeBottomSheet() {
        _showBottomSheet.value = false
        _selectedContact.value = null
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmDialog(type: DeleteConfirmType, habitId: UUID? = null, contact: ContactInfo? = null) {
        _deleteConfirmContext.value = DeleteConfirmContext(type, habitId, contact)
        _showDeleteConfirmDialog.value = true
    }

    /**
     * 关闭删除确认对话框
     */
    fun closeDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = false
        _deleteConfirmContext.value = null
    }

    /**
     * 从指定习惯删除联系人
     */
    fun deleteContactFromHabit(habitId: UUID, contact: ContactInfo? = null) {
        val contactToDelete = contact ?: _selectedContact.value ?: return

        viewModelScope.launch {
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                val updatedHabit = when (contactToDelete.type) {
                    ContactType.EMAIL -> {
                        val emails = habit.getSupervisorEmailsList().filter { it != contactToDelete.value }
                        habit.copyWithSupervisorEmails(emails)
                    }
                    ContactType.PHONE -> {
                        val phones = habit.getSupervisorPhonesList().filter { it != contactToDelete.value }
                        habit.copyWithSupervisorPhones(phones)
                    }
                }

                // 如果删除后没有联系人了，将监督方式设为 NONE
                val hasOtherContacts = when (contactToDelete.type) {
                    ContactType.EMAIL -> {
                        updatedHabit.getSupervisorEmailsList().isNotEmpty() ||
                            updatedHabit.getSupervisorPhonesList().isNotEmpty()
                    }
                    ContactType.PHONE -> {
                        updatedHabit.getSupervisorPhonesList().isNotEmpty() ||
                            updatedHabit.getSupervisorEmailsList().isNotEmpty()
                    }
                }

                val finalHabit = if (!hasOtherContacts) {
                    updatedHabit.copy(supervisionMethod = SupervisionMethod.NONE)
                } else {
                    updatedHabit
                }

                repository.updateHabit(finalHabit)

                // 更新选中的联系人信息
                if (_selectedContact.value != null) {
                    _selectedContact.value = _selectedContact.value!!.copy(
                        habitIds = _selectedContact.value!!.habitIds.filter { it != habitId }
                    )
                }

                // 如果该联系人不再用于任何习惯，关闭 Bottom Sheet
                if (_selectedContact.value?.habitIds?.isEmpty() == true) {
                    closeBottomSheet()
                }
            }
        }
    }

    /**
     * 从全部习惯删除联系人
     */
    fun deleteContactFromAllHabits(contact: ContactInfo? = null) {
        val contactToDelete = contact ?: _selectedContact.value ?: return

        viewModelScope.launch {
            contactToDelete.habitIds.forEach { habitId ->
                val habit = repository.getHabitById(habitId)
                if (habit != null) {
                    val updatedHabit = when (contactToDelete.type) {
                        ContactType.EMAIL -> {
                            val emails = habit.getSupervisorEmailsList().filter { it != contactToDelete.value }
                            habit.copyWithSupervisorEmails(emails)
                        }
                        ContactType.PHONE -> {
                            val phones = habit.getSupervisorPhonesList().filter { it != contactToDelete.value }
                            habit.copyWithSupervisorPhones(phones)
                        }
                    }

                    val finalHabit = updatedHabit.copy(supervisionMethod = SupervisionMethod.NONE)
                    repository.updateHabit(finalHabit)
                }
            }

            closeBottomSheet()
        }
    }

    /**
     * ViewModel Provider Factory
     */
    class Factory(private val application: HabitPulseApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
                return ContactsViewModel(application.repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
