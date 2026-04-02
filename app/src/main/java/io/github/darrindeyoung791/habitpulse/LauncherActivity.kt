package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * 应用启动器 Activity
 * 
 * 根据 onboarding 状态决定启动 WelcomeActivity 还是 MainActivity
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = applicationContext as HabitPulseApplication
        val hasCompletedOnboarding = application.habitViewModel.hasCompletedOnboarding.value

        // 根据 onboarding 状态决定启动哪个 Activity
        val targetActivity = if (hasCompletedOnboarding) {
            MainActivity::class.java
        } else {
            WelcomeActivity::class.java
        }

        startActivity(
            android.content.Intent(this, targetActivity)
        )
        finish()
    }
}
