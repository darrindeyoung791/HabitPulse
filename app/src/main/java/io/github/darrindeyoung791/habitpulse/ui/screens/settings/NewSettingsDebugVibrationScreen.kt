package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedBox
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationDefaultAmplitude
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationDurationMs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 调试 → 震动调试子页面：
 * 调整按压震动时长与强度，并提供立即测试按钮与恢复默认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsDebugVibrationScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val durationMs by userPreferences.pressVibrationDurationMsFlow.collectAsStateWithLifecycle(initialValue = PressVibrationDurationMs)
    val amplitude by userPreferences.pressVibrationAmplitudeFlow.collectAsStateWithLifecycle(initialValue = PressVibrationDefaultAmplitude)

    var sliderDuration by remember(durationMs) { mutableFloatStateOf(durationMs.toFloat()) }
    var sliderAmplitude by remember(amplitude) { mutableFloatStateOf(amplitude.toFloat()) }

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_debug_vibration_title),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SectionHeader(text = stringResource(id = R.string.settings_debug_vibration_duration_group))
        SettingsSegmentedBox(index = 0, count = 1) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_debug_vibration_duration),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${sliderDuration.roundToInt()} ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = sliderDuration,
                    onValueChange = { sliderDuration = snapDuration(it) },
                    valueRange = 5f..200f,
                    steps = 38,
                    onValueChangeFinished = {
                        val value = sliderDuration.roundToInt().coerceIn(5, 200)
                        scope.launch { userPreferences.setPressVibrationDurationMs(value.toLong()) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        SectionHeader(text = stringResource(id = R.string.settings_debug_vibration_amplitude_group))
        SettingsSegmentedBox(index = 0, count = 1) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_debug_vibration_amplitude),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${sliderAmplitude.roundToInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = sliderAmplitude,
                    onValueChange = { sliderAmplitude = snapAmplitude(it) },
                    valueRange = 1f..255f,
                    steps = 253,
                    onValueChangeFinished = {
                        val value = sliderAmplitude.roundToInt().coerceIn(1, 255)
                        scope.launch { userPreferences.setPressVibrationAmplitude(value) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        FilledTonalButton(
            onClick = {
                vibrateTest(
                    context,
                    sliderDuration.roundToInt().coerceIn(5, 200).toLong(),
                    sliderAmplitude.roundToInt().coerceIn(1, 255)
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_debug_vibration_test),
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Vibration,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.settings_debug_vibration_test))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    userPreferences.resetPressVibration()
                    sliderDuration = PressVibrationDurationMs.toFloat()
                    sliderAmplitude = PressVibrationDefaultAmplitude.toFloat()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.settings_debug_vibration_reset))
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        Text(
            text = stringResource(id = R.string.settings_debug_vibration_notice_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.settings_debug_vibration_notice_content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

private fun snapDuration(value: Float): Float {
    val snapped = (value / 5f).roundToInt() * 5f
    return snapped.coerceIn(5f, 200f)
}

private fun snapAmplitude(value: Float): Float {
    val snapped = value.roundToInt().toFloat()
    return snapped.coerceIn(1f, 255f)
}

private fun vibrateTest(context: android.content.Context, durationMs: Long, amplitude: Int) {
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    try {
        val effectiveAmplitude = if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, effectiveAmplitude))
    } catch (_: Throwable) {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
    }
}