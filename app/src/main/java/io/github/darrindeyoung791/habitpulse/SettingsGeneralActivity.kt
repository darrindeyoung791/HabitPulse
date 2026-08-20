package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.SettingsGeneralScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class SettingsGeneralActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsGeneralContent()
            }
        }
    }
}

@Composable
private fun SettingsGeneralContent() {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    SettingsGeneralScreen(
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenHelp = onHelp,
        onOpenLanguage = {
            context.startActivity(Intent(context, SettingsLanguageActivity::class.java))
        },
        onOpenFontScale = {
            context.startActivity(Intent(context, SettingsFontScaleActivity::class.java))
        }
    )
}
