package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.darrindeyoung791.habitpulse.ui.screens.ai.AIChatScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

/**
 * 新版 AI 对话独立 Activity：宿主 [AIChatScreen]，从新建习惯对话框进入。
 * 习惯编辑通过带 extra 启动 MainActivity，由 NavHost 处理跳转。
 */
class AIChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_EDIT_HABIT_ID = "extra_edit_habit_id"
        const val EXTRA_MANUAL_CREATE = "extra_manual_create"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                AIChatScreen(
                    onNavigateBack = { finish() },
                    onNavigateToSettings = {
                        startActivity(Intent(this, SettingsAIActivity::class.java))
                    },
                    onEditHabit = { habitId ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra(EXTRA_EDIT_HABIT_ID, habitId.toString())
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                    },
                    onManualCreateHabit = {
                        // 回到主窗口并打开手动创建习惯页
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra(EXTRA_MANUAL_CREATE, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                        finish()
                    },
                    application = applicationContext as HabitPulseApplication
                )
            }
        }
    }
}
