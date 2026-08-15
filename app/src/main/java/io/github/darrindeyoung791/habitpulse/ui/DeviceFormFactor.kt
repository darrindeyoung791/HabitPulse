package io.github.darrindeyoung791.habitpulse.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.window.core.layout.WindowSizeClass
import kotlin.math.min

/** 平板判定最小边（dp）：与既有 `smallestScreenWidthDp >= 600` 语义一致。 */
const val TABLET_MIN_WIDTH_DP = 600

/** 宽屏布局宽度下限（dp）：横屏且宽 >= 840 时启用双列/瀑布流。 */
const val WIDE_LAYOUT_MIN_WIDTH_DP = 840

/** 大屏窗口宽度下限（dp）：对齐官方 Large size class 断点。 */
const val LARGE_SCREEN_MIN_WIDTH_DP = 1200

/**
 * 统一设备形态信息。
 *
 * 业务布尔值供各页面直接使用；同时携带原始 [WindowSizeClass] 与 [Posture]，
 * 为未来折叠屏/桌面功能预留扩展点。
 */
data class DeviceFormInfo(
    val windowSizeClass: WindowSizeClass,
    val windowPosture: Posture,
    val isTabletDevice: Boolean,
    val isLandscape: Boolean,
    val windowWidthDp: Dp,
    val windowHeightDp: Dp,
) {
    val isTabletLandscape: Boolean get() = isTabletDevice && isLandscape
    val isPhoneLandscape: Boolean get() = !isTabletDevice && isLandscape
    val isWideLayout: Boolean
        get() = isLandscape &&
            windowSizeClass.isWidthAtLeastBreakpoint(WIDE_LAYOUT_MIN_WIDTH_DP)
    val isLargeWindow: Boolean
        get() = windowSizeClass.isWidthAtLeastBreakpoint(LARGE_SCREEN_MIN_WIDTH_DP)
}

/**
 * 纯逻辑判定：由窗口宽高 dp 与官方 size class 计算设备形态。
 * 无 Android 依赖，可 JVM 单元测试。
 */
fun classifyDeviceForm(
    widthDp: Dp,
    heightDp: Dp,
    windowSizeClass: WindowSizeClass,
    windowPosture: Posture = Posture(),
): DeviceFormInfo = DeviceFormInfo(
    windowSizeClass = windowSizeClass,
    windowPosture = windowPosture,
    isTabletDevice = min(widthDp.value, heightDp.value) >= TABLET_MIN_WIDTH_DP,
    isLandscape = widthDp >= heightDp,
    windowWidthDp = widthDp,
    windowHeightDp = heightDp,
)

/**
 * 全应用统一的设备形态数据源。窗口尺寸变化（旋转/分屏/折叠/桌面缩放）时自动重组。
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun rememberDeviceFormInfo(): DeviceFormInfo {
    val adaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val windowSize = currentWindowDpSize()
    return classifyDeviceForm(
        widthDp = windowSize.width,
        heightDp = windowSize.height,
        windowSizeClass = adaptiveInfo.windowSizeClass,
        windowPosture = adaptiveInfo.windowPosture,
    )
}
