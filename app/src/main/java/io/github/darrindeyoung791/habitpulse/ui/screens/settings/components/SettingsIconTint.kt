package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal data class AccentTint(
    val container: Color,
    val content: Color
)

internal data class AccentTintPair(
    val light: AccentTint,
    val dark: AccentTint
)

/**
 * Low-saturation accent palette used for leading icon chips.
 * Fixed colors (NOT dynamic color), each with light/dark variants that adapt to theme.
 */
internal val AccentPalette = listOf(
    AccentTintPair(
        light = AccentTint(Color(0xFFDCE8FB), Color(0xFF2A5AA8)),
        dark = AccentTint(Color(0xFF1F2D4A), Color(0xFF9FC0F0))
    ),
    AccentTintPair(
        light = AccentTint(Color(0xFFDCEFDC), Color(0xFF2E7D46)),
        dark = AccentTint(Color(0xFF1E3327), Color(0xFF97D0A4))
    ),
    AccentTintPair(
        light = AccentTint(Color(0xFFFBEBD6), Color(0xFF9A6B2A)),
        dark = AccentTint(Color(0xFF36281A), Color(0xFFE3BC7E))
    ),
    AccentTintPair(
        light = AccentTint(Color(0xFFEAE1F7), Color(0xFF5E3F9E)),
        dark = AccentTint(Color(0xFF2B2340), Color(0xFFBCA6E8))
    ),
    AccentTintPair(
        light = AccentTint(Color(0xFFF9E0E0), Color(0xFFA63C47)),
        dark = AccentTint(Color(0xFF3A2224), Color(0xFFE8A0A6))
    ),
    AccentTintPair(
        light = AccentTint(Color(0xFFD9EEEF), Color(0xFF2E7C80)),
        dark = AccentTint(Color(0xFF1E3234), Color(0xFF9CD3D6))
    )
)

@Composable
internal fun rememberAccentTint(index: Int): AccentTint {
    val dark = isSystemInDarkTheme()
    val pair = AccentPalette[(index % AccentPalette.size + AccentPalette.size) % AccentPalette.size]
    return if (dark) pair.dark else pair.light
}