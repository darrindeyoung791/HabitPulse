package com.ddy.habitpulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddy.habitpulse.db.Habit
import com.ddy.habitpulse.db.HabitDatabase
import com.ddy.habitpulse.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HabitListViewModel(applicationContext: android.content.Context) : ViewModel() {
    private val habitDao = HabitDatabase.getDatabase(applicationContext).habitDao()
    private val habitRepository = HabitRepository(habitDao)
    
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()
    
    init {
        loadHabits()
    }
    
    fun loadHabits() {
        viewModelScope.launch {
            _habits.value = habitRepository.getAllHabits()
        }
    }
    
    fun addHabit(habit: Habit) {
        viewModelScope.launch {
            val id = habitRepository.insertHabit(habit)
            loadHabits() // Refresh the list
        }
    }
    
    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.updateHabit(habit)
            loadHabits() // Refresh the list
        }
    }
    
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
            loadHabits() // Refresh the list
        }
    }
    
    fun deleteHabitById(id: Long) {
        viewModelScope.launch {
            habitRepository.deleteHabitById(id)
            loadHabits() // Refresh the list
        }
    }
    
    fun updateHabitCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            if (completed) {
                // When marking as completed, increment the count
                habitRepository.updateHabitCompletedWithIncrement(id, completed)
            } else {
                // When marking as incomplete, just update the completed status
                habitRepository.updateHabitCompleted(id, completed)
            }
            loadHabits() // Refresh the list
        }
    }
}