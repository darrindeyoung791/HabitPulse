package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.llm.AiConnectionTester
import io.github.darrindeyoung791.habitpulse.data.model.AIConfig
import io.github.darrindeyoung791.habitpulse.data.model.AIPreset
import io.github.darrindeyoung791.habitpulse.data.model.AIPresets
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.security.ApiKeyCrypto
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// 加密态展示：用一串圆点示意「已填写内容但已加密」，圆点不可选中/编辑
private val ENCRYPTED_DOTS = "•".repeat(12)

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
    val fragmentActivity = context as? FragmentActivity

    val configs by userPreferences.aiConfigsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val editingConfig = configs.firstOrNull { it.id == configId }
    val isEditMode = configId != null

    var nameInput by remember(editingConfig) { mutableStateOf(editingConfig?.name.orEmpty()) }
    var endpointInput by remember(editingConfig) { mutableStateOf(editingConfig?.apiEndpoint.orEmpty()) }
    var modelInput by remember(editingConfig) { mutableStateOf(editingConfig?.modelName.orEmpty()) }
    var streamingEnabled by remember(editingConfig) { mutableStateOf(editingConfig?.streamingEnabled ?: true) }
    var thinkingEnabled by remember(editingConfig) { mutableStateOf(editingConfig?.thinkingEnabled ?: false) }

    // API key 查看门控状态：
    // - apiKeyInput：当前输入框内容。编辑已配置 key 时不预填密文，初始为空。
    // - revealedPlaintext：本次会话通过生物识别解密出的明文（null = 尚未展示）。
    // - apiKeyEdited：用户是否主动改动了 key（区别于「仅查看未修改」）。
    var apiKeyInput by remember(editingConfig) { mutableStateOf("") }
    var revealedPlaintext by remember(editingConfig) { mutableStateOf<String?>(null) }
    var apiKeyEdited by remember(editingConfig) { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    // 页内 Tab 方案（仅新建模式）：0 = 预置方案，1 = 自定义。编辑模式不显示 Tab。
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedPresetId by remember { mutableStateOf<String?>(null) }
    val isPresetTab = !isEditMode && selectedTab == 0

    // 预置页重名时跳转自定义页并自动聚焦名称输入框
    var focusNameOnCustom by remember { mutableStateOf(false) }
    val nameFocusRequester = remember { FocusRequester() }

    val presetNames: Map<String, String> =
        AIPresets.ALL.associate { it.id to stringResource(it.nameRes) }

    // 选中预置：就地预填名称/端点/模型/开关默认值；保留已输入的 API 密钥不清空。
    val applyPreset: (AIPreset) -> Unit = { preset ->
        selectedPresetId = preset.id
        presetNames[preset.id]?.let { nameInput = it }
        endpointInput = preset.apiEndpoint
        modelInput = preset.modelName
        streamingEnabled = preset.defaultStreaming
        thinkingEnabled = preset.defaultThinking
    }

    val defaultModelLabel = stringResource(id = R.string.ai_settings_model_default_label, "glm-4-flash-250414")
    val flagshipModelLabel = stringResource(id = R.string.ai_settings_model_flagship_label, "glm-5.1")

    val presetModels = listOf(
        "glm-4-flash-250414" to defaultModelLabel,
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to flagshipModelLabel
    )

    // 设备是否具备强生物识别或设备凭据（决定「查看」按钮可用性）
    val canAuthenticate = remember { ApiKeyCrypto.hasAuthenticationMethod(context) }

    // BiometricPrompt 回调引用（rememberUpdatedState 保证闭包拿到最新状态）
    val pendingDisplayCipher = remember { mutableStateOf("") }
    val onRevealSucceeded = rememberUpdatedState<(String) -> Unit> { plaintext ->
        revealedPlaintext = plaintext
        apiKeyInput = plaintext
        apiKeyEdited = false
        showApiKey = true
    }
    val onRevealFailed = rememberUpdatedState<() -> Unit> {
        // 解密失败（密钥失效）→ 引导重新录入，不崩溃
        revealedPlaintext = null
        apiKeyInput = ""
        apiKeyEdited = true
        showApiKey = false
        Toast.makeText(
            context,
            context.getString(R.string.ai_config_reveal_failed),
            Toast.LENGTH_LONG
        ).show()
    }

    val biometricPrompt = remember(fragmentActivity) {
        fragmentActivity?.let { host ->
            val executor = ContextCompat.getMainExecutor(host)
            BiometricPrompt(
                host,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val display = pendingDisplayCipher.value
                        val cipher = result.cryptoObject?.cipher
                        if (cipher != null && display.isNotBlank()) {
                            try {
                                onRevealSucceeded.value(ApiKeyCrypto.finishRevealDecrypt(cipher, display))
                            } catch (e: Exception) {
                                onRevealFailed.value()
                            }
                        } else {
                            onRevealFailed.value()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // 验证失败/取消 → 保持隐藏
                    }

                    override fun onAuthenticationFailed() {
                        // 保持隐藏
                    }
                }
            )
        }
    }

    val revealSource = editingConfig?.takeIf { it.hasApiKeyConfigured() }
    val startReveal: (() -> Unit)? = if (
        isEditMode && revealSource != null && canAuthenticate && biometricPrompt != null
    ) {
        val source = revealSource
        {
            val displayCipher = source.displayCipher
            val cipher = ApiKeyCrypto.createRevealDecryptCipher(displayCipher)
            if (cipher != null) {
                pendingDisplayCipher.value = displayCipher
                val promptInfo = ApiKeyCrypto.createRevealPromptInfo(
                    title = context.getString(R.string.ai_config_reveal_title),
                    subtitle = context.getString(R.string.ai_config_reveal_subtitle),
                    negativeButtonText = context.getString(R.string.dialog_cancel)
                )
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                onRevealFailed.value()
            }
        }
    } else {
        null
    }

    // 展示后自动隐藏：超时（60s）或离开编辑页后恢复密文态，每次查看需重新验证
    LaunchedEffect(revealedPlaintext) {
        val original = revealedPlaintext
        if (original != null) {
            delay(60_000)
            if (apiKeyInput == original) {
                apiKeyInput = ""
                revealedPlaintext = null
                showApiKey = false
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            revealedPlaintext = null
            showApiKey = false
        }
    }

    // 编辑已有已配置 key 时，字段为空视为「未改动，保留原密文」
    val hasConfiguredKey = isEditMode && editingConfig?.hasApiKeyConfigured() == true
    val apiKeyChanged = when {
        hasConfiguredKey -> apiKeyEdited
        else -> apiKeyInput.isNotBlank()
    }

    // 加密态展示：已配置 key 且未解密、未手动编辑时，显示不可选中的圆点
    val isEncryptedDisplay = hasConfiguredKey && revealedPlaintext == null && apiKeyInput.isBlank() && !apiKeyEdited
    val apiKeyFieldValue = if (isEncryptedDisplay) ENCRYPTED_DOTS else apiKeyInput

    // 重名检测：配置名不可重复（忽略大小写、按去除首尾空白比较），模型类型可重复；
    // 判断时排除当前正在编辑的这条，避免编辑自身名称时误判。
    val nameTrimmed = nameInput.trim()
    val duplicateName = nameTrimmed.isNotBlank() &&
        configs.any { it.id != editingConfig?.id && it.name.trim().equals(nameTrimmed, ignoreCase = true) }

    val canSave = !duplicateName && nameInput.isNotBlank() && endpointInput.isNotBlank() && modelInput.isNotBlank() &&
        (apiKeyInput.isNotBlank() || hasConfiguredKey)

    val hasUnsavedChanges =
        nameInput != (editingConfig?.name.orEmpty()) ||
            endpointInput != (editingConfig?.apiEndpoint.orEmpty()) ||
            apiKeyChanged ||
            modelInput != (editingConfig?.modelName.orEmpty()) ||
            streamingEnabled != (editingConfig?.streamingEnabled ?: true) ||
            thinkingEnabled != (editingConfig?.thinkingEnabled ?: false)

    val handleBack: () -> Unit = {
        if (hasUnsavedChanges) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_unsaved_changes),
                Toast.LENGTH_SHORT
            ).show()
        }
        onBack()
    }
    BackHandler { handleBack() }

    NewSettingsScaffold(
        title = stringResource(
            id = if (isEditMode) R.string.ai_config_edit_title else R.string.ai_config_add_title
        ),
        onBack = handleBack,
        onHelp = onOpenHelp,
        floatingActionButton = {
            val fabInteractionSource = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
                onClick = {
                    if (!canSave) return@ExtendedFloatingActionButton
                    val trimmedName = nameInput.trim()
                    if (trimmedName.isBlank()) return@ExtendedFloatingActionButton
                    scope.launch {
                        val preserveKey = hasConfiguredKey && !apiKeyEdited
                        val values = { existing: AIConfig ->
                            existing.copy(
                                name = trimmedName,
                                apiEndpoint = endpointInput.trim(),
                                apiKey = if (preserveKey) existing.apiKey else apiKeyInput.trim(),
                                displayCipher = if (preserveKey) existing.displayCipher else "",
                                keyVersion = if (preserveKey) existing.keyVersion else 0,
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
                                displayCipher = "",
                                keyVersion = 0,
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
                interactionSource = fabInteractionSource,
                containerColor = if (canSave) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (canSave) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.ai_settings_save)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null
                    )
                },
                text = { Text(text = stringResource(id = R.string.ai_settings_save)) }
            )
            PressVibrationFeedback(interactionSource = fabInteractionSource, enabled = canSave)
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditMode) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.ai_config_tab_presets)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.ai_config_tab_custom)) }
                )
            }
            Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
        }

        if (isPresetTab) {
            Text(
                text = stringResource(id = R.string.ai_config_presets_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            SettingsSegmentedGroup {
                AIPresets.ALL.forEachIndexed { i, preset ->
                    val isSelected = preset.id == selectedPresetId
                    SettingsSegmentedItem(
                        index = i,
                        count = AIPresets.ALL.size,
                        headline = presetNames[preset.id].orEmpty(),
                        supportingText = "${preset.modelName} · ${endpointHost(preset.apiEndpoint)}",
                        selected = isSelected,
                        onClick = { applyPreset(preset) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (preset.isFree) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ai_preset_free),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Filled.RadioButtonChecked
                                    } else {
                                        Icons.Filled.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    )
                }
            }

            if (duplicateName) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.ai_config_name_duplicate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        selectedTab = 1
                        focusNameOnCustom = true
                    }) {
                        Text(
                            text = stringResource(id = R.string.ai_config_name_duplicate_fix),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // 从预置页跳来重名修复：进入自定义页后自动聚焦名称输入框
            LaunchedEffect(Unit) {
                if (focusNameOnCustom) {
                    nameFocusRequester.requestFocus()
                    focusNameOnCustom = false
                }
            }
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 40) nameInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester),
                label = { Text(stringResource(id = R.string.ai_config_name_label)) },
                placeholder = { Text(stringResource(id = R.string.ai_config_name_placeholder)) },
                singleLine = true,
                isError = duplicateName,
                supportingText = {
                    if (duplicateName) {
                        Text(stringResource(id = R.string.ai_config_name_duplicate))
                    }
                },
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
        }

        OutlinedTextField(
            value = apiKeyFieldValue,
            onValueChange = {
                if (it.length <= 100) {
                    apiKeyInput = it
                    apiKeyEdited = true
                    if (revealedPlaintext != null && it == revealedPlaintext) {
                        apiKeyEdited = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.ai_settings_api_key_label)) },
            // 加密态：禁用输入/选中，仅展示圆点；尾图标（查看/清除）仍可点击
            enabled = !isEncryptedDisplay,
            colors = if (isEncryptedDisplay) {
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.Transparent,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    val needsReveal = hasConfiguredKey && revealedPlaintext == null && !apiKeyEdited
                    IconButton(
                        onClick = {
                            if (needsReveal) {
                                startReveal?.invoke()
                            } else {
                                showApiKey = !showApiKey
                            }
                        },
                        enabled = if (needsReveal) canAuthenticate else true
                    ) {
                        Icon(
                            imageVector = if (needsReveal) {
                                Icons.Outlined.Lock
                            } else if (showApiKey) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (needsReveal) {
                                stringResource(id = R.string.ai_config_reveal_key)
                            } else if (showApiKey) {
                                stringResource(id = R.string.hide_password)
                            } else {
                                stringResource(id = R.string.show_password)
                            }
                        )
                    }
                    if (apiKeyInput.isNotEmpty()) {
                        IconButton(onClick = {
                            apiKeyInput = ""
                            revealedPlaintext = null
                            apiKeyEdited = true
                            showApiKey = false
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                        }
                    }
                }
            }
        )

        if (!isPresetTab) {
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
                        val testKey = if (apiKeyInput.isNotBlank()) {
                            apiKeyInput.trim()
                        } else {
                            // 编辑已有配置且未改动 key：用运行时密钥静默解密后测试
                            try {
                                editingConfig?.apiKey
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { ApiKeyCrypto.decryptRuntime(it) }
                                    .orEmpty()
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        val result = AiConnectionTester.test(
                            context,
                            endpointInput.trim(),
                            testKey,
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
            text = stringResource(id = R.string.ai_config_info_may_change),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
        )

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

private fun endpointHost(endpoint: String): String {
    return try {
        Uri.parse(endpoint.trim()).host ?: endpoint
    } catch (e: Exception) {
        endpoint
    }
}
