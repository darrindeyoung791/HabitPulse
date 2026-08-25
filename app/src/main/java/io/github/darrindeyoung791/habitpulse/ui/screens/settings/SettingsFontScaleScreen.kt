package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedBox
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHapticsEnabled
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberPressVibrationParams
import io.github.darrindeyoung791.habitpulse.ui.utils.vibrateShort
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android 系统字体缩放档位（FontScale），与系统「字体大小」设置一一对应。
 * 滑杆值使用档位索引（0..6），保证每个刻度都落在真实档位上。
 */
private val FontScalePresets = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.45f, 1.6f, 2.0f)
private val FontScaleMaxIndex = FontScalePresets.size - 1
private val FontScaleStandardIndex = FontScalePresets.indexOf(1.0f)

/**
 * 每个档位对应的语义标签资源 ID，供屏幕阅读器朗读。
 * 索引与 FontScalePresets 一一对应。
 */
@Composable
private fun fontScaleLabel(index: Int): String {
    val labels = listOf(
        stringResource(id = R.string.settings_font_scale_smallest),   // 0.85
        stringResource(id = R.string.settings_font_scale_small),      // 1.0
        stringResource(id = R.string.settings_font_scale_standard),   // 1.15
        stringResource(id = R.string.settings_font_scale_large),      // 1.3
        stringResource(id = R.string.settings_font_scale_larger),     // 1.45
        stringResource(id = R.string.settings_font_scale_extra_large),// 1.6
        stringResource(id = R.string.settings_font_scale_largest)     // 2.0
    )
    return labels[index.coerceIn(0, FontScaleMaxIndex)]
}

private fun nearestIndex(scale: Float): Int {
    return FontScalePresets.indices.minByOrNull { abs(FontScalePresets[it] - scale) }
        ?: FontScaleStandardIndex
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFontScaleScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences.getInstance(context) }

    // 页内待定状态：离开页面（返回）时一次性写入生效。
    // 初始值只在进入页面时读取一次持久化值（first()），仅初次跟随系统；
    // 用户关闭「跟随系统」后，再次进入仍保持自定义值，
    // 不能依赖 collectAsStateWithLifecycle 的 initialValue（恒为默认值，会遮蔽已保存的真实值）。
    var followSystem by remember { mutableStateOf(true) }
    var customIndex by remember { mutableFloatStateOf(FontScaleStandardIndex.toFloat()) }
    LaunchedEffect(userPreferences) {
        followSystem = userPreferences.fontScaleFollowSystemFlow.first()
        customIndex = nearestIndex(userPreferences.fontScaleFlow.first()).toFloat()
    }

    // 跟随系统时，滑杆滚动到当前系统缩放最接近的那一档
    val systemIndex = nearestIndex(LocalConfiguration.current.fontScale)

    // 跟随系统时，将 customIndex 同步为 systemIndex，避免关闭跟随系统时滑块跳变
    LaunchedEffect(systemIndex, followSystem) {
        if (followSystem) {
            customIndex = systemIndex.toFloat()
        }
    }

    // 提交：先持久化再退出，确保写入完成
    var commit by remember { mutableStateOf(false) }
    LaunchedEffect(commit) {
        if (commit) {
            userPreferences.setFontScaleFollowSystem(followSystem)
            userPreferences.setFontScale(FontScalePresets[customIndex.toInt()])
            onBack()
        }
    }
    val saveAndBack: () -> Unit = { commit = true }
    BackHandler(onBack = saveAndBack)

    SettingsScaffold(
        title = stringResource(id = R.string.settings_font_scale),
        onBack = saveAndBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            SettingsSegmentedBox(index = 0, count = 2) {
                FontScaleSlider(
                    valueIndex = if (followSystem) systemIndex.toFloat() else customIndex,
                    visuallyDisabled = followSystem,
                    onIndexChange = { newIndex ->
                        if (followSystem) {
                            followSystem = false
                        }
                        customIndex = newIndex
                    }
                )
            }
            SettingsSegmentedSwitch(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_font_scale_follow_system),
                checked = followSystem,
                onCheckedChange = { isChecked ->
                    followSystem = isChecked
                    if (!isChecked) {
                        customIndex = systemIndex.toFloat()
                    }
                }
            )
        }
    }
}

@Composable
private fun FontScaleSlider(
    valueIndex: Float,
    visuallyDisabled: Boolean,
    onIndexChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val hapticsEnabled = rememberHapticsEnabled()
    val (vibrationDurationMs, vibrationAmplitude) = rememberPressVibrationParams()

    var lastIndex by remember { mutableFloatStateOf(valueIndex) }
    LaunchedEffect(valueIndex) { lastIndex = valueIndex }

    // 使用 roundToInt() 确保精确吸附到整数索引，避免浮点误差
    val currentIndex = valueIndex.roundToInt().coerceIn(0, FontScaleMaxIndex)
    val currentLabel = fontScaleLabel(currentIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 当前字体大小标签（居中显示）
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.titleMedium,
            color = if (visuallyDisabled) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 最小档位：小 A
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = valueIndex,
                onValueChange = { rawIndex ->
                    // 使用 roundToInt() 确保精确吸附，避免 2.9999..3.0 的浮点误差
                    val snapped = rawIndex.roundToInt().toFloat().coerceIn(0f, FontScaleMaxIndex.toFloat())
                    if (snapped != lastIndex && hapticsEnabled) {
                        vibrateShort(context, vibrationDurationMs, vibrationAmplitude)
                    }
                    lastIndex = snapped
                    onIndexChange(snapped)
                },
                valueRange = 0f..FontScaleMaxIndex.toFloat(),
                steps = FontScaleMaxIndex - 1,
                colors = if (visuallyDisabled) DisabledSliderColors() else SliderDefaults.colors(),
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        stateDescription = currentLabel
                        contentDescription = currentLabel
                    }
            )
            // 最大档位：大 A
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 跟随系统时的滑杆配色：整体呈禁用（灰化）样式，但滑杆仍可拖动，
 * 首次拖动即关闭「跟随系统」开关、进入自定义模式。
 */
@Composable
private fun DisabledSliderColors(): SliderColors {
    val scheme = MaterialTheme.colorScheme
    return SliderDefaults.colors(
        thumbColor = scheme.onSurface.copy(alpha = 0.38f),
        activeTrackColor = scheme.primary.copy(alpha = 0.38f),
        inactiveTrackColor = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
        disabledThumbColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTrackColor = scheme.primary.copy(alpha = 0.38f),
        disabledInactiveTrackColor = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
        activeTickColor = scheme.primary.copy(alpha = 0.38f),
        inactiveTickColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTickColor = scheme.primary.copy(alpha = 0.38f),
        disabledInactiveTickColor = scheme.onSurface.copy(alpha = 0.38f)
    )
}