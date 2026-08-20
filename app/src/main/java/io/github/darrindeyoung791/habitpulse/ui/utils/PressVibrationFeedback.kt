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
 * 按压触感震动默认时长（毫秒）。
 * 为主页打卡按钮震动时长（50ms）的一半，用于模拟卡片按下的触感。
 */
const val PressVibrationDurationMs = 25L

/**
 * 按压触感震动默认强度（1-255）。
 * 128 为中等强度。
 */
const val PressVibrationDefaultAmplitude = 128

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
 * 读取用户可配置的按压震动参数（时长 + 强度）。
 */
@Composable
fun rememberPressVibrationParams(): Pair<Long, Int> {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences.getInstance(context) }
    val durationMs by userPreferences.pressVibrationDurationMsFlow.collectAsStateWithLifecycle(initialValue = PressVibrationDurationMs)
    val amplitude by userPreferences.pressVibrationAmplitudeFlow.collectAsStateWithLifecycle(initialValue = PressVibrationDefaultAmplitude)
    return durationMs to amplitude
}

/**
 * 触发一次按压触感震动。
 *
 * 强度仅在设备硬件支持幅度控制（[android.os.Vibrator.hasAmplitudeControl]）时生效；
 * 不支持时回退为 [VibrationEffect.DEFAULT_AMPLITUDE]（忽略 [amplitude]）。
 *
 * @param durationMs 震动时长（毫秒）
 * @param amplitude 强度（1-255），仅硬件支持幅度控制时生效
 */
fun vibrateShort(
    context: Context,
    durationMs: Long = PressVibrationDurationMs,
    amplitude: Int = PressVibrationDefaultAmplitude
) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effectiveAmplitude = try {
            if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        } catch (_: Throwable) {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, effectiveAmplitude)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
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
 * - 按下时若 [enabled] 且未关闭应用内震动，触发一次震动并「武装」。
 * - 松手时若已武装则再震动一次（模拟卡片按下触感）。
 * - 在按下期间组件主动变为禁用（如开始网络请求的按钮），松手仍按「按下时」的状态震动，
 *   不会因禁用而丢失松手反馈。
 * - 按下时本身已禁用（如 `enabled = false` 的占位项）则全程静默。
 * - 时长与强度读取用户在调试页的配置（[rememberPressVibrationParams]）。
 */
@Composable
fun PressVibrationFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val hapticsEnabled = rememberHapticsEnabled()
    val (durationMs, amplitude) = rememberPressVibrationParams()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)
    val currentDuration by rememberUpdatedState(durationMs)
    val currentAmplitude by rememberUpdatedState(amplitude)

    LaunchedEffect(interactionSource, context) {
        var armed = false
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    armed = currentEnabled && currentHaptics   // 按下瞬间快照
                    if (armed) vibrateShort(context, currentDuration, currentAmplitude)
                }
                is PressInteraction.Release -> {
                    if (armed) vibrateShort(context, currentDuration, currentAmplitude)
                    armed = false
                }
                is PressInteraction.Cancel -> armed = false
                else -> Unit
            }
        }
    }
}