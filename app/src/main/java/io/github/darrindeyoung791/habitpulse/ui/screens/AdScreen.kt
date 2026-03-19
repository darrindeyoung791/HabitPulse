package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import kotlinx.coroutines.delay

/**
 * 开屏广告页面
 *
 * 显示 5 秒倒计时，底部居中显示"跳过广告"按钮
 *
 * @param onAdFinished 广告结束回调（倒计时结束或用户点击跳过）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdScreen(
    onAdFinished: () -> Unit
) {
    // 广告时长（秒）
    val adDurationSeconds = 5

    // 当前倒计时
    var countdown by remember { mutableStateOf(adDurationSeconds) }

    // 是否显示跳过按钮
    var showSkipButton by remember { mutableStateOf(false) }

    // 倒计时
    LaunchedEffect(Unit) {
        // 短暂延迟后显示跳过按钮
        delay(300)
        showSkipButton = true

        // 开始倒计时
        for (i in adDurationSeconds downTo 1) {
            delay(1000)
            countdown = i - 1
        }

        // 倒计时结束，进入主页
        onAdFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 广告内容区域（占满整个屏幕）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 广告占位内容 - 可以替换为实际的广告内容
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(id = R.string.ad_space_for_rent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 底部跳过按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (showSkipButton) {
                Button(
                    onClick = onAdFinished,
                    modifier = Modifier
                        .height(64.dp)
                        .widthIn(min = 200.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.ad_skip_button, countdown),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdScreenPreview() {
    MaterialTheme {
        AdScreen(
            onAdFinished = {}
        )
    }
}
