package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.SettingsAIEditScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class SettingsAIEditActivity : FragmentActivity() {

    companion object {
        /**
         * 传入要编辑的 AI 配置 id；不传表示新建。
         */
        const val EXTRA_CONFIG_ID = "extra_config_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsAIEditContent(
                    configId = intent.getStringExtra(EXTRA_CONFIG_ID)
                )
            }
        }
    }
}

@Composable
private fun SettingsAIEditContent(configId: String?) {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    SettingsAIEditScreen(
        configId = configId,
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenHelp = onHelp
    )
}
