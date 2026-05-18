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
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import kotlinx.coroutines.launch

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
    val modelName by userPreferences.llmModelNameFlow.collectAsStateWithLifecycle(initialValue = "glm-4.7-flash")

    var endpointInput by remember(apiEndpoint) { mutableStateOf(apiEndpoint) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var modelInput by remember(modelName) { mutableStateOf(modelName) }

    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    val listState = rememberLazyListState()

    val presetModels = listOf(
        "glm-4.7-flash" to "glm-4.7-flash (默认)",
        "glm-4-flash" to "glm-4-flash",
        "glm-4" to "glm-4",
        "glm-3-flash" to "glm-3-flash"
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
                    placeholder = { Text("https://open.bigmodel.cn/api/paas/v4/") },
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
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) stringResource(id = R.string.hide_password)
                                                       else stringResource(id = R.string.show_password)
                                )
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
                            val result = testApiConnection(endpointInput, apiKeyInput, modelInput)
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

private suspend fun testApiConnection(endpoint: String, apiKey: String, model: String): TestResult {
    return try {
        val url = java.net.URL(endpoint)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val body = """
            {
                "model": "$model",
                "messages": [{"role": "user", "content": "hi"}],
                "max_tokens": 10
            }
        """.trimIndent()

        connection.outputStream.use { it.write(body.toByteArray()) }

        val responseCode = connection.responseCode
        connection.disconnect()

        if (responseCode == 200 || responseCode == 401 || responseCode == 429) {
            TestResult.Success
        } else {
            TestResult.Error("HTTP $responseCode")
        }
    } catch (e: Exception) {
        TestResult.Error(e.message ?: "Unknown error")
    }
}