package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.generateSampleHabits
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDebugScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onNavigateDebugReminder: () -> Unit,
    onNavigateDebugVibration: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences.getInstance(context) }

    var showAddSampleDialog by remember { mutableStateOf(false) }
    var showResetOnboardingDialog by remember { mutableStateOf(false) }

    val toolRetryLimit by prefs.aiToolRetryLimitFlow.collectAsStateWithLifecycle(initialValue = 20)
    val maxOutputTokens by prefs.aiMaxOutputTokensFlow.collectAsStateWithLifecycle(initialValue = 20000)

    var editingAiParamId by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(
        title = stringResource(id = R.string.settings_debug_title),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.settings_debug_add_sample_habits),
                supportingText = stringResource(id = R.string.settings_debug_add_sample_habits_description),
                showArrow = false,
                onClick = { showAddSampleDialog = true }
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_debug_reset_onboarding),
                supportingText = stringResource(id = R.string.settings_debug_reset_onboarding_description),
                showArrow = false,
                onClick = { showResetOnboardingDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        SettingsSegmentedGroup(tintOffset = 1) {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.settings_reminder_settings),
                supportingText = stringResource(id = R.string.settings_reminder_settings_description),
                onClick = onNavigateDebugReminder
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_debug_vibration),
                supportingText = stringResource(id = R.string.settings_debug_vibration_description),
                onClick = onNavigateDebugVibration
            )
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        SettingsSegmentedGroup(tintOffset = 2) {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.settings_debug_ai_retry_limit),
                supportingText = toolRetryLimit.toString(),
                showArrow = false,
                onClick = { editingAiParamId = "retry" }
            )
            SettingsSegmentedItem(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.settings_debug_ai_max_output_tokens),
                supportingText = maxOutputTokens.toString(),
                showArrow = false,
                onClick = { editingAiParamId = "output" }
            )
        }
    }

    if (showAddSampleDialog) {
        AlertDialog(
            onDismissRequest = { showAddSampleDialog = false },
            title = { Text(stringResource(id = R.string.debug_add_sample_data_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.debug_add_sample_data_warning),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(id = R.string.debug_add_sample_data_dialog_message))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddSampleDialog = false
                    scope.launch {
                        val application = context.applicationContext as HabitPulseApplication
                        val database = application.database
                        val (habits, completions) = generateSampleHabits()
                        habits.forEach { database.habitDao().insert(it) }
                        completions.forEach { database.habitCompletionDao().insert(it) }
                        Toast.makeText(
                            context,
                            context.getString(R.string.debug_sample_data_added),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(stringResource(id = R.string.debug_add_sample_data_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddSampleDialog = false }) {
                    Text(stringResource(id = R.string.debug_add_sample_data_no))
                }
            }
        )
    }

    if (showResetOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { showResetOnboardingDialog = false },
            title = { Text(stringResource(id = R.string.settings_debug_reset_onboarding_dialog_title)) },
            text = { Text(stringResource(id = R.string.settings_debug_reset_onboarding_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetOnboardingDialog = false
                    val application = context.applicationContext as HabitPulseApplication
                    application.habitViewModel.resetOnboarding()
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_debug_reset_onboarding_done),
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetOnboardingDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    editingAiParamId?.let { id ->
        val (title, initialValue, apply) = when (id) {
            "retry" -> Triple(
                stringResource(R.string.settings_debug_ai_retry_limit),
                toolRetryLimit,
                prefs::setAiToolRetryLimit
            )
            else -> Triple(
                stringResource(R.string.settings_debug_ai_max_output_tokens),
                maxOutputTokens,
                prefs::setAiMaxOutputTokens
            )
        }
        NumberInputDialog(
            title = title,
            initialValue = initialValue,
            onDismiss = { editingAiParamId = null },
            onConfirm = { value ->
                scope.launch { apply(value) }
                editingAiParamId = null
            }
        )
    }
}

@Composable
private fun NumberInputDialog(
    title: String,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() }.take(6) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let(onConfirm) ?: Unit },
                enabled = text.toIntOrNull() != null
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}