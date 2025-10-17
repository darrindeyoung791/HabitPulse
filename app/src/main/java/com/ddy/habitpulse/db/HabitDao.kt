package com.ddy.habitpulse.db

import androidx.room.*

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdDate DESC")
    suspend fun getAllHabits(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("UPDATE habits SET completed = :completed WHERE id = :id")
    suspend fun updateHabitCompleted(id: Long, completed: Boolean)
    
    @Query("UPDATE habits SET completed = :completed, completionCount = CASE WHEN :completed = 1 THEN completionCount + 1 ELSE completionCount END WHERE id = :id")
    suspend fun updateHabitCompletedAndCount(id: Long, completed: Boolean)
    
    @Query("UPDATE habits SET completionCount = :completionCount WHERE id = :id")
    suspend fun updateHabitCompletionCount(id: Long, completionCount: Int)
}