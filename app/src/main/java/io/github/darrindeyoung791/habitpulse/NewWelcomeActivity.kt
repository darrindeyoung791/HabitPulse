package io.github.darrindeyoung791.habitpulse

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.welcome.NewWelcomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 新版欢迎引导页：欢迎 → 权限声明 → 通知设置 → 完成。
 * 不同意仅可退出应用（不再提供受限模式）；「完成/跳过」写入偏好后进入完成页。
 */
class NewWelcomeActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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

        enableEdgeToEdge()

        setContent {
            HabitPulseTheme {
                val application = applicationContext as HabitPulseApplication
                var currentStep by rememberSaveable { mutableIntStateOf(1) }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                splashScreen.setKeepOnScreenCondition { false }

                NewWelcomeScreen(
                    currentStep = currentStep,
                    onContinue = { currentStep = 2 },
                    onAgree = {
                        if (!NotificationHelper.hasNotificationPermission(this@NewWelcomeActivity)) {
                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        currentStep = 3
                    },
                    onNotificationsNext = { reminderEnabled, dndEnabled, dndStart, dndEnd, persistentEnabled ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val prefs = UserPreferences.getInstance(application)
                            prefs.setReminderEnabled(reminderEnabled)
                            prefs.setDndEnabled(dndEnabled)
                            prefs.setDndStartTime(dndStart)
                            prefs.setDndEndTime(dndEnd)
                            prefs.setPersistentNotification(persistentEnabled)
                        }
                        currentStep = 4
                    },
                    onEnter = {
                        application.habitViewModel.completeOnboarding()
                        startMainActivityAndFinish()
                    },
                    onBack = {
                        if (currentStep > 1) {
                            currentStep -= 1
                        }
                    },
onExitApp = { finish() },
                    onLinkClick = { toastComingSoon() },
                    deviceForm = rememberDeviceFormInfo()
                )
            }
        }
    }

    private fun toastComingSoon() {
        android.widget.Toast.makeText(
            this,
            getString(R.string.welcome_privacy_policy_coming_soon),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun startMainActivityAndFinish() {
        startActivity(
            android.content.Intent(this@NewWelcomeActivity, MainActivity::class.java)
        )
        finish()
    }
}