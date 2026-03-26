package io.github.darrindeyoung791.habitpulse.data.database.dao

import androidx.room.*
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 习惯打卡记录数据访问对象
 * 提供对 HabitCompletion 表的 CRUD 操作
 */
@Dao
interface HabitCompletionDao {

    /**
     * 获取指定习惯的所有打卡记录（按完成时间倒序）
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getCompletionsByHabitIdFlow(habitId: UUID): Flow<List<HabitCompletion>>

    /**
     * 获取指定习惯的所有打卡记录（同步版本）
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    suspend fun getCompletionsByHabitId(habitId: UUID): List<HabitCompletion>

    /**
     * 获取指定习惯在指定日期的打卡记录
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDateLocal = :date ORDER BY completedDate DESC")
    suspend fun getCompletionsByHabitIdAndDate(habitId: UUID, date: String): List<HabitCompletion>

    /**
     * 获取指定习惯在指定日期范围内的打卡记录
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDateLocal BETWEEN :startDate AND :endDate ORDER BY completedDate DESC")
    suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: UUID,
        startDate: String,
        endDate: String
    ): List<HabitCompletion>

    /**
     * 获取所有习惯在指定日期的打卡记录
     */
    @Query("SELECT * FROM habit_completions WHERE completedDateLocal = :date ORDER BY completedDate DESC")
    suspend fun getCompletionsByDate(date: String): List<HabitCompletion>

    /**
     * 获取指定习惯今天的打卡记录数量
     */
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completedDateLocal = :date")
    suspend fun getTodayCompletionCount(habitId: UUID, date: String): Int

    /**
     * 插入打卡记录
     * @return 插入行的 rowId
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: HabitCompletion): Long

    /**
     * 批量插入打卡记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<HabitCompletion>)

    /**
     * 删除打卡记录
     */
    @Delete
    suspend fun delete(completion: HabitCompletion)

    /**
     * 删除指定习惯的所有打卡记录
     */
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteByHabitId(habitId: UUID)

    /**
     * 删除指定日期的打卡记录
     */
    @Query("DELETE FROM habit_completions WHERE completedDateLocal = :date")
    suspend fun deleteByDate(date: String)

    /**
     * 删除所有打卡记录
     */
    @Query("DELETE FROM habit_completions")
    suspend fun deleteAll()

    /**
     * 获取打卡记录总数
     */
    @Query("SELECT COUNT(*) FROM habit_completions")
    fun getCompletionCount(): Flow<Int>

    /**
     * 获取指定习惯的打卡记录总数
     */
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getCompletionCountByHabitId(habitId: UUID): Int
}
