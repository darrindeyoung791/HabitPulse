package com.ddy.habitpulse.db

import androidx.room.*

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
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
}