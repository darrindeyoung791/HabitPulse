package io.github.darrindeyoung791.habitpulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 启动完成广播接收器
 *
 * 当设备启动完成后，如果用户之前开启了持久通知设置，
 * 则自动启动前台通知服务以保持应用在后台运行。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            // Use a coroutine scope to read preferences and start service
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    val userPreferences = UserPreferences.getInstance(context)
                    val isPersistentNotificationEnabled = userPreferences.persistentNotificationFlow.first()

                    // Only start service if user had enabled it AND has permission
                    if (isPersistentNotificationEnabled && NotificationHelper.hasNotificationPermission(context)) {
                        NotificationHelper.createNotificationChannel(context)
                        ForegroundNotificationService.toggleService(context, enable = true)
                    }
                } catch (e: Exception) {
                    // Log error but don't crash the receiver
                    android.util.Log.e("BootReceiver", "Failed to start foreground service on boot", e)
                }
            }
        }
    }
}
