package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.ui.DeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback

/**
 * 各步内容的最大宽度（对齐原型：竖屏 360 / 手机横屏 460 / 平板 520）。
 */
internal fun welcomeContentMaxWidth(deviceForm: DeviceFormInfo): Dp = when {
    deviceForm.isTabletLandscape -> 520.dp
    deviceForm.isPhoneLandscape -> 460.dp
    else -> 360.dp
}

/**
 * 欢迎页各步的公共骨架：
 * - 顶部（步骤指示器）固定
 * - 中部内容：竖屏/平板垂直居中；手机横屏顶部对齐、可滚动
 * - 底部（按钮区）固定，与内容分离
 */
@Composable
internal fun WelcomeStepLayout(
    isPhoneLandscape: Boolean,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        top()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = if (isPhoneLandscape) Alignment.TopCenter else Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .widthIn(max = maxWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                body()
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            bottom()
        }
    }
}

/**
 * 底部固定按钮区：与内容分离，按钮列最大宽 [maxWidth]。
 */
@Composable
internal fun WelcomeBottomBar(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}

/**
 * 全宽大圆角主按钮（确认类 filled 按钮）。
 * 按下时圆角半径变大（16dp → 20dp，与分段列表行一致），并带按压震动。
 */
@Composable
internal fun WelcomePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (pressed) 20.dp else 16.dp,
        label = "welcomePrimaryCorner"
    )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
    PressVibrationFeedback(interactionSource = interactionSource)
}

/**
 * 次按钮（文本次按钮，取消类）。
 * 全宽居中显示，带按压震动。
 */
@Composable
internal fun WelcomeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    PressVibrationFeedback(interactionSource = interactionSource)
}