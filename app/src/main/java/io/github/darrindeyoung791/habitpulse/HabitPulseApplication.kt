package io.github.darrindeyoung791.habitpulse

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.color.DynamicColors
import io.github.darrindeyoung791.habitpulse.data.database.HabitDatabase
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import io.github.darrindeyoung791.habitpulse.utils.ReminderNotificationBuilder
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
        .addMigrations(HabitDatabase.MIGRATION_3_4)  // Migration from v3 to v4
        .addMigrations(HabitDatabase.MIGRATION_4_5)  // Migration from v4 to v5
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

    // 用户偏好设置（延迟初始化）
    val userPreferences: UserPreferences by lazy {
        UserPreferences.getInstance(this)
    }

    // HabitViewModel 单例（延迟初始化）
    val habitViewModel: HabitViewModel by lazy {
        HabitViewModel(repository, onboardingPreferences, userPreferences)
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

        // One-time migration of the legacy single LLM config to the multi-config storage
        initializeReminderOnColdStart()
    }

    /**
     * 在冷启动时恢复提醒闹钟调度
     *
     * 无论应用是被用户主动打开还是被 BOOT_COMPLETED 拉起，
     * 都确保提醒通道已创建、闹钟已恢复。
     * 前台通知服务（保活）由用户打开 Activity 时初始化。
     */
    private fun initializeReminderOnColdStart() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val userPreferences = UserPreferences.getInstance(applicationContext)
                try {
                    userPreferences.migrateLegacyAiConfig()
                } catch (e: Exception) {
                    android.util.Log.e("HabitPulseApplication", "Failed to migrate legacy AI config", e)
                }
                val isReminderEnabled = userPreferences.reminderEnabledFlow.first()
                val hasPermission = NotificationHelper.hasNotificationPermission(applicationContext)

                if (isReminderEnabled && hasPermission) {
                    ReminderNotificationBuilder.createNotificationChannel(applicationContext)
                    ReminderManager.scheduleNextAlarm(applicationContext)
                } else {
                    ReminderNotificationBuilder.createNotificationChannel(applicationContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitPulseApplication", "Failed to initialize reminder on cold start", e)
            }
        }
    }
}
