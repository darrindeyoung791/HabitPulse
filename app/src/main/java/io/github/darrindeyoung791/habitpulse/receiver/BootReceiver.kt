package io.github.darrindeyoung791.habitpulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 启动完成广播接收器
 *
 * 设备启动后恢复习惯提醒的闹钟调度。
 * 后台保活的前台服务由用户打开 App 时初始化。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val userPreferences = UserPreferences.getInstance(context)
                val isReminderEnabled = userPreferences.reminderEnabledFlow.first()
                val hasPermission = NotificationHelper.hasNotificationPermission(context)

                if (isReminderEnabled && hasPermission) {
                    ReminderManager.scheduleNextAlarm(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to restore reminder alarm", e)
            }
        }
    }
}
