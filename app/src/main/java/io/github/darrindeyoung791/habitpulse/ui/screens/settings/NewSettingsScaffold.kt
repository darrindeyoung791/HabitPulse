package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.ScrollEdgeFadeOverlay
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHapticsEnabled
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberPressVibrationParams
import io.github.darrindeyoung791.habitpulse.ui.utils.vibrateShort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    onHelp: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onBack,
                        interactionSource = backInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                    PressVibrationFeedback(interactionSource = backInteractionSource)
                },
                actions = {
                    if (onHelp != null) {
                        val helpInteractionSource = remember { MutableInteractionSource() }
                        IconButton(onClick = onHelp, interactionSource = helpInteractionSource) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = stringResource(id = R.string.webview_help)
                            )
                        }
                        PressVibrationFeedback(interactionSource = helpInteractionSource)
                    }
                }
            )
        },
        floatingActionButton = { floatingActionButton() }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        val context = LocalContext.current
        val hapticsEnabled = rememberHapticsEnabled()
        val (vibrationDurationMs, vibrationAmplitude) = rememberPressVibrationParams()
        val currentHapticsEnabled by rememberUpdatedState(hapticsEnabled)
        val currentDuration by rememberUpdatedState(vibrationDurationMs)
        val currentAmplitude by rememberUpdatedState(vibrationAmplitude)

        LaunchedEffect(scrollState) {
            var lastValue = scrollState.value
            snapshotFlow { scrollState.value to scrollState.maxValue }
                .collect { (value, maxValue) ->
                    val canScroll = maxValue > 0
                    if (canScroll) {
                        val hitTop = value <= 0 && lastValue > 0
                        val hitBottom = value >= maxValue && lastValue < maxValue
                        if (currentHapticsEnabled && (hitTop || hitBottom)) {
                            vibrateShort(context, currentDuration, currentAmplitude)
                        }
                    }
                    lastValue = value
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(contentPadding)
            ) {
                content()
            }

            ScrollEdgeFadeOverlay(
                scrollState = scrollState,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}