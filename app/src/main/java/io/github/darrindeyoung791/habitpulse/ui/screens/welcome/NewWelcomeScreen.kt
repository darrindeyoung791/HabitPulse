package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo

/**
 * 新版欢迎页容器：4 步（欢迎 → 权限声明 → 通知设置 → 完成）。
 *
 * 通知偏好状态在此提升，步骤切换用 [AnimatedContent] 淡入淡出；
 * 首屏进入动画仅首次播放（[step1Played]）。
 */
@Composable
fun NewWelcomeScreen(
    currentStep: Int,
    onContinue: () -> Unit,
    onAgree: () -> Unit,
    onNotificationsNext: (
        reminderEnabled: Boolean,
        dndEnabled: Boolean,
        dndStart: String,
        dndEnd: String,
        persistentEnabled: Boolean
    ) -> Unit,
    onEnter: () -> Unit,
    onBack: () -> Unit,
    onExitApp: () -> Unit,
    onLinkClick: () -> Unit,
    deviceForm: DeviceFormInfo,
    modifier: Modifier = Modifier
) {
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
    var dndEnabled by rememberSaveable { mutableStateOf(true) }
    var dndStart by rememberSaveable { mutableStateOf("22:00") }
    var dndEnd by rememberSaveable { mutableStateOf("07:00") }
    var persistentEnabled by rememberSaveable { mutableStateOf(true) }

    var step1Played by remember { mutableStateOf(false) }
    LaunchedEffect(currentStep) {
        if (currentStep != 1) {
            step1Played = true
        }
    }

    var showDisagreeDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentStep > 1, onBack = onBack)

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(tween(250)) togetherWith fadeOut(tween(250)))
            },
            label = "welcomeStep",
            modifier = Modifier.padding(innerPadding)
        ) { step ->
            when (step) {
                1 -> WelcomeGreetingStep(
                    animate = !step1Played,
                    onContinue = onContinue,
                    deviceForm = deviceForm
                )
                2 -> WelcomePermissionsStep(
                    onAgree = onAgree,
                    onDisagree = { showDisagreeDialog = true },
                    onLinkClick = onLinkClick,
                    onBack = onBack,
                    deviceForm = deviceForm
                )
                3 -> WelcomeNotificationsStep(
                    reminderEnabled = reminderEnabled,
                    dndEnabled = dndEnabled,
                    dndStart = dndStart,
                    dndEnd = dndEnd,
                    persistentEnabled = persistentEnabled,
                    onReminderChanged = {
                        reminderEnabled = it
                        if (!it) dndEnabled = false
                    },
                    onDndChanged = { dndEnabled = it },
                    onDndStartChanged = { dndStart = it },
                    onDndEndChanged = { dndEnd = it },
                    onPersistentChanged = { persistentEnabled = it },
                    onFinish = {
                        onNotificationsNext(
                            reminderEnabled, dndEnabled, dndStart, dndEnd, persistentEnabled
                        )
                    },
                    onSkip = {
                        onNotificationsNext(
                            reminderEnabled, dndEnabled, dndStart, dndEnd, persistentEnabled
                        )
                    },
                    onBack = onBack,
                    deviceForm = deviceForm
                )
                else -> WelcomeDoneStep(
                    onEnter = onEnter,
                    deviceForm = deviceForm
                )
            }
        }
    }

    if (showDisagreeDialog) {
        AlertDialog(
            onDismissRequest = { showDisagreeDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.welcome_disagree_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(stringResource(R.string.welcome_disagree_dialog_message, stringResource(R.string.app_name)))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisagreeDialog = false
                        onExitApp()
                    }
                ) {
                    Text(stringResource(R.string.welcome_disagree_dialog_disagree_exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisagreeDialog = false }
                ) {
                    Text(stringResource(R.string.welcome_disagree_dialog_agree))
                }
            }
        )
    }
}