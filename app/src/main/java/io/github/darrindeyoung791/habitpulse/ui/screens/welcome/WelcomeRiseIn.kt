package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * MD3 emphasized-decelerate 逐层进入动画（cubic-bezier(0.05, 0.7, 0.1, 1.0)）。
 *
 * 每层内容从下方约 22dp 淡入上浮至最终位置，时长 800ms，
 * 步进 delay 由调用方传入（40 / 160 / 280 / 400 / 520 / 640 / 760ms）。
 *
 * @param animate 是否播放进入动画。设为 false 时直接渲染内容（用于「首屏仅首次播放」）。
 */
private val WelcomeEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
internal fun WelcomeRiseIn(
    delayMs: Int,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!animate) {
        content()
        return
    }
    val density = LocalDensity.current
    val offsetPx = remember(density) { with(density) { 22.dp.toPx() }.roundToInt() }
    val enter = remember(delayMs, offsetPx) {
        fadeIn(animationSpec = tween(800, delayMillis = delayMs, easing = WelcomeEmphasizedDecelerate)) +
            slideInVertically(
                animationSpec = tween(800, delayMillis = delayMs, easing = WelcomeEmphasizedDecelerate)
            ) { offsetPx }
    }
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = enter,
        label = "welcomeRiseIn"
    ) {
        content()
    }
}