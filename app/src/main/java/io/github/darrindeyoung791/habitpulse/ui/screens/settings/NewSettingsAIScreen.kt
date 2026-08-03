package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsAIScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
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

    val defaultModelLabel = stringResource(id = R.string.ai_settings_model_default_label, "glm-4-flash-250414")
    val flagshipModelLabel = stringResource(id = R.string.ai_settings_model_flagship_label, "glm-5.1")

    val presetModels = listOf(
        "glm-4-flash-250414" to defaultModelLabel,
        "glm-4.7-flash" to "glm-4.7-flash",
        "glm-4-flash" to "glm-4-flash",
        "glm-3-flash" to "glm-3-flash",
        "glm-5.1" to flagshipModelLabel
    )

    var modelDropdownExpanded by remember { mutableStateOf(false) }

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_category_ai),
        onBack = onBack,
        onHelp = onOpenHelp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = endpointInput,
            onValueChange = {
                if (it.length <= 200) {
                    endpointInput = it
                    scope.launch { userPreferences.setLlmApiEndpoint(it) }
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
                        scope.launch { userPreferences.setLlmApiEndpoint("") }
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = {
                if (it.length <= 100) {
                    apiKeyInput = it
                    scope.launch { userPreferences.setLlmApiKey(it) }
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
                            scope.launch { userPreferences.setLlmApiKey("") }
                        }) {
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
                onValueChange = {
                    if (it.length <= 50) {
                        modelInput = it
                        scope.launch { userPreferences.setLlmModelName(it) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text(stringResource(id = R.string.ai_settings_model)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
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
                            scope.launch { userPreferences.setLlmModelName(model) }
                            modelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 2,
                headline = stringResource(id = R.string.ai_settings_test_connection),
                supportingText = stringResource(id = R.string.ai_settings_test_connection_description),
                leadingIcon = Icons.AutoMirrored.Outlined.Send,
                showArrow = false,
                enabled = endpointInput.isNotBlank() && apiKeyInput.isNotBlank() && !isTesting,
                onClick = {
                    isTesting = true
                    scope.launch {
                        val result = LLMConfig.ensureChatCompletionsUrl(endpointInput).let { _ ->
                            testApiConnection(context, endpointInput, apiKeyInput, modelInput)
                        }
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
            SettingsSegmentedSwitch(
                index = 1,
                count = 2,
                headline = stringResource(id = R.string.ai_settings_streaming),
                supportingText = stringResource(id = R.string.ai_settings_streaming_description),
                leadingIcon = Icons.Outlined.Subject,
                checked = streamingEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { userPreferences.setLlmStreamingResponse(enabled) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSegmentedGroup {
            SettingsSegmentedItem(
                index = 0,
                count = 1,
                headline = stringResource(id = R.string.settings_memory),
                supportingText = stringResource(id = R.string.settings_memory_description),
                enabled = false,
                showArrow = false,
                leadingIcon = Icons.Outlined.Memory,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.ai_settings_notice_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.ai_settings_notice_content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
    }
}

private sealed class TestResult(val isSuccess: Boolean, val error: String? = null) {
    object Success : TestResult(true)
    data class Error(val message: String) : TestResult(false, message)
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
                            BufferedReader(InputStreamReader(errorInput, Charsets.UTF_8)).use { it.readText() }
                        } ?: ""
                    } catch (_: Exception) { "" }
                    TestResult.Error(extractErrorMessage(errorBody) ?: "HTTP $responseCode")
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
