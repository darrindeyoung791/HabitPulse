package io.github.darrindeyoung791.habitpulse.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences

/**
 * 按压触感震动时长（毫秒）。
 * 为主页打卡按钮震动时长（50ms）的一半，用于模拟卡片按下的触感。
 */
const val PressVibrationDurationMs = 25L

/**
 * 应用内全部震动开关是否开启
 */
@Composable
fun rememberHapticsEnabled(): Boolean {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences.getInstance(context) }
    val hapticsEnabled by userPreferences.hapticsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    return hapticsEnabled
}

/**
 * 触发一次短暂的按压触感震动。
 * 非 Composable 版本，供需要手动触发震动的场景使用（如打卡按钮、步进滑动条）。
 */
fun vibrateShort(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(PressVibrationDurationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(PressVibrationDurationMs)
    }
}

/**
 * 按压触感震动反馈。
 *
 * **直接在内联协程中逐条收集 [MutableInteractionSource] 的交互事件**，而不是通过
 * `collectIsPressedAsState` + `LaunchedEffect(pressed)` 观察组合期状态。原因是 Compose
 * 会把同一帧内的按下/抬起状态合并，极快的按下-抬起可能永远读不到中间态，导致不震动；
 * 逐条收集能保证快速操作时按下与松手各触发一次震动。
 *
 * - 按下时若 [enabled] 且未关闭应用内震动，触发一次震动（25ms）并「武装」。
 * - 松手时若已武装则再震动一次（模拟卡片按下触感）。
 * - 在按下期间组件主动变为禁用（如开始网络请求的按钮），松手仍按「按下时」的状态震动，
 *   不会因禁用而丢失松手反馈。
 * - 按下时本身已禁用（如 `enabled = false` 的占位项）则全程静默。
 */
@Composable
fun PressVibrationFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val hapticsEnabled = rememberHapticsEnabled()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)

    LaunchedEffect(interactionSource, context) {
        var armed = false
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    armed = currentEnabled && currentHaptics
                    if (armed) vibrateShort(context)
                }
                is PressInteraction.Release -> {
                    if (armed) vibrateShort(context)
                    armed = false
                }
                is PressInteraction.Cancel -> armed = false
                else -> Unit
            }
        }
    }
}