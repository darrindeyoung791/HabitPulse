package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.llm.AiConnectionTester
import io.github.darrindeyoung791.habitpulse.data.model.AIConfig
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 添加 / 编辑一条 AI 配置。
 *
 * @param configId 非空 = 编辑已有配置；null = 新建。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsAIEditScreen(
    configId: String?,
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val configs by userPreferences.aiConfigsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val editingConfig = configs.firstOrNull { it.id == configId }
    val isEditMode = configId != null

    var nameInput by remember(editingConfig) { mutableStateOf(editingConfig?.name.orEmpty()) }
    var endpointInput by remember(editingConfig) { mutableStateOf(editingConfig?.apiEndpoint.orEmpty()) }
    var apiKeyInput by remember(editingConfig) { mutableStateOf(editingConfig?.apiKey.orEmpty()) }
    var modelInput by remember(editingConfig) { mutableStateOf(editingConfig?.modelName.orEmpty()) }
    var streamingEnabled by remember(editingConfig) { mutableStateOf(editingConfig?.streamingEnabled ?: true) }
    var thinkingEnabled by remember(editingConfig) { mutableStateOf(editingConfig?.thinkingEnabled ?: false) }

    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val defaultModelLabel = stringResource(id = R.string.ai_settings_model_default_label, "glm-4-flash-250414")
    val flagshipModelLabel = stringResource(id = R.string.ai_settings_model_flagship_label, "glm-5.1")

    val presetModels = listOf(
        "glm-4-flash-250414" to defaultModelLabel,
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to flagshipModelLabel
    )

    val canSave = nameInput.isNotBlank() && endpointInput.isNotBlank() &&
        apiKeyInput.isNotBlank() && modelInput.isNotBlank()

    NewSettingsScaffold(
        title = stringResource(
            id = if (isEditMode) R.string.ai_config_edit_title else R.string.ai_config_add_title
        ),
        onBack = onBack,
        onHelp = onOpenHelp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { if (it.length <= 40) nameInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.ai_config_name_label)) },
            placeholder = { Text(stringResource(id = R.string.ai_config_name_placeholder)) },
            singleLine = true,
            trailingIcon = {
                if (nameInput.isNotEmpty()) {
                    IconButton(onClick = { nameInput = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = endpointInput,
            onValueChange = { if (it.length <= 200) endpointInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.ai_settings_api_endpoint_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                if (endpointInput.isNotEmpty()) {
                    IconButton(onClick = { endpointInput = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { if (it.length <= 100) apiKeyInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.ai_settings_api_key_label)) },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) stringResource(id = R.string.hide_password)
                            else stringResource(id = R.string.show_password)
                        )
                    }
                    if (apiKeyInput.isNotEmpty()) {
                        IconButton(onClick = { apiKeyInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = modelDropdownExpanded,
            onExpandedChange = { modelDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = modelInput,
                onValueChange = { if (it.length <= 50) modelInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text(stringResource(id = R.string.ai_settings_model_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = { modelDropdownExpanded = false }
            ) {
                presetModels.forEach { (model, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            modelInput = model
                            modelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        SettingsSegmentedGroup {
            SettingsSegmentedSwitch(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.ai_settings_streaming),
                supportingText = stringResource(id = R.string.ai_settings_streaming_description),
                leadingIcon = Icons.Outlined.Subject,
                checked = streamingEnabled,
                onCheckedChange = { streamingEnabled = it }
            )
            SettingsSegmentedSwitch(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.ai_settings_thinking),
                supportingText = stringResource(id = R.string.ai_settings_thinking_description),
                leadingIcon = Icons.Outlined.Psychology,
                checked = thinkingEnabled,
                onCheckedChange = { thinkingEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        SettingsSegmentedGroup(tintOffset = 2) {
            SettingsSegmentedItem(
                index = 0,
                count = 1,
                headline = stringResource(id = R.string.ai_settings_test_connection),
                supportingText = stringResource(id = R.string.ai_settings_test_connection_description),
                leadingIcon = Icons.AutoMirrored.Outlined.Send,
                showArrow = false,
                enabled = canSave && !isTesting,
                onClick = {
                    isTesting = true
                    scope.launch {
                        val result = AiConnectionTester.test(
                            context,
                            endpointInput.trim(),
                            apiKeyInput.trim(),
                            modelInput.trim()
                        )
                        isTesting = false
                        android.widget.Toast.makeText(
                            context,
                            if (result.isSuccess) context.getString(R.string.ai_settings_connection_success)
                            else result.error ?: context.getString(R.string.ai_settings_connection_failed),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                },
                trailing = {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))

        Button(
            onClick = {
                val trimmedName = nameInput.trim()
                if (trimmedName.isBlank()) return@Button
                scope.launch {
                    val values = { existing: AIConfig ->
                        existing.copy(
                            name = trimmedName,
                            apiEndpoint = endpointInput.trim(),
                            apiKey = apiKeyInput.trim(),
                            modelName = modelInput.trim(),
                            streamingEnabled = streamingEnabled,
                            thinkingEnabled = thinkingEnabled
                        )
                    }
                    if (isEditMode && editingConfig != null) {
                        userPreferences.updateAIConfig(values(editingConfig))
                    } else {
                        val newConfig = AIConfig(
                            id = UUID.randomUUID().toString(),
                            name = trimmedName,
                            apiEndpoint = endpointInput.trim(),
                            apiKey = apiKeyInput.trim(),
                            modelName = modelInput.trim(),
                            streamingEnabled = streamingEnabled,
                            thinkingEnabled = thinkingEnabled
                        )
                        userPreferences.addAIConfig(newConfig)
                        userPreferences.setActiveAIConfig(newConfig.id)
                    }
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = canSave,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.ai_settings_save))
        }

        if (isEditMode) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.ai_config_delete))
            }
        }

        Text(
            text = stringResource(id = R.string.ai_settings_notice_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
        )
        Text(
            text = stringResource(id = R.string.ai_settings_notice_content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(id = R.string.ai_config_delete_title)) },
            text = { Text(stringResource(id = R.string.ai_config_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        editingConfig?.let { userPreferences.deleteAIConfig(it.id) }
                        onBack()
                    }
                }) { Text(stringResource(id = R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }
}
