package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.NewSettingsHomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class NewSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                NewSettingsHomeContent()
            }
        }
    }
}

@Composable
private fun NewSettingsHomeContent() {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    NewSettingsHomeScreen(
        onBack = { (context as? android.app.Activity)?.finish() },
        onNavigateAI = {
            context.startActivity(Intent(context, NewSettingsAIActivity::class.java))
        },
        onNavigateNotifications = {
            context.startActivity(Intent(context, NewSettingsNotificationsActivity::class.java))
        },
        onNavigateGeneral = {
            context.startActivity(Intent(context, NewSettingsGeneralActivity::class.java))
        },
        onNavigateAbout = {
            context.startActivity(Intent(context, NewSettingsAboutActivity::class.java))
        },
        onOpenHelp = onHelp
    )
}
