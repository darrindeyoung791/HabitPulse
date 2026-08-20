package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback

/**
 * 顶部栏：仅在需要返回时显示左上角返回按钮（无进度点、无步骤数字）。
 */
@Composable
fun WelcomeTopBar(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 14.dp, bottom = 6.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (onBack != null) {
                WelcomeTopBackButton(onBack = onBack)
            }
        }
    }
}

@Composable
private fun WelcomeTopBackButton(onBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onBack,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.onboarding_step_previous)
        )
    }
    PressVibrationFeedback(interactionSource = interactionSource)
}