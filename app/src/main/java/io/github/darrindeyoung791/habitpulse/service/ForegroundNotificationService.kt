package io.github.darrindeyoung791.habitpulse.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper

/**
 * 前台通知服务 - 用于保持应用在后台运行
 *
 * 该服务通过显示持久通知来降低被系统杀死的风险
 */
class ForegroundNotificationService : Service() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        val notification = NotificationHelper.createPersistentNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    /**
     * 停止前台服务
     */
    fun stopService() {
        NotificationHelper.cancelPersistentNotification(this)
        stopSelf()
    }

    companion object {
        /**
         * 启动或停止前台服务
         *
         * @param context 应用上下文
         * @param enable true 为启动，false 为停止
         */
        fun toggleService(context: android.content.Context, enable: Boolean) {
            val intent = Intent(context, ForegroundNotificationService::class.java)
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }
}
