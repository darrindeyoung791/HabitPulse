package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.MalformedURLException
import java.net.URL
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBackAction: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val apiEndpoint by userPreferences.llmApiEndpointFlow.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by userPreferences.llmApiKeyFlow.collectAsStateWithLifecycle(initialValue = "")
    val modelName by userPreferences.llmModelNameFlow.collectAsStateWithLifecycle(initialValue = "glm-4-flash-250414")
    val streamingEnabled by userPreferences.llmStreamingResponseFlow.collectAsStateWithLifecycle(initialValue = true)

    var endpointInput by remember(apiEndpoint) { mutableStateOf(apiEndpoint) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var modelInput by remember(modelName) { mutableStateOf(modelName) }

    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    val listState = rememberLazyListState()

    val presetModels = listOf(
        "glm-4-flash-250414" to "glm-4-flash-250414 (默认)",
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to "glm-5.1 (最新旗舰)"
    )

    var modelDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.ai_settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.ai_settings_api_endpoint),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = {
                        if (it.length <= 200) {
                            endpointInput = it
                            scope.launch {
                                userPreferences.setLlmApiEndpoint(it)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.ai_settings_api_endpoint_label)) },
                    placeholder = { Text("https://open.bigmodel.cn/api/paas/v4/chat/completions") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        if (endpointInput.isNotEmpty()) {
                            IconButton(onClick = {
                                endpointInput = ""
                                scope.launch {
                                    userPreferences.setLlmApiEndpoint("")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(id = R.string.clear)
                                )
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(id = R.string.ai_settings_api_key),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        if (it.length <= 100) {
                            apiKeyInput = it
                            scope.launch {
                                userPreferences.setLlmApiKey(it)
                            }
                        }
                    },
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
                                IconButton(onClick = {
                                    apiKeyInput = ""
                                    scope.launch {
                                        userPreferences.setLlmApiKey("")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(id = R.string.clear)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(id = R.string.ai_settings_model),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            if (it.length <= 50) {
                                modelInput = it
                                scope.launch {
                                    userPreferences.setLlmModelName(it)
                                }
                            }
                        },
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
                                    scope.launch {
                                        userPreferences.setLlmModelName(model)
                                    }
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Button(
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
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(id = R.string.ai_settings_test_connection))
                }
            }

            testResult?.let { result ->
                item {
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
                                    stringResource(id = R.string.ai_settings_connection_success)
                                } else {
                                    result.error ?: stringResource(id = R.string.ai_settings_connection_failed)
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
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            userPreferences.setLlmStreamingResponse(!streamingEnabled)
                        }
                    },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.ai_settings_streaming),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(id = R.string.ai_settings_streaming_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = streamingEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    userPreferences.setLlmStreamingResponse(enabled)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.ai_settings_notice_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.ai_settings_notice_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

sealed class TestResult(val isSuccess: Boolean, val error: String? = null) {
    object Success : TestResult(true)
    data class Error(val message: String) : TestResult(false, message)
}

private suspend fun testApiConnection(context: android.content.Context, endpoint: String, apiKey: String, model: String): TestResult {
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
    } catch (e: Exception) {
        null
    }
}
