package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.conversation.ChatMessageType
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.AICreateHabitViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICreateHabitScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    application: HabitPulseApplication,
    navController: NavHostController
) {
    val viewModel: AICreateHabitViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val collectedHabits by viewModel.collectedHabits.collectAsStateWithLifecycle()
    val pendingQuestion by viewModel.pendingQuestion.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val hasMessages = messages.isNotEmpty()

    val userPreferences = remember { UserPreferences.getInstance(context) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(uiState.habitsSaved) {
        if (uiState.habitsSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_create_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasMessages) {
                            viewModel.showExitConfirmation()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasMessages) {
                            viewModel.showSettingsConfirmation()
                        } else {
                            onNavigateToSettings()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_ai_title)
                        )
                    }
                    if (uiState.isLoading) {
                        IconButton(onClick = { viewModel.stopGeneration() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.ai_stop_button)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasMessages) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    itemsIndexed(messages) { index, message ->
                        when (message.type) {
                            ChatMessageType.USER -> UserChatBubble(
                                text = message.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            ChatMessageType.AI -> AIChatBubble(
                                text = message.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            ChatMessageType.QUESTION -> {}
                        }
                    }

                    pendingQuestion?.let { question ->
                        item {
                            QuestionComponent(
                                question = question,
                                onAnswer = { answer -> viewModel.submitAnswer(answer) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                AIWelcomeContent(
                    modifier = Modifier.weight(1f),
                    onStartChat = { }
                )
            }

            if (uiState.showClearButton) {
                TextButton(
                    onClick = { viewModel.showClearConfirmation() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ai_clear_conversation))
                }
            }

            AIChatInputBox(
                inputText = uiState.inputText,
                onTextChange = { viewModel.updateInputText(it) },
                onSendClick = {
                    scope.launch {
                        val endpoint = userPreferences.llmApiEndpointFlow.first()
                        val apiKey = userPreferences.llmApiKeyFlow.first()
                        val modelName = userPreferences.llmModelNameFlow.first()

                        if (endpoint.isBlank() || apiKey.isBlank()) {
                            viewModel.updateInputText("")
                            return@launch
                        }

                        viewModel.sendMessage(uiState.inputText)
                        viewModel.updateInputText("")
                    }
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitConfirmation() },
            title = { Text(stringResource(R.string.ai_exit_title)) },
            text = { Text(stringResource(R.string.ai_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissExitConfirmation()
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExitConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearConfirmation() },
            title = { Text(stringResource(R.string.ai_clear_title)) },
            text = { Text(stringResource(R.string.ai_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearConversation()
                    viewModel.dismissClearConfirmation()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showSettingsConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSettingsConfirmation() },
            title = { Text(stringResource(R.string.ai_exit_title)) },
            text = { Text(stringResource(R.string.ai_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSettingsConfirmation()
                    onNavigateToSettings()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSettingsConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showConfirmDialog) {
        ConfirmationDialog(
            habits = collectedHabits,
            onConfirm = { viewModel.confirmAndSaveHabits() },
            onDismiss = { viewModel.dismissConfirmDialog() }
        )
    }

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
        }
    }
}

@Composable
fun UserChatBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val cardColor = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Card(
            modifier = Modifier,
            shape = RoundedCornerShape(32.dp),
            colors = cardColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AIChatBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val cardColor = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = cardColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AIWelcomeContent(
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.ai_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.ai_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AIChatInputBox(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val lineHeight = 24.dp
    val currentLineCount = remember(inputText) {
        inputText.lines().size.coerceAtLeast(1)
    }
    val targetLineCount = when {
        inputText.isEmpty() -> 3
        currentLineCount <= 7 -> currentLineCount
        else -> 7
    }
    val targetHeight = (targetLineCount * lineHeight + 32.dp)
        .coerceIn(3 * lineHeight + 32.dp, 7 * lineHeight + 32.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 3 * lineHeight + 32.dp, max = targetHeight),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ai_input_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (isLoading) {
                    // stop handled elsewhere
                } else if (inputText.isNotBlank()) {
                    onSendClick()
                }
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.ai_send_button),
                tint = if (inputText.isNotBlank() && !isLoading) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun QuestionComponent(
    question: io.github.darrindeyoung791.habitpulse.viewmodel.PendingQuestionUI,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (question.type) {
                "choice" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEach { option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAnswer(option) }
                            ) {
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                "time" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        question.options.forEach { time ->
                            AssistChip(
                                onClick = { onAnswer(time) },
                                label = { Text(time) }
                            )
                        }
                    }
                }
                "day_of_week" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val days = listOf("日", "一", "二", "三", "四", "五", "六")
                        days.forEachIndexed { index, day ->
                            FilterChip(
                                selected = false,
                                onClick = { onAnswer(index.toString()) },
                                label = { Text(day) }
                            )
                        }
                    }
                }
                "text" -> {
                    var textInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.ai_text_input_placeholder)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onAnswer(textInput) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.ai_unsupported_question_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    habits: List<PartialHabit>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.ai_confirm_message, habits.size))
                Spacer(modifier = Modifier.height(12.dp))
                habits.forEachIndexed { index, habit ->
                    Text(
                        text = "${index + 1}. ${habit.toSummaryString()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ai_confirm_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}