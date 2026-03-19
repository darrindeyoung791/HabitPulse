package io.github.darrindeyoung791.habitpulse.data.repository

import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 习惯数据仓库
 * 
 * 作为数据层的单一事实来源，抽象数据库操作细节
 * 为 ViewModel 提供简洁的数据访问接口
 * 
 * @param habitDao 习惯数据访问对象
 */
class HabitRepository(private val habitDao: HabitDao) {
    
    /**
     * 获取所有习惯的 Flow
     * 数据变化时会自动更新
     */
    val allHabitsFlow: Flow<List<Habit>> = habitDao.getAllHabitsFlow()
    
    /**
     * 获取所有习惯（一次性）
     */
    suspend fun getAllHabits(): List<Habit> = habitDao.getAllHabits()
    
    /**
     * 根据 ID 获取习惯的 Flow
     */
    fun getHabitByIdFlow(id: UUID): Flow<Habit?> = habitDao.getHabitByIdFlow(id)
    
    /**
     * 根据 ID 获取习惯（一次性）
     */
    suspend fun getHabitById(id: UUID): Habit? = habitDao.getHabitById(id)
    
    /**
     * 获取未完成习惯的 Flow
     */
    val incompleteHabitsFlow: Flow<List<Habit>> = habitDao.getIncompleteHabitsFlow()
    
    /**
     * 获取已完成习惯的 Flow
     */
    val completedHabitsFlow: Flow<List<Habit>> = habitDao.getCompletedHabitsFlow()
    
    /**
     * 获取习惯总数的 Flow
     */
    val habitCountFlow: Flow<Int> = habitDao.getHabitCount()
    
    /**
     * 插入习惯
     * 
     * @param habit 要插入的习惯
     * @return 插入后的习惯（包含生成的 ID）
     */
    suspend fun insertHabit(habit: Habit): UUID {
        habitDao.insert(habit)
        return habit.id
    }
    
    /**
     * 更新习惯
     * 
     * @param habit 要更新的习惯
     */
    suspend fun updateHabit(habit: Habit) {
        habitDao.update(habit)
    }
    
    /**
     * 删除习惯
     * 
     * @param habit 要删除的习惯
     */
    suspend fun deleteHabit(habit: Habit) {
        habitDao.delete(habit)
    }
    
    /**
     * 删除所有习惯
     */
    suspend fun deleteAllHabits() {
        habitDao.deleteAll()
    }
    
    /**
     * 更新习惯的完成状态
     * 
     * @param id 习惯 ID
     * @param completed 是否完成
     */
    suspend fun updateCompletionStatus(id: UUID, completed: Boolean) {
        habitDao.updateCompletionStatus(id, completed, System.currentTimeMillis())
    }
    
    /**
     * 切换习惯的完成状态
     *
     * @param habit 习惯对象
     */
    suspend fun toggleCompletionStatus(habit: Habit) {
        val newStatus = !habit.completedToday
        updateCompletionStatus(habit.id, newStatus)
    }

    /**
     * 增加习惯的完成次数（打卡）
     *
     * @param habit 习惯对象
     */
    suspend fun incrementCompletionCount(habit: Habit) {
        habitDao.incrementCompletionCount(habit.id, System.currentTimeMillis())
    }

    /**
     * 撤销习惯的完成状态（completionCount 减 1）
     *
     * @param habit 习惯对象
     */
    suspend fun undoCompletionStatus(habit: Habit) {
        habitDao.undoCompletionStatus(habit.id, System.currentTimeMillis())
    }

    /**
     * 重置所有习惯的完成状态（用于每日重置）
     */
    suspend fun resetAllCompletionStatus() {
        habitDao.resetAllCompletionStatus(System.currentTimeMillis())
    }
}
