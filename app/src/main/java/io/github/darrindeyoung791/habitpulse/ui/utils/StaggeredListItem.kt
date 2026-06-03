package io.github.darrindeyoung791.habitpulse.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

@Composable
fun rememberAnimationsFrozen(scrollableState: ScrollableState): State<Boolean> {
    val frozen = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        frozen.value = true
    }

    LaunchedEffect(scrollableState) {
        snapshotFlow { scrollableState.isScrollInProgress }
            .filter { it }
            .collect {
                frozen.value = true
            }
    }

    return frozen
}

@Composable
fun StaggeredListItem(
    index: Int,
    animationsFrozen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!animationsFrozen) {
            delay(index * 30L)
        }
        animationTriggered = true
    }

    LaunchedEffect(animationsFrozen) {
        if (animationsFrozen) {
            animationTriggered = true
        }
    }

    val transition = updateTransition(targetState = animationTriggered, label = "staggeredListItem")

    val alpha by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) },
        label = "alpha"
    ) { triggered -> if (triggered) 1f else 0f }

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) },
        label = "scale"
    ) { triggered -> if (triggered) 1f else 0.93f }

    val translationY by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) },
        label = "translationY"
    ) { triggered -> if (triggered) 0f else 25f }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                this.scaleX = scale
                this.scaleY = scale
            }
    ) {
        content()
    }
}
