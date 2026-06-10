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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.WelcomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeActivity : ComponentActivity() {

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
                var isFinishing by remember { mutableStateOf(false) }

                val limitedModeFlow by application.habitViewModel.isLimitedMode.collectAsStateWithLifecycle(initialValue = false)

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        startForegroundServiceIfEnabled()
                    }
                }

                splashScreen.setKeepOnScreenCondition { false }

                WelcomeScreen(
                    currentStep = currentStep,
                    onAgree = {
                        if (!NotificationHelper.hasNotificationPermission(this@WelcomeActivity)) {
                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        currentStep = 2
                    },
                    onDisagree = {
                        application.habitViewModel.enterLimitedMode()
                        isFinishing = true
                    },
                    isLimitedMode = limitedModeFlow,
                    onNotificationNext = { reminderEnabled, dndEnabled, dndStart, dndEnd, persistentEnabled ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val prefs = UserPreferences.getInstance(application)
                            prefs.setReminderEnabled(reminderEnabled)
                            prefs.setDndEnabled(dndEnabled)
                            prefs.setDndStartTime(dndStart)
                            prefs.setDndEndTime(dndEnd)
                            prefs.setPersistentNotification(persistentEnabled)
                        }
                        currentStep = 3
                    },
                    onPrevious = {
                        currentStep = 2
                    },
                    onSkip = {
                        application.habitViewModel.completeOnboarding()
                        isFinishing = true
                    },
                    onAIComplete = { endpoint, apiKey, model, streamingEnabled ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val prefs = UserPreferences.getInstance(application)
                            prefs.setLlmApiEndpoint(endpoint)
                            prefs.setLlmApiKey(apiKey)
                            prefs.setLlmModelName(model)
                            prefs.setLlmStreamingResponse(streamingEnabled)
                        }
                        application.habitViewModel.completeOnboarding()
                        isFinishing = true
                    }
                )

                LaunchedEffect(isFinishing) {
                    if (isFinishing) {
                        startMainActivityAndFinish()
                    }
                }
            }
        }
    }

    private fun startMainActivityAndFinish() {
        startActivity(
            android.content.Intent(this@WelcomeActivity, MainActivity::class.java)
        )
        finish()
    }

    private fun startForegroundServiceIfEnabled() {
    }
}
