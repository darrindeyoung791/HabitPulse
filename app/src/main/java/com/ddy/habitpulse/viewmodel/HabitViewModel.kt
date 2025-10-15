package com.ddy.habitpulse.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddy.habitpulse.db.Habit
import com.ddy.habitpulse.db.HabitDatabase
import com.ddy.habitpulse.enums.RepeatCycle
import com.ddy.habitpulse.enums.SupervisionMethod
import com.ddy.habitpulse.repository.HabitRepository
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HabitRepository

    init {
        val habitDao = HabitDatabase.getDatabase(application).habitDao()
        repository = HabitRepository(habitDao)
    }

    // UI state variables using separate State objects to avoid property/setter conflicts
    private val _title = mutableStateOf("")
    val title: String
        get() = _title.value
    
    private val _repeatCycle = mutableStateOf(RepeatCycle.DAILY)
    val repeatCycle: RepeatCycle
        get() = _repeatCycle.value
        
    private val _selectedDays = mutableStateOf(listOf<Int>()) // 0=Monday, 6=Sunday
    val selectedDays: List<Int>
        get() = _selectedDays.value
        
    private val _reminderTimes = mutableStateOf(listOf<String>())
    val reminderTimes: List<String>
        get() = _reminderTimes.value

    private val _notes = mutableStateOf("")
    val notes: String
        get() = _notes.value
        
    private val _supervisionMethod = mutableStateOf(SupervisionMethod.LOCAL_NOTIFICATION_ONLY)
    val supervisionMethod: SupervisionMethod
        get() = _supervisionMethod.value
        
    private val _supervisorPhoneNumbers = mutableStateOf(listOf<String>())
    val supervisorPhoneNumbers: List<String>
        get() = _supervisorPhoneNumbers.value

    fun setTitle(title: String) {
        _title.value = title
    }

    fun setRepeatCycle(repeatCycle: RepeatCycle) {
        _repeatCycle.value = repeatCycle
        // When changing repeat cycle, reset related fields
        if (repeatCycle == RepeatCycle.WEEKLY) {
            // For weekly, by default select all days
            _selectedDays.value = (0..6).toList()
        } else {
            _selectedDays.value = emptyList()
        }
        // Reset reminder times when changing cycle
        _reminderTimes.value = emptyList()
    }

    fun setSelectedDay(dayIndex: Int, isSelected: Boolean) {
        val currentDays = _selectedDays.value.toMutableList()
        if (isSelected) {
            if (!currentDays.contains(dayIndex)) {
                currentDays.add(dayIndex)
                currentDays.sort()
            }
        } else {
            currentDays.remove(dayIndex)
        }
        _selectedDays.value = currentDays
    }

    fun addReminderTime(time: String) {
        val newTimes = _reminderTimes.value.toMutableList()
        newTimes.add(time)
        newTimes.sort() // Sort times in ascending order
        _reminderTimes.value = newTimes
    }

    fun removeReminderTime(time: String) {
        val newTimes = _reminderTimes.value.toMutableList()
        newTimes.remove(time)
        _reminderTimes.value = newTimes
    }

    fun setReminderTimes(times: List<String>) {
        _reminderTimes.value = times.toMutableList()
    }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun setSupervisionMethod(method: SupervisionMethod) {
        _supervisionMethod.value = method
        // When changing to SMS reporting, ensure we have at least one supervisor
        if (method == SupervisionMethod.SMS_REPORTING && _supervisorPhoneNumbers.value.isEmpty()) {
            _supervisorPhoneNumbers.value = listOf("")
        }
    }

    fun addSupervisorPhoneNumber(phone: String) {
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        newPhones.add(phone)
        _supervisorPhoneNumbers.value = newPhones
    }

    fun removeSupervisorPhoneNumber(phone: String) {
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        newPhones.remove(phone)
        _supervisorPhoneNumbers.value = newPhones
    }

    fun updateSupervisorPhoneNumber(index: Int, newPhone: String) {
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        if (index < newPhones.size) {
            newPhones[index] = newPhone
        }
        _supervisorPhoneNumbers.value = newPhones
    }

    fun saveHabit() {
        viewModelScope.launch {
            // Validate required fields
            if (title.isBlank()) {
                return@launch
            }
            
            if (repeatCycle == RepeatCycle.WEEKLY && selectedDays.isEmpty()) {
                return@launch
            }
            
            if (supervisionMethod == SupervisionMethod.SMS_REPORTING && supervisorPhoneNumbers.any { it.isBlank() }) {
                return@launch
            }

            val habit = Habit(
                title = title,
                repeatCycle = repeatCycle,
                repeatDays = if (repeatCycle == RepeatCycle.WEEKLY) selectedDays else emptyList(),
                reminderTimes = reminderTimes,
                notes = notes,
                supervisionMethod = supervisionMethod,
                supervisorPhoneNumbers = if (supervisionMethod == SupervisionMethod.SMS_REPORTING) supervisorPhoneNumbers else emptyList()
            )
            
            repository.insertHabit(habit)
            
            // Reset form after saving
            resetForm()
        }
    }

    fun resetForm() {
        _title.value = ""
        _repeatCycle.value = RepeatCycle.DAILY
        _selectedDays.value = emptyList()
        _reminderTimes.value = emptyList()
        _notes.value = ""
        _supervisionMethod.value = SupervisionMethod.LOCAL_NOTIFICATION_ONLY
        _supervisorPhoneNumbers.value = emptyList()
    }
    
    fun isFormValid(): Boolean {
        return _title.value.isNotBlank() &&
                (_repeatCycle.value == RepeatCycle.DAILY || (_repeatCycle.value == RepeatCycle.WEEKLY && _selectedDays.value.isNotEmpty())) &&
                (_supervisionMethod.value == SupervisionMethod.LOCAL_NOTIFICATION_ONLY || 
                 (_supervisionMethod.value == SupervisionMethod.SMS_REPORTING && _supervisorPhoneNumbers.value.isNotEmpty() && _supervisorPhoneNumbers.value.all { it.isNotBlank() }))
    }
}