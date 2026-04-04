package io.github.darrindeyoung791.habitpulse

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.ui.screens.WelcomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

/**
 * 欢迎界面 Activity
 *
 * 用于首次启动时显示欢迎界面，用户完成引导后启动 MainActivity
 */
class WelcomeActivity : ComponentActivity() {
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
                        // 启动 MainActivity 并关闭自身
                        startActivity(
                            android.content.Intent(this@WelcomeActivity, MainActivity::class.java)
                        )
                        finish()
                    }
                }
            }
        }
    }
}
