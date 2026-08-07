package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import kotlinx.coroutines.delay

/**
 * 深度思考块（普通容器：无底色、无阴影、灰字、非斜体）。
 *
 * - 思考中：标题 + 「已深度思考（X秒）…」实时计时；
 * - 完成：标题 + 「已深度思考（X秒）」汇总；
 * - 整行可点击展开/收起，收起时不预览内容。
 */
@Composable
fun ThinkingBlock(
    modifier: Modifier = Modifier,
    thoughts: String,
    isLoading: Boolean = false,
    elapsedSeconds: Long = 0L
) {
    var isExpanded by remember { mutableStateOf(false) }
    var tickSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            tickSeconds = 0
            while (true) {
                delay(1000)
                tickSeconds++
            }
        }
    }

    val shownSeconds = if (isLoading) tickSeconds.toLong() else elapsedSeconds
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "thinking_rotation"
    )
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    val gray = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { isExpanded = !isExpanded }
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.ai_chat_thinking_title),
                style = MaterialTheme.typography.labelLarge,
                color = gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_chat_thinking_elapsed, shownSeconds) +
                        if (isLoading) "…" else "",
                style = MaterialTheme.typography.labelSmall,
                color = gray.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse)
                                    else stringResource(R.string.expand),
                tint = gray,
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                HorizontalDivider(
                    color = gray.copy(alpha = 0.15f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = thoughts + if (isLoading) "●" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = gray,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}
