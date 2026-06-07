package io.github.darrindeyoung791.habitpulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val userPreferences = UserPreferences.getInstance(context)
                val isPersistentNotificationEnabled = userPreferences.persistentNotificationFlow.first()
                val isReminderEnabled = userPreferences.reminderEnabledFlow.first()
                val hasPermission = NotificationHelper.hasNotificationPermission(context)

                if (isReminderEnabled && hasPermission) {
                    ReminderManager.scheduleNextAlarm(context)
                }

                if (isPersistentNotificationEnabled && hasPermission) {
                    NotificationHelper.createNotificationChannel(context)
                    Handler(Looper.getMainLooper()).postDelayed({
                        ForegroundNotificationService.toggleService(context, enable = true)
                    }, 5000)
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to process boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
