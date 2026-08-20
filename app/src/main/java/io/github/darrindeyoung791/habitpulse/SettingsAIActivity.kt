package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.SettingsAIScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class SettingsAIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsAIContent()
            }
        }
    }
}

@Composable
private fun SettingsAIContent() {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    SettingsAIScreen(
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenHelp = onHelp,
        onAddConfig = {
            context.startActivity(Intent(context, SettingsAIEditActivity::class.java))
        },
        onEditConfig = { configId ->
            context.startActivity(
                Intent(context, SettingsAIEditActivity::class.java).apply {
                    putExtra(SettingsAIEditActivity.EXTRA_CONFIG_ID, configId)
                }
            )
        }
    )
}
