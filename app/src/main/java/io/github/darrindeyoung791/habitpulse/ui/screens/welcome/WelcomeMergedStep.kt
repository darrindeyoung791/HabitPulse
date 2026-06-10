package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.dnd.DndRangeSlider
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.NotificationPermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeMergedStep(
    initialReminderEnabled: Boolean = true,
    initialDndEnabled: Boolean = true,
    initialDndStart: String = "22:00",
    initialDndEnd: String = "07:00",
    initialPersistentEnabled: Boolean = true,
    initialEndpoint: String = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    initialApiKey: String = "",
    initialModel: String = "glm-4-flash-250414",
    initialStreamingEnabled: Boolean = true,
    onComplete: (endpoint: String, apiKey: String, model: String, streamingEnabled: Boolean) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Notification state
    var hasNotificationPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var dndEnabled by remember { mutableStateOf(initialDndEnabled) }
    var dndStart by remember { mutableStateOf(initialDndStart) }
    var dndEnd by remember { mutableStateOf(initialDndEnd) }
    var persistentEnabled by remember { mutableStateOf(initialPersistentEnabled) }

    // AI state
    var endpointInput by remember { mutableStateOf(initialEndpoint) }
    var apiKeyInput by remember { mutableStateOf(initialApiKey) }
    var modelInput by remember { mutableStateOf(initialModel) }
    var streamingEnabled by remember { mutableStateOf(initialStreamingEnabled) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
        }
    }

    val presetModels = listOf(
        "glm-4-flash-250414" to "glm-4-flash-250414",
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to "glm-5.1"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
            // Left: Notification settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 12.dp, top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_notification_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.onboarding_step_notification_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Persistent notification
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (hasNotificationPermission) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_persistent_notification),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_persistent_notification_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = persistentEnabled,
                                    onCheckedChange = { persistentEnabled = it }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.settings_persistent_notification),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.settings_persistent_notification_description),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    val activity = context as? Activity
                                    val shouldShowRationale = activity?.let {
                                        NotificationHelper.shouldShowPermissionRationale(it)
                                    } ?: true
                                    if (shouldShowRationale) {
                                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationPermissionHelper.openAppSettings(context)
                                    }
                                }) {
                                    Text(stringResource(R.string.settings_persistent_notification_authorize))
                                }
                            }
                        }
                    }

                    // Reminder
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (hasNotificationPermission) reminderEnabled = !reminderEnabled },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        enabled = hasNotificationPermission
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_reminder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasNotificationPermission) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                                Text(
                                    text = stringResource(R.string.settings_reminder_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasNotificationPermission) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { if (hasNotificationPermission) reminderEnabled = it },
                                enabled = hasNotificationPermission
                            )
                        }
                    }

                    // DND
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dndEnabled = !dndEnabled },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_reminder_dnd),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_reminder_dnd_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dndEnabled,
                                    onCheckedChange = { dndEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = dndEnabled,
                                enter = expandVertically(expandFrom = Alignment.Top),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top)
                            ) {
                                DndRangeSlider(
                                    startTime = dndStart,
                                    endTime = dndEnd,
                                    onStartTimeChange = { dndStart = it },
                                    onEndTimeChange = { dndEnd = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right: AI settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 24.dp, top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ai_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.onboarding_step_ai_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = {
                        if (it.length <= 200) {
                            endpointInput = it
                            testResult = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ai_settings_api_endpoint_label)) },
                    placeholder = { Text("https://open.bigmodel.cn/api/paas/v4/chat/completions") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        if (endpointInput.isNotEmpty()) {
                            IconButton(onClick = { endpointInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        if (it.length <= 100) {
                            apiKeyInput = it
                            testResult = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ai_settings_api_key_label)) },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) stringResource(R.string.hide_password)
                                    else stringResource(R.string.show_password)
                                )
                            }
                            if (apiKeyInput.isNotEmpty()) {
                                IconButton(onClick = { apiKeyInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            if (it.length <= 50) {
                                modelInput = it
                                testResult = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text(stringResource(R.string.ai_settings_model_label)) },
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
                                    testResult = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch {
                            val result = mergedTestApiConnection(context, endpointInput, apiKeyInput, modelInput)
                            testResult = result
                            isTesting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = endpointInput.isNotBlank() && apiKeyInput.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = stringResource(R.string.ai_settings_test_connection))
                }

                testResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isSuccess) MaterialTheme.colorScheme.primaryContainer
                                           else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (result.isSuccess) stringResource(R.string.ai_settings_connection_success)
                                       else result.error ?: stringResource(R.string.ai_settings_connection_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { streamingEnabled = !streamingEnabled },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ai_settings_streaming),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.ai_settings_streaming_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = streamingEnabled,
                            onCheckedChange = { streamingEnabled = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.onboarding_step_ai_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom mask and buttons
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = {
                        testResult = null
                        onSkip()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_step_skip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Button(
                    onClick = {
                        if (endpointInput.isBlank() || apiKeyInput.isBlank() || modelInput.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.create_habit_validation_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            scope.launch {
                                val prefs = UserPreferences.getInstance(context.applicationContext)
                                prefs.setReminderEnabled(reminderEnabled)
                                prefs.setDndEnabled(dndEnabled)
                                prefs.setDndStartTime(dndStart)
                                prefs.setDndEndTime(dndEnd)
                                prefs.setPersistentNotification(persistentEnabled)
                                prefs.setLlmStreamingResponse(streamingEnabled)
                            }
                            onComplete(endpointInput, apiKeyInput, modelInput, streamingEnabled)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_step_complete),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private suspend fun mergedTestApiConnection(
    context: android.content.Context,
    endpoint: String,
    apiKey: String,
    model: String
): TestResult {
    val res = context.resources
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig.ensureChatCompletionsUrl(endpoint))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.useCaches = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val body = """
                {
                    "model": "$model",
                    "messages": [{"role": "user", "content": "hi"}],
                    "max_tokens": 10
                }
            """.trimIndent()

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode

            when (responseCode) {
                200 -> TestResult.Success
                401 -> TestResult.Error(res.getString(R.string.ai_error_auth_failed))
                403 -> TestResult.Error(res.getString(R.string.ai_error_access_denied))
                429 -> TestResult.Success
                in 400..499 -> {
                    val errorBody = try {
                        connection.errorStream?.use { errorInput ->
                            BufferedReader(InputStreamReader(errorInput, Charsets.UTF_8)).use { it.readText() }
                        } ?: ""
                    } catch (_: Exception) { "" }
                    val errorMsg = mergedExtractErrorMessage(errorBody) ?: "HTTP $responseCode"
                    TestResult.Error(errorMsg)
                }
                in 500..599 -> TestResult.Error(res.getString(R.string.ai_error_server_error, responseCode))
                else -> TestResult.Error("HTTP $responseCode")
            }
        } catch (e: SocketTimeoutException) {
            TestResult.Error(res.getString(R.string.ai_error_timeout))
        } catch (e: ConnectException) {
            TestResult.Error(res.getString(R.string.ai_error_connection_failed))
        } catch (e: UnknownHostException) {
            TestResult.Error(res.getString(R.string.ai_error_dns_failed))
        } catch (e: MalformedURLException) {
            TestResult.Error(res.getString(R.string.ai_error_invalid_url))
        } catch (e: Exception) {
            TestResult.Error(e.message ?: res.getString(R.string.ai_error_unknown))
        }
    }
}

private fun mergedExtractErrorMessage(response: String): String? {
    return try {
        val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        regex.find(response)?.groupValues?.getOrNull(1)
    } catch (_: Exception) {
        null
    }
}
