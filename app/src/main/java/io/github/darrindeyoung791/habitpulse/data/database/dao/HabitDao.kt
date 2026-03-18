package io.github.darrindeyoung791.habitpulse.data.database.dao

import androidx.room.*
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 习惯数据访问对象
 * 提供对 Habit 表的 CRUD 操作
 */
@Dao
interface HabitDao {
    
    /**
     * 获取所有习惯（按创建日期倒序）
     */
    @Query("SELECT * FROM habits ORDER BY createdDate DESC")
    fun getAllHabitsFlow(): Flow<List<Habit>>
    
    /**
     * 获取所有习惯（同步版本，用于后台操作）
     */
    @Query("SELECT * FROM habits ORDER BY createdDate DESC")
    suspend fun getAllHabits(): List<Habit>
    
    /**
     * 根据 ID 获取习惯
     */
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitByIdFlow(id: UUID): Flow<Habit?>
    
    /**
     * 根据 ID 获取习惯（同步版本）
     */
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: UUID): Habit?
    
    /**
     * 获取今日未完成的所有习惯
     */
    @Query("SELECT * FROM habits WHERE completedToday = 0 ORDER BY createdDate DESC")
    fun getIncompleteHabitsFlow(): Flow<List<Habit>>
    
    /**
     * 获取今日已完成的所有习惯
     */
    @Query("SELECT * FROM habits WHERE completedToday = 1 ORDER BY createdDate DESC")
    fun getCompletedHabitsFlow(): Flow<List<Habit>>
    
    /**
     * 插入习惯
     * @return 插入行的 rowId（对于 UUID 主键，此值无实际意义）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long
    
    /**
     * 更新习惯
     */
    @Update
    suspend fun update(habit: Habit)
    
    /**
     * 删除习惯
     */
    @Delete
    suspend fun delete(habit: Habit)
    
    /**
     * 删除所有习惯
     */
    @Query("DELETE FROM habits")
    suspend fun deleteAll()
    
    /**
     * 更新习惯的今日完成状态
     */
    @Query("UPDATE habits SET completedToday = :completed, lastCompletedDate = :timestamp, completionCount = completionCount + CASE WHEN :completed = 1 THEN 1 ELSE 0 END, modifiedDate = :timestamp WHERE id = :id")
    suspend fun updateCompletionStatus(id: UUID, completed: Boolean, timestamp: Long)
    
    /**
     * 重置所有习惯的今日完成状态（用于每日重置）
     */
    @Query("UPDATE habits SET completedToday = 0, modifiedDate = :timestamp")
    suspend fun resetAllCompletionStatus(timestamp: Long)
    
    /**
     * 获取习惯总数
     */
    @Query("SELECT COUNT(*) FROM habits")
    fun getHabitCount(): Flow<Int>
}
