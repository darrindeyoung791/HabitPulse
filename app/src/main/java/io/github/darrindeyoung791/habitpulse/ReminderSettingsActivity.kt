package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.darrindeyoung791.habitpulse.ui.screens.ReminderSettingsScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class ReminderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                ReminderSettingsScreen(
                    onBackAction = { finish() }
                )
            }
        }
    }
}
