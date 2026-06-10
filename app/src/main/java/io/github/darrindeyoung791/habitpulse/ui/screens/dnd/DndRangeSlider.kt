package io.github.darrindeyoung791.habitpulse.ui.screens.dnd

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R

@Composable
fun DndRangeSlider(
    startTime: String,
    endTime: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rangeStart = 1260f
    val rangeEnd = 1920f
    val stepCount = 21

    var curStart by remember(startTime) { mutableFloatStateOf(snapToStep(toLinear(startTime))) }
    var curEnd by remember(endTime) { mutableFloatStateOf(snapToStep(toLinear(endTime))) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stepToTime(curStart),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.settings_reminder_dnd_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(id = R.string.settings_reminder_dnd_to),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stepToTime(curEnd),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.settings_reminder_dnd_end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        RangeSlider(
            value = curStart..curEnd,
            onValueChange = { range ->
                curStart = range.start
                curEnd = range.endInclusive
            },
            onValueChangeFinished = {
                val snappedStart = snapToStep(curStart)
                val snappedEnd = snapToStep(curEnd)
                if (snappedStart == snappedEnd) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.reminder_settings_dnd_same_time),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                onStartTimeChange(stepToTime(snappedStart))
                onEndTimeChange(stepToTime(snappedEnd))
            },
            valueRange = rangeStart..rangeEnd,
            steps = stepCount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun toLinear(time: String): Float {
    val parts = time.split(":")
    var h = parts[0].toInt()
    val m = parts[1].toInt()
    if (h < 12) h += 24
    return (h * 60 + m).toFloat()
}

fun snapToStep(value: Float): Float {
    val pos = kotlin.math.round((value - 1260f) / 30f).toInt().coerceIn(0, 22)
    return 1260f + pos * 30f
}

fun stepToTime(value: Float): String {
    var totalMin = value.toInt()
    var h = totalMin / 60
    val m = totalMin % 60
    if (h >= 24) h -= 24
    return String.format("%02d:%02d", h, m)
}
