package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.Route
import io.github.darrindeyoung791.habitpulse.viewmodel.AICreateHabitViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.AIPrefillHabitHolder
import io.github.darrindeyoung791.habitpulse.viewmodel.ChatMessageType
import io.github.darrindeyoung791.habitpulse.viewmodel.PendingQuestionUI
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoApiKeyPrompt(
    onGoToSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.ai_api_key_required_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ai_api_key_required_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onGoToSettings) {
                Text(stringResource(R.string.ai_api_key_required_action))
            }
        }
    }
}

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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val collectedHabits by viewModel.collectedHabits.collectAsStateWithLifecycle()
    val pendingQuestion by viewModel.pendingQuestion.collectAsStateWithLifecycle()
    val confirmedTempIds by viewModel.confirmedTempIds.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val hasMessages = messages.isNotEmpty()
    val hasPendingQuestion = pendingQuestion != null

    var initialAnimationComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2 * 120L + 400L)
        initialAnimationComplete = true
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 2
        }
    }

    val userPreferences = remember { UserPreferences.getInstance(context) }

    var apiKeyConfigured by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val apiKey = userPreferences.llmApiKeyFlow.first()
        apiKeyConfigured = apiKey.isNotBlank()
    }

    if (apiKeyConfigured == false) {
        NoApiKeyPrompt(
            onGoToSettings = onNavigateToSettings,
            onNavigateBack = onNavigateBack
        )
        return
    }

    BackHandler(enabled = true) {
        if (hasMessages) {
            viewModel.showExitConfirmation()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(pendingQuestion) {
        if (pendingQuestion != null) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isLoading) stringResource(R.string.ai_streaming_title)
                        else stringResource(R.string.ai_create_title)
                    )
                },
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
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            if (hasMessages) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                    thoughts = message.thoughts,
                                    isStreaming = message.isStreaming
                                )
                                ChatMessageType.QUESTION -> {}
                                ChatMessageType.HABIT -> {
                                    message.habit?.let { habit ->
                                        HabitCreatedCard(
                                            habit = habit,
                                            isConfirmed = habit.tempId in confirmedTempIds,
                                            onConfirmClick = { viewModel.confirmHabit(habit.tempId) },
                                            onEditClick = {
                                                AIPrefillHabitHolder.prefillHabit = habit
                                                AIPrefillHabitHolder.editingHabitDbId = viewModel.getDbIdForTempId(habit.tempId)
                                                navController.navigate(Route.CreateHabit.route)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
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

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp)
                    ) {
                        AnimatedVisibility(visible = !isAtBottom && hasMessages) {
                            SmallFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "滚动到底部"
                                )
                            }
                        }
                    }
                }
            } else {
                AnimatedStaggeredItem(
                    index = 0,
                    initialAnimationComplete = initialAnimationComplete,
                    modifier = Modifier.weight(1f)
                ) {
                    AIWelcomeContent(
                        modifier = Modifier.fillMaxSize(),
                        onStartChat = { }
                    )
                }
            }

            if (uiState.showClearButton && !hasPendingQuestion) {
                TextButton(
                    onClick = { viewModel.showClearConfirmation() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ai_clear_conversation))
                }
            }

            if (!hasPendingQuestion) {
                AnimatedStaggeredItem(
                    index = if (hasMessages) 0 else 1,
                    initialAnimationComplete = if (hasMessages) true else initialAnimationComplete
                ) {
                    AIChatInputBox(
                        inputText = uiState.inputText,
                        onTextChange = { viewModel.updateInputText(it) },
                        onSendClick = {
                            scope.launch {
                                val apiKey = userPreferences.llmApiKeyFlow.first()

                                if (apiKey.isBlank()) {
                                    viewModel.updateInputText("")
                                    return@launch
                                }

                                viewModel.sendMessage(uiState.inputText)
                                viewModel.updateInputText("")
                            }
                        },
                        onStopClick = { viewModel.stopGeneration() },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
            shape = RoundedCornerShape(16.dp),
            colors = cardColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AIChatBubble(
    text: String,
    thoughts: String = "",
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        if (thoughts.isNotEmpty()) {
            ThinkingBlock(
                thoughts = thoughts,
                isStreaming = isStreaming
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ThinkingBlock(
    thoughts: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStreaming) stringResource(R.string.thinking_in_progress) else stringResource(R.string.view_thinking),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = borderColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thoughts,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    onStopClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .heightIn(max = 168.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    enabled = !isLoading,
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
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isLoading) {
                    IconButton(
                        onClick = onStopClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.ai_stop_button),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendClick()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.ai_send_button),
                            tint = if (inputText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
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
fun HabitCreatedCard(
    habit: PartialHabit,
    isConfirmed: Boolean,
    onConfirmClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isConfirmed)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isConfirmed)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConfirmed)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = habit.toSummaryString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConfirmed)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            if (habit.reminderTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏰ ${habit.reminderTimes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConfirmed)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isConfirmed) {
                    OutlinedButton(
                        onClick = onConfirmClick
                    ) {
                        Text(stringResource(R.string.habit_card_menu_complete))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(
                    onClick = onEditClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.habit_card_menu_edit))
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

@Composable
private fun AnimatedStaggeredItem(
    index: Int,
    initialAnimationComplete: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationDelayMs = index * 120L
    val shouldAnimate = !initialAnimationComplete
    var visible by remember { mutableStateOf(!shouldAnimate) }

    LaunchedEffect(Unit) {
        if (shouldAnimate) {
            delay(animationDelayMs)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "alpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "translationY"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}