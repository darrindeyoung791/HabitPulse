package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import io.github.darrindeyoung791.habitpulse.ui.screens.ai.AISettingsScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class AISettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                AISettingsScreen(
                    onBackAction = { finish() }
                )
            }
        }
    }
}