package io.github.darrindeyoung791.habitpulse

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.WelcomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper

/**
 * 欢迎界面 Activity
 *
 * 用于首次启动时显示欢迎界面，用户完成引导后启动 MainActivity
 */
class WelcomeActivity : ComponentActivity() {

    private var shouldStartMainActivityAfterPermission = false

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
        if (isGranted) {
            startForegroundServiceIfEnabled()
        }
        // Now start MainActivity and finish
        startMainActivityAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Set custom fade-out animation for splash screen exit
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOutAnimator = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                "alpha",
                1f,
                0f
            )
            fadeOutAnimator.duration = 200
            fadeOutAnimator.interpolator = AccelerateDecelerateInterpolator()
            fadeOutAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    splashScreenViewProvider.remove()
                }
            })
            fadeOutAnimator.start()
        }

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            HabitPulseTheme {
                val application = applicationContext as HabitPulseApplication
                var isFinishing by remember { mutableStateOf(false) }

                // 收集受限模式状态
                val isLimitedMode by application.habitViewModel.isLimitedMode.collectAsStateWithLifecycle(initialValue = false)

                // Splash screen 立即释放
                splashScreen.setKeepOnScreenCondition { false }

                WelcomeScreen(
                    onAgree = {
                        application.habitViewModel.completeOnboarding()
                        isFinishing = true
                    },
                    onDisagree = {
                        application.habitViewModel.enterLimitedMode()
                        isFinishing = true
                    },
                    isLimitedMode = isLimitedMode
                )

                // 用户完成操作后，启动 MainActivity 并关闭自身
                LaunchedEffect(isFinishing) {
                    if (isFinishing) {
                        // 如果用户同意（非受限模式），则请求通知权限
                        if (!isLimitedMode) {
                            requestNotificationPermission()
                        } else {
                            // 受限模式：直接启动 MainActivity
                            startMainActivityAndFinish()
                        }
                    }
                }
            }
        }
    }

    /**
     * 请求通知权限
     */
    private fun requestNotificationPermission() {
        if (NotificationHelper.hasNotificationPermission(this)) {
            // Already has permission, start service if enabled
            startForegroundServiceIfEnabled()
            // Now start MainActivity and finish
            startMainActivityAndFinish()
        } else {
            // Request permission - will finish in callback
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * 启动 MainActivity 并关闭自身
     */
    private fun startMainActivityAndFinish() {
        startActivity(
            android.content.Intent(this@WelcomeActivity, MainActivity::class.java)
        )
        finish()
    }

    /**
     * 如果用户已启用持久通知且具有权限，则启动前台服务
     */
    private fun startForegroundServiceIfEnabled() {
        val application = applicationContext as HabitPulseApplication
        // Note: We can't collect Flow here since we're not in Compose context
        // The service will be started by MainActivity when it collects the preference
        // For now, we just ensure the permission is requested
    }
}
