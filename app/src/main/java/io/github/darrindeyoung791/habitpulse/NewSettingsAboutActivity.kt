package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.NewSettingsAboutDetailScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class NewSettingsAboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                NewSettingsAboutContent()
            }
        }
    }
}

@Composable
private fun NewSettingsAboutContent() {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    NewSettingsAboutDetailScreen(
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenHelp = onHelp,
        onNavigateDebug = {
            context.startActivity(Intent(context, NewSettingsDebugActivity::class.java))
        }
    )
}
