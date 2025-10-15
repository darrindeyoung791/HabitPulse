package com.ddy.habitpulse.repository

import com.ddy.habitpulse.db.Habit
import com.ddy.habitpulse.db.HabitDao

class HabitRepository(private val habitDao: HabitDao) {
    suspend fun getAllHabits() = habitDao.getAllHabits()
    
    suspend fun getHabitById(id: Long) = habitDao.getHabitById(id)
    
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    
    suspend fun deleteHabitById(id: Long) = habitDao.deleteHabitById(id)
}