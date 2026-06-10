package io.github.darrindeyoung791.habitpulse.ui.screens.welcome

import android.widget.Toast
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
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
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
fun WelcomeAIStep(
    initialEndpoint: String = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    initialApiKey: String = "",
    initialModel: String = "glm-4-flash-250414",
    initialStreamingEnabled: Boolean = true,
    onComplete: (endpoint: String, apiKey: String, model: String, streamingEnabled: Boolean) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var endpointInput by remember { mutableStateOf(initialEndpoint) }
    var apiKeyInput by remember { mutableStateOf(initialApiKey) }
    var modelInput by remember { mutableStateOf(initialModel) }
    var streamingEnabled by remember { mutableStateOf(initialStreamingEnabled) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val presetModels = listOf(
        "glm-4-flash-250414" to "glm-4-flash-250414",
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to "glm-5.1"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 80.dp),
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
                modifier = Modifier.padding(horizontal = 16.dp)
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
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
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
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear)
                                )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Test connection button
            FilledTonalButton(
                onClick = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        val result = testApiConnection(context, endpointInput, apiKeyInput, modelInput)
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

            // Test result card
            testResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isSuccess) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (result.isSuccess) {
                                stringResource(R.string.ai_settings_connection_success)
                            } else {
                                result.error ?: stringResource(R.string.ai_settings_connection_failed)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.isSuccess) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Streaming toggle
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom mask and buttons row
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
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_previous),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                            UserPreferences.getInstance(context.applicationContext)
                                .setLlmStreamingResponse(streamingEnabled)
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

private suspend fun testApiConnection(
    context: android.content.Context,
    endpoint: String,
    apiKey: String,
    model: String
): TestResult {
    val res = context.resources
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(LLMConfig.ensureChatCompletionsUrl(endpoint))
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
                            BufferedReader(InputStreamReader(errorInput, Charsets.UTF_8)).use {
                                it.readText()
                            }
                        } ?: ""
                    } catch (_: Exception) { "" }
                    val errorMsg = extractErrorMessage(errorBody) ?: "HTTP $responseCode"
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

private fun extractErrorMessage(response: String): String? {
    return try {
        val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        regex.find(response)?.groupValues?.getOrNull(1)
    } catch (_: Exception) {
        null
    }
}

sealed class TestResult(val isSuccess: Boolean, val error: String? = null) {
    data object Success : TestResult(true)
    data class Error(val message: String) : TestResult(false, message)
}
