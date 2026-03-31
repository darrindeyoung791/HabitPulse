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
     * 获取所有习惯（按最后修改日期倒序）
     */
    @Query("SELECT * FROM habits ORDER BY modifiedDate DESC")
    fun getAllHabitsFlow(): Flow<List<Habit>>

    /**
     * 获取所有习惯（同步版本，用于后台操作）
     */
    @Query("SELECT * FROM habits ORDER BY modifiedDate DESC")
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
    @Query("SELECT * FROM habits WHERE completedToday = 0 ORDER BY modifiedDate DESC")
    fun getIncompleteHabitsFlow(): Flow<List<Habit>>

    /**
     * 获取今日已完成的所有习惯
     */
    @Query("SELECT * FROM habits WHERE completedToday = 1 ORDER BY modifiedDate DESC")
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
     * 撤销习惯的今日完成状态（ completionCount 减 1）
     */
    @Query("UPDATE habits SET completedToday = 0, completionCount = MAX(0, completionCount - 1), modifiedDate = :timestamp WHERE id = :id")
    suspend fun undoCompletionStatus(id: UUID, timestamp: Long)

    /**
     * 增加习惯的完成次数（打卡）
     */
    @Query("UPDATE habits SET completedToday = 1, completionCount = completionCount + 1, lastCompletedDate = :timestamp, modifiedDate = :timestamp WHERE id = :id")
    suspend fun incrementCompletionCount(id: UUID, timestamp: Long)

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

    /**
     * 搜索习惯（根据标题或备注）
     * @param query 搜索关键词，使用 LIKE 匹配（自动添加 % 通配符）
     */
    @Query("SELECT * FROM habits WHERE title LIKE :query OR notes LIKE :query ORDER BY modifiedDate DESC")
    fun searchHabitsFlow(query: String): Flow<List<Habit>>
}
