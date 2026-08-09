package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Chat

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.Route
import io.github.darrindeyoung791.habitpulse.viewmodel.AICreateHabitViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.ChatMessageType
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.activity.compose.BackHandler
import io.github.darrindeyoung791.habitpulse.ui.screens.TimePickerDialog
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHideKeyboardAndNavigateBack
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val clickHandler = rememberDebounceClickHandler()
    val hideKeyboardAndNavigateBack = rememberHideKeyboardAndNavigateBack(navController)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val collectedHabits by viewModel.collectedHabits.collectAsStateWithLifecycle()
    val pendingQuestion by viewModel.pendingQuestion.collectAsStateWithLifecycle()
    val confirmedTempIds by viewModel.confirmedTempIds.collectAsStateWithLifecycle()
    val completedTempIds by viewModel.completedTempIds.collectAsStateWithLifecycle()
    val retrySignal by viewModel.retrySignal.collectAsStateWithLifecycle()
    val showRetryConfirmation by viewModel.showRetryConfirmation.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val hasMessages = messages.isNotEmpty()
    val hasPendingQuestion = pendingQuestion != null

    var initialAnimationComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3 * 80L + 400L)
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
    val activeConfig by userPreferences.activeConfigFlow.collectAsStateWithLifecycle(initialValue = null)

    if (activeConfig?.hasApiKeyConfigured() != true) {
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

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!hasMessages) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(retrySignal) {
        if (retrySignal > 0) {
            delay(300)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        topBar = {
            AnimatedStaggeredItem(
                index = 0,
                applyScale = false,
                initialAnimationComplete = initialAnimationComplete
            ) {
                if (!(isLandscape && imeVisible)) {
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
                                    scope.launch {
                                        clickHandler.processClick {
                                            hideKeyboardAndNavigateBack()
                                        }
                                    }
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
                                context.startActivity(
                                    android.content.Intent(
                                        context,
                                        io.github.darrindeyoung791.habitpulse.AIChatActivity::class.java
                                    )
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = stringResource(R.string.ai_chat_entry)
                                )
                            }
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
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.displayCutout.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal + androidx.compose.foundation.layout.WindowInsetsSides.Bottom))
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
                                ChatMessageType.AI -> {
                                    AIChatBubble(
                                        text = message.text,
                                        thoughts = message.thoughts,
                                        isStreaming = message.isStreaming
                                    )
                                }
                                ChatMessageType.QUESTION -> {
                                    AnsweredQuestionCard(
                                        questionPrompt = message.text,
                                        answer = message.answer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                ChatMessageType.HABIT -> {
                                    message.habit?.let { habit ->
                                        HabitCreatedCard(
                                            habit = habit,
                                            isConfirmed = habit.tempId in confirmedTempIds,
                                            isCompleted = habit.tempId in completedTempIds,
                                            onConfirmClick = { viewModel.toggleHabitCompleted(habit.tempId) },
                                            onEditClick = {
                                                scope.launch {
                                                    viewModel.saveHabitAndGetId(habit.tempId) { dbId ->
                                                        if (dbId != null) {
                                                            navController.navigate(Route.EditHabit.createRoute(dbId))
                                                        }
                                                    }
                                                }
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
                                    onAnswer = { answer ->
                                        viewModel.submitAnswer(answer)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        val hasAIMessage = messages.any { it.type == ChatMessageType.AI }
                        if (hasAIMessage && !uiState.isLoading && pendingQuestion == null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.retryLastTurn() },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.webview_retry),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
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
                                    contentDescription = stringResource(R.string.ai_error_scroll_to_bottom)
                                )
                            }
                        }
                    }
                }
            } else {
                AnimatedStaggeredItem(
                    index = 1,
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
                    index = if (hasMessages) 1 else 2,
                    initialAnimationComplete = if (hasMessages) true else initialAnimationComplete
                ) {
                    AIChatInputBox(
                        inputText = uiState.inputText,
                        onTextChange = { viewModel.updateInputText(it) },
                        focusRequester = focusRequester,
                        onSendClick = {
                            scope.launch {
                                val active = userPreferences.getActiveAIConfig()

                                if (active?.hasApiKeyConfigured() != true) {
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

            if (!(isLandscape && imeVisible)) {
                Text(
                    text = stringResource(R.string.ai_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (uiState.showExitConfirmation) {
        val hasUnsavedHabits = collectedHabits.any { it.tempId !in completedTempIds }
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitConfirmation() },
            title = { Text(stringResource(R.string.ai_exit_title)) },
            text = {
                Text(
                    if (hasUnsavedHabits) stringResource(R.string.ai_exit_unsaved_message)
                    else stringResource(R.string.ai_exit_message)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cleanupUnconfirmedHabits()
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
        val hasUnsavedHabits = collectedHabits.any { it.tempId !in completedTempIds }
        AlertDialog(
            onDismissRequest = { viewModel.dismissSettingsConfirmation() },
            title = { Text(stringResource(R.string.ai_exit_title)) },
            text = {
                Text(
                    if (hasUnsavedHabits) stringResource(R.string.ai_exit_unsaved_message)
                    else stringResource(R.string.ai_exit_message)
                )
            },
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

    if (showRetryConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRetryConfirmation() },
            title = { Text(stringResource(R.string.ai_retry_confirm_title)) },
            text = { Text(stringResource(R.string.ai_retry_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRetry() }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRetryConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        if (thoughts.isNotEmpty()) {
            ThinkingBlock(
                thoughts = thoughts,
                isLoading = isStreaming
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
                        Toast.makeText(context, R.string.ai_chat_copied, Toast.LENGTH_SHORT).show()
                    },
                    onClick = {}
                )
            )
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
    focusRequester: FocusRequester,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    isLoading: Boolean,
    placeholderRes: Int = R.string.ai_input_placeholder,
    containerCornerRadius: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(containerCornerRadius)
    // 发送/停止按钮：尺寸更小，贴右下角；按钮圆角 + 内边距 = 输入框外圆角，
    // 使按钮像「嵌」在圆角里，观感更现代。
    val buttonSize = 32.dp
    val buttonCornerRadius = 10.dp
    val cornerInset = (containerCornerRadius - buttonCornerRadius).coerceAtLeast(8.dp)

    // 默认约 3 行高；随输入增长最多约半屏，之后在输入框内部上下滚动。
    // 横屏点击输入框时由系统 IME 进入原生全屏编辑（不在此手搓）。
    val textStyle = MaterialTheme.typography.bodyLarge
    // 用当前窗口实际高度计算半屏上限，避免 configuration.screenHeightDp
    // 在 configChanges(orientation) 下未重建 Activity 时返回旧方向的屏高。
    // containerDpSize 排除系统栏内边距，随窗口尺寸变化自动重组。
    val configuration = LocalConfiguration.current
    val windowHeightDp = run {
        val windowHeight = LocalWindowInfo.current.containerDpSize.height
        if (windowHeight != Dp.Unspecified && windowHeight > 0.dp) windowHeight
        else configuration.screenHeightDp.dp
    }
    val maxInputHeightDp = (windowHeightDp * 0.5f)
    // 用约 24dp 行高估算最大行数；仅作软上限，实际高度由下方 heightIn(max) 封顶
    val maxLines = (((maxInputHeightDp.value - 40.dp.value) / 24.dp.value).toInt()).coerceAtLeast(3)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = containerShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = inputText,
                onValueChange = onTextChange,
                minLines = 3,
                maxLines = maxLines,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxInputHeightDp)
                    .padding(
                        start = 16.dp,
                        end = buttonSize + 10.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    )
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                textStyle = textStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                enabled = !isLoading,
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = stringResource(placeholderRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isLoading) {
                val stopInteractionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = stopInteractionSource)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = cornerInset, bottom = cornerInset)
                        .size(buttonSize)
                        .clip(RoundedCornerShape(buttonCornerRadius))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = stopInteractionSource,
                            indication = null,
                            onClick = { onStopClick() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.ai_stop_button),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                val sendEnabled = inputText.isNotBlank()
                val sendInteractionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = sendInteractionSource)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = cornerInset, bottom = cornerInset)
                        .size(buttonSize)
                        .clip(RoundedCornerShape(buttonCornerRadius))
                        .background(
                            if (sendEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                        )
                        .clickable(
                            interactionSource = sendInteractionSource,
                            indication = null,
                            enabled = sendEnabled
                        ) { onSendClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.ai_send_button),
                        tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
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
                "choice", "confirm" -> {
                    var selectedOption by remember { mutableStateOf<String?>(null) }
                    var customInput by remember { mutableStateOf("") }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEach { option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedOption = if (option == selectedOption) null else option
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (option == selectedOption)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.ai_custom_input_placeholder)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val parts = mutableListOf<String>()
                                if (selectedOption != null) parts.add(selectedOption!!)
                                if (customInput.isNotBlank()) parts.add(customInput)
                                onAnswer(parts.joinToString("，"))
                            },
                            enabled = selectedOption != null || customInput.isNotBlank()
                        ) {
                            Text(stringResource(R.string.ai_submit))
                        }
                    }
                }
                "time", "time_of_day" -> {
                    var selectedChipTime by remember { mutableStateOf<String?>(null) }
                    var customInput by remember { mutableStateOf("") }
                    var selectedTimes by remember { mutableStateOf<List<String>>(emptyList()) }
                    var showAITimePicker by remember { mutableStateOf(false) }
                    var currentPickerTime by remember { mutableStateOf(LocalTime.now()) }
                    val context = LocalContext.current

                    Column {
                        if (question.options.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                question.options.forEach { time ->
                                    AssistChip(
                                        onClick = {
                                            if (selectedTimes.isNotEmpty()) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.ai_deselect_times_first),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                selectedChipTime = if (time == selectedChipTime) null else time
                                            }
                                        },
                                        label = { Text(time) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedTimes.isEmpty(),
                            placeholder = { Text(stringResource(R.string.ai_custom_input_placeholder)) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = {
                                if (selectedChipTime != null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.ai_select_time_first),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    currentPickerTime = LocalTime.now()
                                    showAITimePicker = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ai_add_time_button))
                        }

                        if (selectedTimes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(R.string.ai_selected_times_label),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    selectedTimes.forEachIndexed { index, time ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = {
                                                    selectedTimes = selectedTimes.filterIndexed { i, _ -> i != index }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = stringResource(R.string.accessibility_delete_reminder_time, time),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val parts = mutableListOf<String>()
                                if (selectedChipTime != null) parts.add(selectedChipTime!!)
                                if (customInput.isNotBlank()) parts.add(customInput)
                                if (selectedTimes.isNotEmpty()) {
                                    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                                    val formatted = selectedTimes.map { LocalTime.parse(it).format(formatter) }
                                    parts.add(formatted.joinToString("，"))
                                }
                                onAnswer(parts.joinToString("，"))
                            },
                            enabled = selectedChipTime != null || customInput.isNotBlank() || selectedTimes.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.ai_submit))
                        }
                    }

                    if (showAITimePicker) {
                        TimePickerDialog(
                            currentTime = currentPickerTime,
                            onDismissRequest = { showAITimePicker = false },
                            onConfirmRequest = { selectedTime ->
                                val timeString = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                                if (timeString !in selectedTimes) {
                                    selectedTimes = selectedTimes + timeString
                                }
                                showAITimePicker = false
                            }
                        )
                    }
                }
                "day_of_week" -> {
                    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
                    var customInput by remember { mutableStateOf("") }
                    val dayIndices = listOf(6, 0, 1, 2, 3, 4, 5)
                    val dayLabels = listOf(
                        stringResource(R.string.ai_day_sun),
                        stringResource(R.string.ai_day_mon),
                        stringResource(R.string.ai_day_tue),
                        stringResource(R.string.ai_day_wed),
                        stringResource(R.string.ai_day_thu),
                        stringResource(R.string.ai_day_fri),
                        stringResource(R.string.ai_day_sat),
                    )
                    val indexToLabel = mapOf(
                        6 to dayLabels[0], 0 to dayLabels[1], 1 to dayLabels[2],
                        2 to dayLabels[3], 3 to dayLabels[4], 4 to dayLabels[5], 5 to dayLabels[6]
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    Column {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dayIndices.forEachIndexed { displayPos, actualIndex ->
                                FilterChip(
                                    modifier = Modifier.width(48.dp),
                                    selected = actualIndex in selectedIndices,
                                    onClick = {
                                        selectedIndices = if (actualIndex in selectedIndices)
                                            selectedIndices - actualIndex
                                        else
                                            selectedIndices + actualIndex
                                    },
                                    label = {
                                        Text(
                                            text = dayLabels[displayPos],
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.ai_custom_input_placeholder)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val parts = mutableListOf<String>()
                                if (selectedIndices.isNotEmpty()) {
                                    parts.add(selectedIndices.sorted().map { indexToLabel[it]!! }.joinToString("、"))
                                }
                                if (customInput.isNotBlank()) {
                                    parts.add(customInput)
                                }
                                onAnswer(parts.joinToString("，"))
                            },
                            enabled = selectedIndices.isNotEmpty() || customInput.isNotBlank()
                        ) {
                            Text(
                                if (selectedIndices.isNotEmpty())
                                    stringResource(R.string.ai_submit_selection, selectedIndices.size)
                                else
                                    stringResource(R.string.ai_submit)
                            )
                        }
                    }
                }
                "multi_choice" -> {
                    var customInput by remember { mutableStateOf("") }
                    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { index, option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndices = if (index in selectedIndices)
                                            selectedIndices - index
                                        else
                                            selectedIndices + index
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (index in selectedIndices)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.ai_custom_input_placeholder)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val parts = mutableListOf<String>()
                                if (selectedIndices.isNotEmpty()) {
                                    val selected = selectedIndices.mapNotNull { question.options.getOrNull(it) }
                                    parts.add(selected.joinToString("、"))
                                }
                                if (customInput.isNotBlank()) {
                                    parts.add(customInput)
                                }
                                onAnswer(parts.joinToString("，"))
                            },
                            enabled = selectedIndices.isNotEmpty() || customInput.isNotBlank()
                        ) {
                            Text(
                                if (selectedIndices.isNotEmpty())
                                    stringResource(R.string.ai_submit_selection, selectedIndices.size)
                                else
                                    stringResource(R.string.ai_submit)
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
fun AnsweredQuestionCard(
    questionPrompt: String,
    answer: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = questionPrompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = answer,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HabitCreatedCard(
    habit: PartialHabit,
    isConfirmed: Boolean,
    isCompleted: Boolean = false,
    onConfirmClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = isCompleted || isConfirmed

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> MaterialTheme.colorScheme.primaryContainer
                isConfirmed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle
                                  else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = when {
                        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                        isConfirmed -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = habit.toSummaryString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (habit.reminderTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (habit.reminderTimes.size > 1) {
                            stringResource(R.string.habit_card_more_times, habit.reminderTimes.first(), habit.reminderTimes.size - 1)
                        } else {
                            habit.reminderTimes.first()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onConfirmClick,
                    colors = if (isCompleted) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(
                        if (isCompleted) stringResource(R.string.habit_card_menu_undo_create)
                        else stringResource(R.string.habit_card_menu_confirm_create)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
    applyScale: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationDelayMs = index * 80L
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

    val scaleVal by animateFloatAsState(
        targetValue = if (visible || !applyScale) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
            if (applyScale) {
                this.scaleX = scaleVal
                this.scaleY = scaleVal
            }
        }
    ) {
        content()
    }
}