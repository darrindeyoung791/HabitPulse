package io.github.darrindeyoung791.habitpulse.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.darrindeyoung791.habitpulse.data.database.converter.EnumConverters
import io.github.darrindeyoung791.habitpulse.data.database.converter.ListConverters
import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao
import io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion

/**
 * HabitPulse Room 数据库
 *
 * 数据库版本：5
 * 包含：habits 表、habit_completions 表
 * 
 * Version history:
 * - v1: Initial schema with habits table
 * - v2: Added habit_completions table
 * - v3: Added sortOrder and timeZone columns to habits table
 * - v4: Added slotTime and isLate columns to habit_completions table for slot-based check-in
 * - v5: Removed supervisionMethod column from habits table for mixed contact support
 */
@Database(
    entities = [Habit::class, HabitCompletion::class],
    version = 5,
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

    /**
     * 获取 HabitCompletionDao 实例
     */
    abstract fun habitCompletionDao(): HabitCompletionDao

    companion object {
        const val DATABASE_NAME = "habitpulse_database"

        /**
         * Migration from version 2 to 3
         * Adds sortOrder and timeZone columns to habits table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sortOrder column with default value 0
                database.execSQL("ALTER TABLE habits ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                // Add timeZone column with default value (will be set at runtime)
                database.execSQL("ALTER TABLE habits ADD COLUMN timeZone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habit_completions ADD COLUMN slotTime TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE habit_completions ADD COLUMN isLate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habits DROP COLUMN supervisionMethod")
            }
        }
    }
}
