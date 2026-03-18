package io.github.darrindeyoung791.habitpulse.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.darrindeyoung791.habitpulse.data.database.converter.EnumConverters
import io.github.darrindeyoung791.habitpulse.data.database.converter.ListConverters
import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao
import io.github.darrindeyoung791.habitpulse.data.model.Habit

/**
 * HabitPulse Room 数据库
 * 
 * 数据库版本：1
 * 包含：habits 表
 */
@Database(
    entities = [Habit::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    ListConverters::class,
    EnumConverters::class
)
abstract class HabitDatabase : RoomDatabase() {
    
    /**
     * 获取 HabitDao 实例
     */
    abstract fun habitDao(): HabitDao
    
    companion object {
        const val DATABASE_NAME = "habitpulse_database"
    }
}
