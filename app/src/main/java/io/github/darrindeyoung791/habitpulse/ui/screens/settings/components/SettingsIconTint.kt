package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import io.github.darrindeyoung791.habitpulse.ui.theme.AccentSeeds
import io.github.darrindeyoung791.habitpulse.ui.theme.AccentTint
import io.github.darrindeyoung791.habitpulse.ui.theme.seedAccentTint

/**
 * 页面级 tint 偏移量：同一页面内连续的分组继续使用色板，而不是从 index 0
 * 重新开始，从而保证同一页面内的图标颜色不重复。
 * 由 [SettingsSegmentedGroup] 提供（`tintOffset` 参数）。
 */
internal val LocalAccentTintOffset = staticCompositionLocalOf { 0 }

/**
 * 前置图标 chip 的颜色：以 [AccentSeeds] 为种子、通过 Material 取色器
 * （tonal spot 色板）派生 container / content 颜色，见 [seedAccentTint]。
 *
 * 使用 [MaterialTheme.colorScheme] 的背景色亮度判断当前是否深色主题，
 * 而非 [androidx.compose.foundation.isSystemInDarkTheme]（后者只读系统配置，
 * 忽略应用内手动切换）。
 */
@Composable
internal fun rememberAccentTint(index: Int): AccentTint {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val globalIndex = (LocalAccentTintOffset.current + index).mod(AccentSeeds.size)
    val seed = AccentSeeds[globalIndex]
    return remember(seed, dark) { seedAccentTint(seed, dark) }
}
