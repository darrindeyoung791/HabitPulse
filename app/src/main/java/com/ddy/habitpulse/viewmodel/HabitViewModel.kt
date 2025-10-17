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
    private val _id = mutableStateOf<Long>(0) // 0 means creating new habit, >0 means editing existing
    val id: Long
        get() = _id.value

    fun setId(newId: Long) {
        _id.value = newId
    }
    
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

    fun setSupervisorPhoneNumbers(numbers: List<String>) {
        _supervisorPhoneNumbers.value = numbers
    }

    fun setTitle(title: String) {
        val cleanTitle = title.replace("\n", "").take(200) // Remove newlines and limit to 200 chars
        _title.value = cleanTitle
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
        val cleanNotes = notes.take(2000) // Limit to 2000 chars but allow newlines
        _notes.value = cleanNotes
    }

    fun setSupervisionMethod(method: SupervisionMethod) {
        _supervisionMethod.value = method
        // When changing to SMS reporting, do not add an empty supervisor by default
        if (method == SupervisionMethod.SMS_REPORTING && _supervisorPhoneNumbers.value.isEmpty()) {
            // Don't add empty supervisor by default
            // _supervisorPhoneNumbers.value = listOf("")
        }
    }

    fun addSupervisorPhoneNumber(phone: String) {
        val cleanPhone = phone.replace("\n", "").take(20) // Remove newlines and limit to 20 chars
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        newPhones.add(cleanPhone)
        _supervisorPhoneNumbers.value = newPhones
    }

    fun removeSupervisorPhoneNumber(phone: String) {
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        newPhones.remove(phone)
        _supervisorPhoneNumbers.value = newPhones
    }

    fun updateSupervisorPhoneNumber(index: Int, newPhone: String) {
        val cleanPhone = newPhone.replace("\n", "").take(20) // Remove newlines and limit to 20 chars
        val newPhones = _supervisorPhoneNumbers.value.toMutableList()
        if (index < newPhones.size) {
            newPhones[index] = cleanPhone
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
                id = _id.value,
                title = title,
                repeatCycle = repeatCycle,
                repeatDays = if (repeatCycle == RepeatCycle.WEEKLY) selectedDays else emptyList(),
                reminderTimes = reminderTimes,
                notes = notes,
                supervisionMethod = supervisionMethod,
                supervisorPhoneNumbers = if (supervisionMethod == SupervisionMethod.SMS_REPORTING) supervisorPhoneNumbers else emptyList(),
                completed = false, // Default to not completed
                completionCount = 0 // Default to 0 completions
            )
            
            if (_id.value > 0) {
                // If ID > 0, we're updating an existing habit
                repository.updateHabit(habit)
            } else {
                // If ID is 0, we're creating a new habit
                repository.insertHabit(habit)
            }
            
            // Reset form after saving
            resetForm()
        }
    }

    fun loadHabitForEdit(habitId: Long) {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId)
            habit?.let {
                _id.value = it.id
                _title.value = it.title
                _repeatCycle.value = it.repeatCycle
                _selectedDays.value = it.repeatDays
                _reminderTimes.value = it.reminderTimes
                _notes.value = it.notes
                _supervisionMethod.value = it.supervisionMethod
                setSupervisorPhoneNumbers(it.supervisorPhoneNumbers)
            }
        }
    }
    
    fun resetForm() {
        _id.value = 0L
        _title.value = ""
        _repeatCycle.value = RepeatCycle.DAILY
        _selectedDays.value = emptyList()
        _reminderTimes.value = emptyList()
        _notes.value = ""
        _supervisionMethod.value = SupervisionMethod.LOCAL_NOTIFICATION_ONLY
        _supervisorPhoneNumbers.value = emptyList()
    }
    
    fun isFormValid(): Boolean {
        // For daily repeat cycle, at least one reminder time is required
        val hasValidReminderTime = if (_repeatCycle.value == RepeatCycle.DAILY) {
            _reminderTimes.value.isNotEmpty()
        } else if (_repeatCycle.value == RepeatCycle.WEEKLY) {
            _selectedDays.value.isNotEmpty() && _reminderTimes.value.isNotEmpty()
        } else {
            true // For other cases
        }
        
        return _title.value.isNotBlank() &&
                (_repeatCycle.value == RepeatCycle.DAILY || (_repeatCycle.value == RepeatCycle.WEEKLY && _selectedDays.value.isNotEmpty())) &&
                hasValidReminderTime &&
                (_supervisionMethod.value == SupervisionMethod.LOCAL_NOTIFICATION_ONLY || 
                 (_supervisionMethod.value == SupervisionMethod.SMS_REPORTING && _supervisorPhoneNumbers.value.isNotEmpty() && _supervisorPhoneNumbers.value.all { it.isNotBlank() }))
    }
    
    fun updateHabitCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.updateHabitCompleted(id, completed)
        }
    }
}
