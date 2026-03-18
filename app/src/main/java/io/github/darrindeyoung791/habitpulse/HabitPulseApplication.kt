package io.github.darrindeyoung791.habitpulse

import android.app.Application
import androidx.room.Room
import com.google.android.material.color.DynamicColors
import io.github.darrindeyoung791.habitpulse.data.database.HabitDatabase
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository

/**
 * HabitPulse 应用程序类
 * 
 * 负责初始化全局单例对象：
 * - Room 数据库
 * - 数据仓库
 */
class HabitPulseApplication : Application() {
    
    // 数据库单例（延迟初始化）
    val database: HabitDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            HabitDatabase::class.java,
            HabitDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()  // Alpha 阶段使用破坏性迁移
        .build()
    }
    
    // 仓库单例（延迟初始化）
    val repository: HabitRepository by lazy {
        HabitRepository(database.habitDao())
    }
    
    override fun onCreate() {
        super.onCreate()
        // Apply Material Dynamic Colors (Monet) to activities when available (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
