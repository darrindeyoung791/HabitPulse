package io.github.darrindeyoung791.habitpulse.data.repository

import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao
import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 习惯数据仓库
 *
 * 作为数据层的单一事实来源，抽象数据库操作细节
 * 为 ViewModel 提供简洁的数据访问接口
 *
 * @param habitDao 习惯数据访问对象
 * @param habitCompletionDao 习惯打卡记录数据访问对象
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao
) {

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
     * 获取指定习惯的所有打卡记录 Flow
     */
    fun getCompletionsByHabitIdFlow(habitId: UUID): Flow<List<HabitCompletion>> =
        habitCompletionDao.getCompletionsByHabitIdFlow(habitId)

    /**
     * 获取所有打卡记录 Flow（按时间倒序）
     */
    val allCompletionsFlow: Flow<List<HabitCompletion>> =
        habitCompletionDao.getAllCompletionsFlow()

    /**
     * 获取指定习惯的所有打卡记录（一次性）
     */
    suspend fun getCompletionsByHabitId(habitId: UUID): List<HabitCompletion> =
        habitCompletionDao.getCompletionsByHabitId(habitId)

    /**
     * 获取指定习惯在指定日期的打卡记录
     */
    suspend fun getCompletionsByHabitIdAndDate(
        habitId: UUID,
        date: String
    ): List<HabitCompletion> =
        habitCompletionDao.getCompletionsByHabitIdAndDate(habitId, date)

    /**
     * 获取指定习惯今天的打卡记录数量
     */
    suspend fun getTodayCompletionCount(habitId: UUID): Int =
        habitCompletionDao.getTodayCompletionCount(habitId, HabitCompletion.getTodayDate())

    /**
     * 获取今天所有打卡记录
     */
    suspend fun getTodayCompletions(): List<HabitCompletion> =
        habitCompletionDao.getCompletionsByDate(HabitCompletion.getTodayDate())

    /**
     * 获取指定日期所有打卡记录
     */
    suspend fun getCompletionsByDate(date: String): List<HabitCompletion> =
        habitCompletionDao.getCompletionsByDate(date)

    /**
     * 获取指定习惯在指定日期指定时段的打卡记录
     */
    suspend fun getCompletionByHabitIdDateAndSlot(habitId: UUID, date: String, slotTime: String): HabitCompletion? =
        habitCompletionDao.getCompletionByHabitIdDateAndSlot(habitId, date, slotTime)

    /**
     * 获取习惯总数的 Flow
     */
    val completionCountFlow: Flow<Int> = habitCompletionDao.getCompletionCount()

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
     * 同时更新 Habit 表的统计信息并插入打卡记录
     *
     * @param habit 习惯对象
     * @return 新插入的打卡记录
     */
    suspend fun incrementCompletionCount(habit: Habit): HabitCompletion {
        val timestamp = System.currentTimeMillis()
        val todayDate = HabitCompletion.getTodayDate()

        // 更新 Habit 表的统计信息
        habitDao.incrementCompletionCount(habit.id, timestamp)

        // 插入打卡记录
        val completion = HabitCompletion(
            habitId = habit.id,
            completedDate = timestamp,
            completedDateLocal = todayDate
        )
        habitCompletionDao.insert(completion)

        return completion
    }

    /**
     * 按 Slot 打卡（新打卡方式）
     * 分配打卡到指定提醒时段，标记是否逾期补卡
     *
     * @param habit 习惯对象
     * @param slotTime 目标提醒时段 (如 "08:00")
     * @param isLate 是否为逾期补卡
     * @param isAllCompleted 是否所有时段都已打卡完成
     * @return 新插入的打卡记录
     */
    suspend fun performSlotCheckIn(
        habit: Habit,
        slotTime: String,
        isLate: Boolean,
        isAllCompleted: Boolean
    ): HabitCompletion {
        val timestamp = System.currentTimeMillis()
        val todayDate = HabitCompletion.getTodayDate()

        habitDao.incrementCompletionCountWithCompleted(habit.id, isAllCompleted, timestamp)

        val completion = HabitCompletion(
            habitId = habit.id,
            completedDate = timestamp,
            completedDateLocal = todayDate,
            slotTime = slotTime,
            isLate = isLate
        )
        habitCompletionDao.insert(completion)

        return completion
    }

    /**
     * 撤销习惯的完成状态（completionCount 减 1）
     * 同时删除该习惯最近一次的打卡记录（不限日期）
     *
     * @param habit 习惯对象
     */
    suspend fun undoCompletionStatus(habit: Habit) {
        val timestamp = System.currentTimeMillis()

        val allCompletions = habitCompletionDao.getCompletionsByHabitId(habit.id)
            .sortedByDescending { it.completedDate }

        if (allCompletions.isNotEmpty()) {
            val lastCompletion = allCompletions.first()
            habitCompletionDao.delete(lastCompletion)

            val remainingToday = habitCompletionDao.getCompletionsByHabitIdAndDate(
                habit.id, HabitCompletion.getTodayDate()
            )
            val allSlots = habit.getReminderTimesList()
            val completedSlotTimes = remainingToday
                .filter { it.slotTime.isNotEmpty() }
                .map { it.slotTime }
                .toSet()
            val isAllCompleted = allSlots.isNotEmpty() && allSlots.all { it in completedSlotTimes }

            habitDao.undoSlotCompletion(habit.id, isAllCompleted, timestamp)
        }
    }

    /**
     * 重置所有习惯的完成状态（用于每日重置）
     */
    suspend fun resetAllCompletionStatus() {
        habitDao.resetAllCompletionStatus(System.currentTimeMillis())
    }

    /**
     * 搜索习惯（根据标题或备注）
     * @param query 搜索关键词
     */
    fun searchHabitsFlow(query: String): Flow<List<Habit>> {
        val searchQuery = "%${query}%"
        return habitDao.searchHabitsFlow(searchQuery)
    }

    /**
     * 获取按 sortOrder 排序的所有习惯 Flow
     */
    val habitsBySortOrderFlow: Flow<List<Habit>> = habitDao.getHabitsBySortOrderFlow()

    /**
     * 更新习惯的排序顺序
     */
    suspend fun updateHabitSortOrder(id: UUID, sortOrder: Int) {
        habitDao.updateSortOrder(id, sortOrder, System.currentTimeMillis())
    }

    /**
     * 批量更新多个习惯的排序顺序
     */
    suspend fun updateMultipleHabitSortOrders(habitsWithOrder: List<Pair<UUID, Int>>) {
        habitDao.updateMultipleSortOrders(habitsWithOrder)
    }

    /**
     * 根据 ID 列表删除多个习惯
     */
    suspend fun deleteHabitsByIds(habitIds: Set<UUID>) {
        habitDao.deleteHabitsByIds(habitIds)
    }
}
