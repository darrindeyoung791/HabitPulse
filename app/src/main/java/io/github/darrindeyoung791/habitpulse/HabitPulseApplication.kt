package io.github.darrindeyoung791.habitpulse

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.color.DynamicColors
import io.github.darrindeyoung791.habitpulse.data.database.HabitDatabase
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.ContactsViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.RecordsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                // Enable foreign key constraints for cascade delete
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        })
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

        // Start foreground service on cold start if user had enabled it
        startForegroundServiceIfEnabled()
    }

    /**
     * 在冷启动时尝试启动前台通知服务
     *
     * 读取用户偏好设置，如果之前开启了持久通知且拥有通知权限，
     * 则自动启动前台服务以保持应用在后台运行。
     */
    private fun startForegroundServiceIfEnabled() {
        // Use a coroutine scope to read preferences asynchronously
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val userPreferences = UserPreferences.getInstance(applicationContext)
                val isPersistentNotificationEnabled = userPreferences.persistentNotificationFlow.first()

                // Only start service if user had enabled it AND has permission
                if (isPersistentNotificationEnabled && NotificationHelper.hasNotificationPermission(applicationContext)) {
                    NotificationHelper.createNotificationChannel(applicationContext)
                    ForegroundNotificationService.toggleService(applicationContext, enable = true)
                }
            } catch (e: Exception) {
                // Log error but don't crash the application
                android.util.Log.e("HabitPulseApplication", "Failed to start foreground service on cold start", e)
            }
        }
    }
}
