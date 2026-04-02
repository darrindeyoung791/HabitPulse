package io.github.darrindeyoung791.habitpulse

import android.app.Application
import androidx.room.Room
import com.google.android.material.color.DynamicColors
import io.github.darrindeyoung791.habitpulse.data.database.HabitDatabase
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.ContactsViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.RecordsViewModel

/**
 * HabitPulse 应用程序类
 *
 * 负责初始化全局单例对象：
 * - Room 数据库
 * - 数据仓库
 * - ViewModel
 */
class HabitPulseApplication : Application() {

    // 数据库单例（延迟初始化）
    val database: HabitDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            HabitDatabase::class.java,
            HabitDatabase.DATABASE_NAME
        )
        .addMigrations(HabitDatabase.MIGRATION_2_3)  // Migration from v2 to v3
        .build()
    }

    // 仓库单例（延迟初始化）
    val repository: HabitRepository by lazy {
        HabitRepository(database.habitDao(), database.habitCompletionDao())
    }

    // 引导偏好设置（延迟初始化）
    val onboardingPreferences: OnboardingPreferences by lazy {
        OnboardingPreferences(applicationContext)
    }

    // HabitViewModel 单例（延迟初始化）
    val habitViewModel: HabitViewModel by lazy {
        HabitViewModel(repository, onboardingPreferences)
    }

    // RecordsViewModel 单例（延迟初始化）
    val recordsViewModel: RecordsViewModel by lazy {
        RecordsViewModel(repository)
    }

    // ContactsViewModel 单例（延迟初始化）
    val contactsViewModel: ContactsViewModel by lazy {
        ContactsViewModel(repository)
    }

    override fun onCreate() {
        super.onCreate()
        // Apply Material Dynamic Colors (Monet) to activities when available (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
