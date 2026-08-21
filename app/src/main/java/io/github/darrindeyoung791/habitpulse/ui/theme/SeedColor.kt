package io.github.darrindeyoung791.habitpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot

/**
 * 由「颜色种子」派生的一组前景 / 背景颜色。
 *
 * 不硬编码明暗主题下的具体色值：以 [container] 作为承载背景、[content] 作为其上
 * 的内容前景，二者来自 Material 取色器（`material-color-utilities` 的 tonal spot
 * 色板），在明 / 暗主题下都保证约 7:1 的对比度。
 */
internal data class AccentTint(
    val container: Color,
    val content: Color
)

/**
 * 内置颜色种子（蓝 / 绿 / 橙 / 紫 / 红 / 青）。
 *
 * 设置页图标 chip、首页入口卡片等均从这些种子派生颜色，保证全应用配色一致，
 * 且天然适配明暗主题。
 */
internal val AccentSeeds: List<Color> = listOf(
    Color(0xFF2A5AA8), // blue
    Color(0xFF2E7D46), // green
    Color(0xFF9A6B2A), // orange
    Color(0xFF5E3F9E), // purple
    Color(0xFFA63C47), // red
    Color(0xFF2E7C80)  // teal
)

/**
 * 使用 Material 取色器从种子色派生 container / content 颜色。
 */
internal fun seedAccentTint(seed: Color, dark: Boolean): AccentTint {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), dark, 0.0)
    return AccentTint(
        container = Color(scheme.primaryContainer),
        content = Color(scheme.onPrimaryContainer)
    )
}

/**
 * 根据明暗主题派生指定种子色的 container / content 颜色。
 *
 * 使用 [MaterialTheme.colorScheme] 的背景色亮度判断当前是否深色主题，
 * 而非 [androidx.compose.foundation.isSystemInDarkTheme]（后者只读系统配置，
 * 忽略应用内手动切换）。
 */
@Composable
internal fun rememberSeedAccentTint(seed: Color): AccentTint {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(seed, dark) { seedAccentTint(seed, dark) }
}
