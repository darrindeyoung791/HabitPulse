package com.ddy.habitpulse.repository

import com.ddy.habitpulse.db.HabitDao
import com.ddy.habitpulse.db.Habit

class HabitRepository(
    private val habitDao: HabitDao
) {
    suspend fun getAllHabits(): List<Habit> = habitDao.getAllHabits()
    
    suspend fun getHabitById(id: Long): Habit? = habitDao.getHabitById(id)
    
    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)
    
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    
    suspend fun deleteHabitById(id: Long) = habitDao.deleteHabitById(id)
    
    suspend fun updateHabitCompleted(id: Long, completed: Boolean) = 
        habitDao.updateHabitCompleted(id, completed)
    
    suspend fun updateHabitCompletedWithIncrement(id: Long, completed: Boolean) =
        habitDao.updateHabitCompletedAndCount(id, completed)
        
    suspend fun updateHabitCompletionCount(id: Long, completionCount: Int) =
        habitDao.updateHabitCompletionCount(id, completionCount)
}