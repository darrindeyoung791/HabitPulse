package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 打卡奖励底部弹窗
 * 显示完成次数、动画图标和操作按钮
 * 
 * @param habit 刚刚完成打卡的习惯
 * @param completionCount 当前完成次数
 * @param onDismiss 关闭弹窗的回调
 * @param onComplete 点击完成按钮的回调
 * @param onNotifySupervisor 点击通知监督人按钮的回调（暂未实现）
 * @param onSkipNotification 点击跳过通知按钮的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardBottomSheet(
    habit: Habit,
    completionCount: Int,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onNotifySupervisor: () -> Unit,
    onSkipNotification: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    
    // 检测设备和方向（与 HomeScreen 相同的判定逻辑）
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    val isTabletDevice = smallestScreenWidthDp >= 600
    val isPhoneLandscape = !isTabletDevice && isLandscape  // 手机横屏模式
    
    // Check if habit has supervisors
    val hasSupervisors = habit.supervisionMethod != SupervisionMethod.NONE &&
        (habit.getSupervisorEmailsList().isNotEmpty() || habit.getSupervisorPhonesList().isNotEmpty())
    
    // 动画状态
    var animationStarted by remember { mutableStateOf(false) }
    
    // Custom dismiss handler that shows toast when swiping down/back with supervisors
    val onSheetDismiss: () -> Unit = {
        if (hasSupervisors) {
            Toast.makeText(
                context,
                context.getString(R.string.reward_dismiss_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
        onDismiss()
    }
    
    // 触发入场动画
    LaunchedEffect(Unit) {
        animationStarted = true
        // 触发微妙的触觉反馈
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } catch (e: Exception) {
            // 忽略不支持的设备
        }
    }
    
    val accessibilityDesc = stringResource(id = R.string.accessibility_reward_sheet)
    
    ModalBottomSheet(
        onDismissRequest = onSheetDismiss,
        sheetState = sheetState,
        modifier = Modifier.semantics {
            contentDescription = accessibilityDesc
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题区域
            Text(
                text = stringResource(
                    id = R.string.reward_title,
                    completionCount,
                    habit.title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.reward_subtitle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            // 与上方相同的间距
            Spacer(modifier = Modifier.height(32.dp))
            
            // 动画图标区域（手机横屏时不显示，为按钮留出空间）
            if (!isPhoneLandscape) {
                AnimatedCheckIcon(
                    animationStarted = animationStarted,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )

                // 与上方相同的间距
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // 操作按钮区域
            ActionButtons(
                habit = habit,
                completionCount = completionCount,
                hasSupervisors = hasSupervisors,
                onComplete = onComplete,
                onNotifySupervisor = onNotifySupervisor,
                onSkipNotification = onSkipNotification
            )
        }
    }
}

/**
 * 带动画效果的勾选图标
 * 背景形状从小到大，顺时针旋转
 * 勾选图标从小到大，不旋转
 */
@Composable
private fun AnimatedCheckIcon(
    animationStarted: Boolean,
    containerColor: androidx.compose.ui.graphics.Color
) {
    // 背景形状动画
    val backgroundScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "backgroundScale"
    )
    
    val backgroundRotation by animateFloatAsState(
        targetValue = if (animationStarted) 0f else -180f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "backgroundRotation"
    )
    
    // 勾选图标动画（稍微延迟）
    val iconScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing,
            delayMillis = 100
        ),
        label = "iconScale"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // 背景形状 - 12 角星形
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .scale(backgroundScale)
                .graphicsLayer {
                    rotationZ = backgroundRotation
                }
        ) {
            val canvasSize = size.minDimension
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = canvasSize / 2
            val innerRadius = outerRadius * 0.8f
            val cornerRadius = 12.dp.toPx()  // 圆角半径，越小角越尖

            // 创建星形顶点
            val numPoints = 12  // 12 个角
            val points = mutableListOf<Offset>()
            for (i in 0 until numPoints * 2) {
                val angle = (PI * i / numPoints) - PI / 2
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                points.add(Offset(x, y))
            }

            // 创建带圆角的路径
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            
            for (i in points.indices) {
                val prev = points[(i - 1 + points.size) % points.size]
                val current = points[i]
                val next = points[(i + 1) % points.size]
                
                // 计算从当前点到前一点的方向
                val toPrevX = prev.x - current.x
                val toPrevY = prev.y - current.y
                val toPrevLen = kotlin.math.sqrt(toPrevX * toPrevX + toPrevY * toPrevY)
                
                // 计算从当前点到下一点的方向
                val toNextX = next.x - current.x
                val toNextY = next.y - current.y
                val toNextLen = kotlin.math.sqrt(toNextX * toNextX + toNextY * toNextY)
                
                // 计算圆角起点（在当前点到前一点的边上，距离顶点 cornerRadius 处）
                val cornerStartX = current.x + (toPrevX / toPrevLen) * cornerRadius
                val cornerStartY = current.y + (toPrevY / toPrevLen) * cornerRadius
                
                // 计算圆角终点（在当前点到下一点的边上，距离顶点 cornerRadius 处）
                val cornerEndX = current.x + (toNextX / toNextLen) * cornerRadius
                val cornerEndY = current.y + (toNextY / toNextLen) * cornerRadius
                
                if (i == 0) {
                    path.lineTo(cornerStartX, cornerStartY)
                } else {
                    path.lineTo(cornerStartX, cornerStartY)
                }
                
                // 使用二次贝塞尔曲线绘制圆角
                path.quadraticTo(
                    current.x, current.y,
                    cornerEndX, cornerEndY
                )
            }
            path.close()

            drawPath(path, color = containerColor)
        }
        
        // 勾选图标
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .scale(iconScale),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * 操作按钮区域
 */
@Composable
private fun ActionButtons(
    habit: Habit,
    completionCount: Int,
    hasSupervisors: Boolean,
    onComplete: () -> Unit,
    onNotifySupervisor: () -> Unit,
    onSkipNotification: () -> Unit
) {
    
    // Pre-compute strings to avoid calling stringResource in lambdas
    val notifyText = stringResource(R.string.reward_notify_supervisor, habit.title)
    val skipText = stringResource(R.string.reward_skip_notification)
    val completeText = stringResource(R.string.reward_complete)
    val notifyContentDesc = stringResource(R.string.accessibility_notify_supervisors)
    val skipContentDesc = stringResource(R.string.accessibility_skip_notification)
    val completeContentDesc = stringResource(R.string.accessibility_reward_sheet)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasSupervisors) {
            // 有监督人：显示通知按钮（自动高度适应多行文本）
            Button(
                onClick = onNotifySupervisor,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .semantics {
                        contentDescription = notifyContentDesc
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = notifyText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            // 跳过通知按钮
            TextButton(
                onClick = onSkipNotification,
                modifier = Modifier
                    .height(44.dp)
                    .semantics {
                        contentDescription = skipContentDesc
                    }
            ) {
                Text(
                    text = skipText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 无监督人：显示完成按钮
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .semantics {
                        contentDescription = completeContentDesc
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = completeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
