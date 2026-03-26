package io.github.darrindeyoung791.habitpulse.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 习惯打卡记录实体类
 *
 * 记录每一次习惯打卡的详细信息，包括打卡时间
 *
 * @property id 打卡记录唯一标识符 (UUID)
 * @property habitId 关联的习惯 ID (外键)
 * @property completedDate 打卡完成时间戳
 * @property completedDateLocal 打卡完成日期的本地表示 (yyyy-MM-dd 格式，用于按日期查询)
 * @property timeZone 打卡时的时区 ID，用于处理跨时区场景
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["completedDateLocal"])
    ]
)
data class HabitCompletion(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    val habitId: UUID,

    val completedDate: Long = System.currentTimeMillis(),

    val completedDateLocal: String,

    val timeZone: String = java.util.TimeZone.getDefault().id
) {
    /**
     * 获取打卡日期（yyyy-MM-dd 格式）
     */
    fun getFormattedDate(): String = completedDateLocal

    companion object {
        /**
         * 获取今天的日期字符串（yyyy-MM-dd 格式）
         */
        fun getTodayDate(): String {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            return String.format("%04d-%02d-%02d", year, month, day)
        }
    }
}
