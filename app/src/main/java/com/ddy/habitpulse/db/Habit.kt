package com.ddy.habitpulse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ddy.habitpulse.enums.RepeatCycle
import com.ddy.habitpulse.enums.SupervisionMethod

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val repeatCycle: RepeatCycle,
    val repeatDays: List<Int>, // For weekly habits, which days (0=Monday, 6=Sunday) 
    val reminderTimes: List<String>, // List of time strings in "HH:mm" format
    val notes: String = "",
    val supervisionMethod: SupervisionMethod = SupervisionMethod.LOCAL_NOTIFICATION_ONLY,
    val supervisorPhoneNumbers: List<String> = emptyList() // List of phone numbers for SMS supervision
)