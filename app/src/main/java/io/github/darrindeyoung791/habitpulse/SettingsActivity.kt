package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.SettingsHomeScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.SettingsTwoPaneScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsRoot()
            }
        }
    }
}

/**
 * 设置界面根路由：
 * - 平板横屏（真平板横屏，或非平板开启「强制平板横屏显示」后横屏）时使用
 *   Android 15 风格双栏布局（左栏总界面 + 右侧悬浮子页面面板）；
 * - 其余形态保持原有单栏列表 + 独立子 Activity 的行为不变。
 */
@Composable
private fun SettingsRoot() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences.getInstance(context) }
    val forceTabletLandscape by userPreferences.forceTabletLandscapeFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val deviceForm = rememberDeviceFormInfo()

    // 与主页一致的「有效平板横屏」判定
    val effectiveTabletLandscape =
        if (forceTabletLandscape && deviceForm.isLandscape && !deviceForm.isTabletDevice) {
            true
        } else {
            deviceForm.isTabletLandscape
        }

    if (effectiveTabletLandscape) {
        val onHelp: () -> Unit = {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
            }
            context.startActivity(intent)
        }
        SettingsTwoPaneScreen(
            onFinish = { (context as? android.app.Activity)?.finish() },
            onOpenHelp = onHelp,
            // 全屏特例：语言 / 字体大小 / AI 编辑配置需要独立 Activity（可能重建、生物识别宿主）
            onOpenLanguage = {
                context.startActivity(Intent(context, SettingsLanguageActivity::class.java))
            },
            onOpenFontScale = {
                context.startActivity(Intent(context, SettingsFontScaleActivity::class.java))
            },
            onOpenAiEdit = { configId ->
                context.startActivity(
                    Intent(context, SettingsAIEditActivity::class.java).apply {
                        configId?.let { putExtra(SettingsAIEditActivity.EXTRA_CONFIG_ID, it) }
                    }
                )
            }
        )
    } else {
        SettingsHomeContent()
    }
}

@Composable
private fun SettingsHomeContent() {
    val context = LocalContext.current
    val onHelp: () -> Unit = {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.HELP_URL)
        }
        context.startActivity(intent)
    }

    SettingsHomeScreen(
        onBack = { (context as? android.app.Activity)?.finish() },
        onNavigateAI = {
            context.startActivity(Intent(context, SettingsAIActivity::class.java))
        },
        onNavigateNotifications = {
            context.startActivity(Intent(context, SettingsNotificationsActivity::class.java))
        },
        onNavigateGeneral = {
            context.startActivity(Intent(context, SettingsGeneralActivity::class.java))
        },
        onNavigateAbout = {
            context.startActivity(Intent(context, SettingsAboutActivity::class.java))
        },
        onOpenHelp = onHelp
    )
}
