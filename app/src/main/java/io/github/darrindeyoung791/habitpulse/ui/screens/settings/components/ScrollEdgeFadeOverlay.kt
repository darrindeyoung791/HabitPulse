package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Renders top/bottom fade-out gradients over a vertically scrollable column
 * driven by a [ScrollState]. The gradients fade out completely when the
 * content is at the corresponding edge, matching the pattern used by the
 * main habit screen and the open source licenses screen.
 */
@Composable
fun ScrollEdgeFadeOverlay(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val isAtTop by remember { derivedStateOf { scrollState.value == 0 } }
    val isAtBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue } }

    val topGradientAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "settingsTopGradientAlpha"
    )
    val bottomGradientAlpha by animateFloatAsState(
        targetValue = if (isAtBottom) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "settingsBottomGradientAlpha"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.TopCenter)
                .alpha(topGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .alpha(bottomGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )
    }
}
