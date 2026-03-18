package io.github.darrindeyoung791.habitpulse.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.util.UUID

/**
 * 习惯重复周期
 */
enum class RepeatCycle {
    DAILY,   // 每日
    WEEKLY   // 每周
}

/**
 * 监督方式
 */
enum class SupervisionMethod {
    NONE,      // 不监督，仅本地提醒
    EMAIL,     // 邮件汇报
    SMS        // 短信汇报
}

/**
 * 习惯实体类
 * 
 * @property id 习惯唯一标识符 (UUID)
 * @property title 习惯标题
 * @property repeatCycle 重复周期 (DAILY/WEEKLY)
 * @property repeatDays 重复日期 (JSON 格式，如 [1,3,5] 表示周一、三、五)
 * @property reminderTimes 提醒时间列表 (JSON 格式，如 ["08:00","20:00"])
 * @property notes 备注信息
 * @property supervisionMethod 监督方式
 * @property supervisorEmails 监督人邮箱列表 (JSON 格式)
 * @property supervisorPhones 监督人电话列表 (JSON 格式)
 * @property completedToday 今日是否已完成
 * @property completionCount 总完成次数
 * @property lastCompletedDate 最后完成日期时间戳
 * @property createdDate 创建日期时间戳
 * @property modifiedDate 最后修改日期时间戳
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    
    val title: String,
    
    val repeatCycle: RepeatCycle = RepeatCycle.DAILY,
    
    val repeatDays: String = "[]",  // JSON format: [0,1,2,3,4,5,6]
    
    val reminderTimes: String = "[]",  // JSON format: ["08:00","20:00"]
    
    val notes: String = "",
    
    val supervisionMethod: SupervisionMethod = SupervisionMethod.NONE,
    
    val supervisorEmails: String = "[]",  // JSON format: ["email1@example.com"]
    
    val supervisorPhones: String = "[]",  // JSON format: ["+1234567890"]
    
    val completedToday: Boolean = false,
    
    val completionCount: Int = 0,
    
    val lastCompletedDate: Long? = null,
    
    val createdDate: Long = System.currentTimeMillis(),
    
    val modifiedDate: Long = System.currentTimeMillis()
) {
    
    // ============ Helper methods for list properties ============
    
    fun getRepeatDaysList(): List<Int> {
        val result = mutableListOf<Int>()
        val jsonArray = JSONArray(repeatDays)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getInt(i))
        }
        return result
    }
    
    fun getReminderTimesList(): List<String> {
        val result = mutableListOf<String>()
        val jsonArray = JSONArray(reminderTimes)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
    
    fun getSupervisorEmailsList(): List<String> {
        val result = mutableListOf<String>()
        val jsonArray = JSONArray(supervisorEmails)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
    
    fun getSupervisorPhonesList(): List<String> {
        val result = mutableListOf<String>()
        val jsonArray = JSONArray(supervisorPhones)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
    
    fun copyWithRepeatDays(days: List<Int>): Habit {
        val jsonArray = JSONArray()
        days.forEach { jsonArray.put(it) }
        return copy(repeatDays = jsonArray.toString(), modifiedDate = System.currentTimeMillis())
    }
    
    fun copyWithReminderTimes(times: List<String>): Habit {
        val jsonArray = JSONArray()
        times.forEach { jsonArray.put(it) }
        return copy(reminderTimes = jsonArray.toString(), modifiedDate = System.currentTimeMillis())
    }
    
    fun copyWithSupervisorEmails(emails: List<String>): Habit {
        val jsonArray = JSONArray()
        emails.forEach { jsonArray.put(it) }
        return copy(supervisorEmails = jsonArray.toString(), modifiedDate = System.currentTimeMillis())
    }
    
    fun copyWithSupervisorPhones(phones: List<String>): Habit {
        val jsonArray = JSONArray()
        phones.forEach { jsonArray.put(it) }
        return copy(supervisorPhones = jsonArray.toString(), modifiedDate = System.currentTimeMillis())
    }
}
