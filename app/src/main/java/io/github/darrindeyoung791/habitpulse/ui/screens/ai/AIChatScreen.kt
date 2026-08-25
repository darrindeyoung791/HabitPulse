package io.github.darrindeyoung791.habitpulse.ui.screens.ai

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.SettingsAIActivity
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ai.conversation.PartialHabit
import io.github.darrindeyoung791.habitpulse.ai.tools.PendingQuestionData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ControllableSetting
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitBrief
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitDeleteData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitEditData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.HabitSearchData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SessionUsage
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingChangeData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingPair
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingsNavData
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.SettingsStatusData
import io.github.darrindeyoung791.habitpulse.navigation.getDeviceCornerRadius
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsIconChip
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.rememberAccentTint
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedSwitch
import io.github.darrindeyoung791.habitpulse.ui.theme.AccentSeeds
import io.github.darrindeyoung791.habitpulse.ui.theme.rememberSeedAccentTint
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHapticsEnabled
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberPressVibrationParams
import io.github.darrindeyoung791.habitpulse.ui.utils.vibrateShort
import io.github.darrindeyoung791.habitpulse.viewmodel.AIChatViewModel
import io.github.darrindeyoung791.habitpulse.viewmodel.AiChatUiMessage
import io.github.darrindeyoung791.habitpulse.viewmodel.AiChatUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 新版 AI 对话界面：TopAppBar + 消息列表 + 底部输入栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCollapse: () -> Unit = {},
    onEditHabit: (UUID) -> Unit,
    application: HabitPulseApplication,
    // Omnibox 形变交接：展开起点带入的草稿文本
    initialInputText: String = "",
    // 显着的手动创建习惯入口（顶栏图标 + 欢迎区卡片均触发）
    onManualCreateHabit: () -> Unit = {},
    // 可选：外部传入的形变进度，用于 TopAppBar 反向拖拽收起
    progress: Animatable<Float, AnimationVector1D>? = null
) {
    val viewModel: AIChatViewModel = viewModel()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var inputText by rememberSaveable { mutableStateOf(initialInputText) }

    var showLeaveDialog by remember { mutableStateOf(false) }

    val isLandscape = rememberDeviceFormInfo().isLandscape
    val density = LocalDensity.current
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0
    val deviceCornerRadius = getDeviceCornerRadius()

    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 完全展开后自动聚焦输入框并弹出键盘
    LaunchedEffect(progress?.value) {
        if (progress == null || (progress.value >= 0.999f)) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    // 横屏时系统栏/摄像头在屏幕侧边，整个界面水平方向都要留出安全区。
    // 注意：Compose 的 displayCutout 在部分设备（本模拟器即如此）上报 0，
    // 必须改用 safeDrawing（systemBars + displayCutout 的并集）才能拿到横向 inset。
    val chatHorizontalInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)

    // AI 输出开始/结束时各震动一次（受全局震动开关与参数控制）
    val hapticsEnabled = rememberHapticsEnabled()
    val (vibrationDuration, vibrationAmplitude) = rememberPressVibrationParams()
    var prevGenerating by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(uiState.isGenerating) {
        val prev = prevGenerating
        prevGenerating = uiState.isGenerating
        if (prev != null && prev != uiState.isGenerating) {
            if (hapticsEnabled) {
                vibrateShort(context, vibrationDuration, vibrationAmplitude)
            }
        }
    }

    // 是否锁定跟随底部：AI 输出时自动滚到底部；用户向上滚动后暂停跟随，滚回底部后恢复。
    var followBottom by remember { mutableStateOf(true) }

    // 消息列表每次变化（新增消息或流式内容增长）时，若仍锁定底部则跳到真正的最底部。
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && followBottom) {
            listState.scrollToItem(
                (messages.size - 1).coerceAtLeast(0),
                scrollOffset = Int.MAX_VALUE
            )
        }
    }

    // 监听用户滚动方向：向上滚离底部则停止跟随；滚回底部（无法再向下滚动）则恢复跟随。
    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }.collect { (inProgress, index, offset) ->
            if (inProgress) {
                val movedUp = index < lastIndex || (index == lastIndex && offset < lastOffset)
                if (movedUp) followBottom = false
            } else if (!listState.canScrollForward) {
                followBottom = true
            }
            lastIndex = index
            lastOffset = offset
        }
    }

    BackHandler(enabled = true) {
        if (uiState.isGenerating) {
            showLeaveDialog = true
        } else {
            onNavigateBack()
        }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                var showMenu by remember { mutableStateOf(false) }
                TopAppBar(
                    modifier = Modifier.windowInsetsPadding(chatHorizontalInsets),
                    title = {
                        Text(
                            text = if (uiState.isGenerating) stringResource(R.string.ai_streaming_title)
                            else stringResource(R.string.ai_chat_title)
                        )
                    },
                navigationIcon = {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    PressVibrationFeedback(interactionSource = backInteractionSource)
                    IconButton(
                        onClick = {
                            if (uiState.isGenerating) showLeaveDialog = true else onCollapse()
                        },
                        interactionSource = backInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                actions = {
                    val newChatInteractionSource = remember { MutableInteractionSource() }
                    PressVibrationFeedback(interactionSource = newChatInteractionSource)
                    IconButton(
                        onClick = { viewModel.clearConversation() },
                        enabled = !uiState.isGenerating,
                        interactionSource = newChatInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddComment,
                            contentDescription = stringResource(R.string.ai_chat_new_conversation)
                        )
                    }
                    val menuInteractionSource = remember { MutableInteractionSource() }
                    PressVibrationFeedback(interactionSource = menuInteractionSource)
                    IconButton(
                        onClick = { showMenu = true },
                        interactionSource = menuInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.settings_ai_title)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (usage.completedCalls > 0) {
                                    stringResource(R.string.ai_chat_token_count, formatTokenCount(usage.totalTokens))
                                } else {
                                    stringResource(R.string.ai_chat_usage_empty)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = { showMenu = false },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_ai_title)) },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    android.content.Intent(context, SettingsAIActivity::class.java)
                                )
                            },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                    .windowInsetsPadding(chatHorizontalInsets)
                    .imePadding()
            ) {
                if (!(isLandscape && imeVisible)) {
                    Text(
                        text = stringResource(R.string.ai_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
                AIChatInputBox(
                    inputText = inputText,
                    onTextChange = { inputText = it },
                    focusRequester = inputFocusRequester,
                    onSendClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            inputText = ""
                            viewModel.sendMessage(text)
                        }
                    },
                    onStopClick = { viewModel.stopGeneration() },
                    isLoading = uiState.isGenerating,
                    placeholderRes = R.string.ai_chat_input_hint,
                    containerCornerRadius = deviceCornerRadius
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(chatHorizontalInsets)
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (messages.isEmpty()) {
                    item(key = "welcome") {
                        AIWelcomeContent(
                            onSuggestionClick = { suggestion ->
                                viewModel.sendMessage(suggestion)
                            },
                            onManualCreateHabit = onManualCreateHabit
                        )
                    }
                }
                items(messages, key = { messageKey(it) }) { message ->
                    ChatMessageItem(
                        message = message,
                        viewModel = viewModel,
                        onEditHabit = onEditHabit,
                        application = application
                    )
                }
                if (messages.isNotEmpty() && uiState.showRetry) {
                    item(key = "retry") {
                        RetryRow(
                            onRetry = { viewModel.retryLastTurn() }
                        )
                    }
                }
            }
            // FAB 节点保持常驻（有消息时始终组合），仅通过 AnimatedVisibility 显隐。
            // 若用裸 if 条件组合，无障碍节点会随 followBottom 突变而频繁增删：
            // 滚到底部时 followBottom 翻转为 true（canScrollForward 为 false 的瞬间），
            // 恰好是无障碍焦点到达该按钮的时刻，导致 TalkBack 无法聚焦。
            if (messages.isNotEmpty()) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = fabInteractionSource)
                AnimatedVisibility(
                    visible = !followBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    FloatingActionButton(
                        onClick = {
                            followBottom = true
                            scope.launch {
                                listState.scrollToItem(
                                    (messages.size - 1).coerceAtLeast(0),
                                    scrollOffset = Int.MAX_VALUE
                                )
                            }
                        },
                        interactionSource = fabInteractionSource,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.ai_error_scroll_to_bottom)
                        )
                }
            }
        }
    }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.ai_chat_confirm_leave_title)) },
            text = {
                Text(stringResource(R.string.ai_chat_confirm_leave_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun formatTokenCount(value: Long): String =
    String.format(java.util.Locale.getDefault(), "%,d", value)

/** 终止后出现在回答下方的重试行。 */
@Composable
private fun RetryRow(
    onRetry: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onRetry,
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_chat_retry))
        }
    }
}

/** 单条消息的渲染分派。 */
private fun messageKey(message: AiChatUiMessage): Any = when (message) {
    is AiChatUiMessage.HabitPickerCard -> message.cardId
    else -> System.identityHashCode(message)
}

@Composable
private fun ChatMessageItem(
    message: AiChatUiMessage,
    viewModel: AIChatViewModel,
    onEditHabit: (UUID) -> Unit,
    application: HabitPulseApplication
) {
    when (message) {
        is AiChatUiMessage.UserBubble -> UserChatBubble(message.text)
        is AiChatUiMessage.AssistantBubble -> AIChatBubble(
            text = message.text,
            thoughts = message.thoughts,
            isStreaming = message.isStreaming
        )
        is AiChatUiMessage.QuestionCard -> ChatQuestionCard(
            question = message.question,
            onAnswer = viewModel::submitAnswer
        )
        is AiChatUiMessage.CreatedHabitCard -> HabitCreatedCard(
            payload = message.payload,
            confirmed = message.confirmed,
            onConfirm = { viewModel.confirmCreateHabit(message.payload) },
            onEdit = {
                viewModel.editCreateHabit(message.payload) { dbId ->
                    onEditHabit(dbId)
                }
            },
            onDelete = { viewModel.deleteCreateHabit(message.payload) }
        )
        is AiChatUiMessage.HabitPickerCard -> HabitPickerCard(
            data = message.data,
            onSearch = { keyword ->
                viewModel.searchHabitsInPicker(message.cardId, keyword)
            },
            onSubmit = { selected ->
                viewModel.submitPickerSelection(message.cardId, selected)
            },
            onManualDone = {
                viewModel.manualPickerDone(message.cardId)
            },
            onEdit = { id ->
                onEditHabit(UUID.fromString(id))
            },
            onDelete = { brief ->
                viewModel.deleteHabitById(brief.id, brief.title)
            }
        )
        is AiChatUiMessage.HabitEditCard -> HabitEditCard(
            data = message.data,
            onOpen = {
                onEditHabit(UUID.fromString(message.data.habitId))
            }
        )
        is AiChatUiMessage.HabitDeleteCard -> HabitDeleteCard(
            data = message.data,
            onConfirm = { viewModel.confirmDeleteHabit(message.data) },
            onCancel = { viewModel.cancelDeleteHabit(message.data) }
        )
        is AiChatUiMessage.HabitDeletedConfirmed -> StatusChip(
            stringResource(R.string.ai_chat_habit_deleted),
            message.data
        )
        is AiChatUiMessage.HabitDeleteCancelled -> StatusChip(
            stringResource(R.string.ai_chat_delete_cancelled),
            message.data
        )
        is AiChatUiMessage.SettingsStatusCard -> SettingStatusCard(
            data = message.data,
            onNavigateToSettings = { page -> navigateToSettingsPage(application, page) }
        )
        is AiChatUiMessage.SettingChangeCard -> SettingChangeCard(
            data = message.data,
            onToggle = { newValue -> viewModel.toggleSetting(message.data.key, newValue) },
            onModeSelected = { intValue -> viewModel.setDarkModeSetting(intValue) }
        )
        is AiChatUiMessage.SettingReverted -> SettingRevertedChip(message.data)
        is AiChatUiMessage.SettingsNavCard -> SettingsNavCard(
            data = message.data,
            onOpen = { openSettingsPage(application, message.data) }
        )
        is AiChatUiMessage.ToolError -> AIChatBubble(
            text = "${stringResource(R.string.ai_error_prefix, message.message)}"
        )
        is AiChatUiMessage.SystemError -> AIChatBubble(
            text = "${stringResource(R.string.ai_error_prefix, message.message)}"
        )
    }
}

@Composable
private fun UserChatBubble(text: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(context, R.string.ai_chat_copied, Toast.LENGTH_SHORT).show()
                },
                onClick = {}
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** 空会话欢迎态：居中图标 + 问候语 + 示例提问 chips（紧凑流式排布）+ 手动创建习惯入口卡。 */
@Composable
private fun AIWelcomeContent(
    onSuggestionClick: (String) -> Unit,
    onManualCreateHabit: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ai_chat_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.ai_chat_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 显著的手动建立习惯入口
        Surface(
            onClick = onManualCreateHabit,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.ai_chat_manual_create),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.ai_chat_manual_create_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val suggestions = listOf(
            stringResource(R.string.ai_chat_suggestion_create),
            stringResource(R.string.ai_chat_suggestion_search),
            stringResource(R.string.ai_chat_suggestion_setting)
        )
        // 紧凑流式排布：横向排列、间距收紧（替代原先逐个竖排的松散布局）
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                val chipInteractionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = chipInteractionSource)
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    interactionSource = chipInteractionSource,
                    label = { Text(suggestion) }
                )
            }
        }
    }
}

/** 新建习惯卡：标题摘要 + 确认 / 编辑 / 删除（确认后卡片进入已确认态）。 */
@Composable
private fun HabitCreatedCard(
    payload: io.github.darrindeyoung791.habitpulse.ai.tools.chat.CreateHabitPayload,
    confirmed: Boolean,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (confirmed) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (confirmed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.habit.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = payload.habit.toSummaryString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (payload.truncated) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ai_error_invalid_url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (confirmed) {
                    Text(
                        text = stringResource(R.string.ai_chat_habit_created),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 4.dp)
                    )
                } else {
                    HapticButton(text = stringResource(R.string.confirm), onClick = onConfirm)
                }
                HapticOutlinedButton(
                    text = stringResource(R.string.ai_chat_edit_habit),
                    onClick = onEdit
                )
                HapticTextButton(
                    text = stringResource(R.string.ai_chat_delete),
                    onClick = onDelete,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** 通用结果状态 chip（已创建/已取消/已删除…）。 */
@Composable
private fun StatusChip(label: String, habit: PartialHabit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label：${habit.title}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, data: HabitDeleteData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label：${data.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * 既有习惯选择卡：
 * - 顶部搜索框（模糊搜索全部习惯，空关键字回到全部）；
 * - 每行可多选（选中后高亮），保留行内编辑 / 删除（删除为行内二次确认）；
 * - 底部「提交」把选中的习惯回给 AI；「我已手动操作」无需选择直接继续会话。
 */
@Composable
private fun HabitPickerCard(
    data: HabitSearchData,
    onSearch: (String) -> Unit,
    onSubmit: (List<HabitBrief>) -> Unit,
    onManualDone: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (HabitBrief) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(searchQuery) {
        delay(300)
        onSearch(searchQuery)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.ai_chat_search_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(stringResource(R.string.ai_chat_picker_search))
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        HapticIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = stringResource(R.string.accessibility_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { searchQuery = "" }
                        )
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (data.habits.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_chat_picker_no_result),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            data.habits.forEach { brief ->
                val selected = brief.id in selectedIds
                if (pendingDeleteId == brief.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_chat_delete_confirm_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = brief.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { pendingDeleteId = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = {
                            pendingDeleteId = null
                            onDelete(brief)
                        }) {
                            Text(
                                text = stringResource(R.string.habit_card_delete_confirm),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable {
                                if (selected) selectedIds.remove(brief.id)
                                else selectedIds.add(brief.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(brief.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = brief.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HapticIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.ai_chat_edit_habit),
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onEdit(brief.id) }
                        )
                        HapticIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.habit_card_menu_delete),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { pendingDeleteId = brief.id }
                        )
                    }
                }
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HapticButton(
                    text = stringResource(R.string.ai_chat_picker_submit),
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val selected = data.habits.filter { it.id in selectedIds }
                        selectedIds.clear()
                        onSubmit(selected)
                    }
                )
                HapticOutlinedButton(
                    text = stringResource(R.string.ai_chat_picker_manual_done),
                    modifier = Modifier.weight(1f),
                    onClick = onManualDone
                )
            }
        }
    }
}

/** 带按压触感的小号图标按钮（受应用内震动开关控制）。 */
@Composable
private fun HapticIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 带按压触感的文本按钮（受应用内震动开关控制）。 */
@Composable
private fun HapticTextButton(
    text: String,
    onClick: () -> Unit,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
    ) {
        Text(text, color = color ?: Color.Unspecified)
    }
}

/** 带按压触感的主按钮（受应用内震动开关控制）。 */
@Composable
private fun HapticButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text)
    }
}

/** 带按压触感的描边按钮（受应用内震动开关控制）。 */
@Composable
private fun HapticOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text)
    }
}

/** 编辑跳转卡。 */
@Composable
private fun HabitEditCard(
    data: HabitEditData,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            HapticButton(
                text = stringResource(R.string.ai_chat_edit_habit),
                onClick = onOpen
            )
        }
    }
}

/** 删除确认卡。 */
@Composable
private fun HabitDeleteCard(
    data: HabitDeleteData,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ai_chat_delete_confirm_prompt),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val interactionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = interactionSource)
                Button(
                    onClick = onConfirm,
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.confirm))
                }
                HapticTextButton(text = stringResource(R.string.cancel), onClick = onCancel)
            }
        }
    }
}

/** 设置状态卡：每项可点击跳转对应设置页，显示当前值文字（非开关）。 */
@Composable
private fun SettingStatusCard(
    data: SettingsStatusData,
    onNavigateToSettings: (String) -> Unit
) {
    SettingsSegmentedGroup(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        data.pairs.forEachIndexed { index, pair ->
            SettingItemRow(
                index = index,
                count = data.pairs.size,
                pair = pair,
                onClick = { pair.navPage?.let(onNavigateToSettings) }
            )
        }
    }
}

@Composable
private fun SettingItemRow(
    index: Int,
    count: Int,
    pair: SettingPair,
    onClick: () -> Unit
) {
    val tint = rememberAccentTint(index)
    val interactionSource = remember { MutableInteractionSource() }

    SettingsListSurface(
        index = index,
        count = count,
        enabled = true,
        onClick = onClick,
        interactionSource = interactionSource,
        leading = {
            SettingsIconChip(
                icon = settingIconFor(pair.key),
                tint = tint,
                twoLine = false,
                enabled = true
            )
        },
        headline = stringResource(pair.labelRes),
        supportingText = pair.displayValue,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun settingIconFor(key: String): ImageVector = when (ControllableSetting.byKey(key)) {
    ControllableSetting.REMINDER_ENABLED -> Icons.Outlined.Alarm
    ControllableSetting.DND_ENABLED -> Icons.Outlined.Bedtime
    ControllableSetting.PERSISTENT_NOTIFICATION -> Icons.Outlined.Notifications
    ControllableSetting.HAPTIC_FEEDBACK_ENABLED -> Icons.Outlined.Vibration
    ControllableSetting.SHOW_SPLASH_AD -> Icons.Outlined.Slideshow
    ControllableSetting.FORCE_TABLET_LANDSCAPE -> Icons.Outlined.Tablet
    ControllableSetting.DARK_MODE -> Icons.Outlined.DarkMode
    else -> Icons.Outlined.Notifications
}

/** 设置变更卡：布尔设置用 Switch，暗色模式用下拉菜单，用户可直接点击切换。 */
@Composable
private fun SettingChangeCard(
    data: SettingChangeData,
    onToggle: (Boolean) -> Unit,
    onModeSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = settingIconFor(data.key),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(data.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (data.newIntValue != null) {
                DarkModeDropdown(
                    currentValue = data.newIntValue,
                    onModeSelected = onModeSelected
                )
            } else {
                val interactionSource = remember { MutableInteractionSource() }
                PressVibrationFeedback(interactionSource = interactionSource)
                Switch(
                    checked = data.newValue,
                    onCheckedChange = onToggle,
                    interactionSource = interactionSource,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }
    }
}

@Composable
private fun DarkModeDropdown(
    currentValue: Int,
    onModeSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (currentValue) {
        1 -> stringResource(R.string.ai_setting_dark_mode_dark)
        2 -> stringResource(R.string.ai_setting_dark_mode_light)
        else -> stringResource(R.string.ai_setting_dark_mode_follow_system)
    }
    Box {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_setting_dark_mode_light)) },
                onClick = { onModeSelected(2); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_setting_dark_mode_dark)) },
                onClick = { onModeSelected(1); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_setting_dark_mode_follow_system)) },
                onClick = { onModeSelected(0); expanded = false }
            )
        }
    }
}

@Composable
private fun onOffLabel(value: Boolean): String =
    stringResource(if (value) R.string.ai_chat_on else R.string.ai_chat_off)

@Composable
private fun SettingRevertedChip(data: SettingChangeData) {
    StatusChip(stringResource(R.string.ai_chat_setting_reverted), data.labelRes)
}

@Composable
private fun StatusChip(label: String, labelRes: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label：${stringResource(labelRes)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** 设置导航卡：种子色图标 + 本地化子页名 + 打开按钮。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsNavCard(
    data: SettingsNavData,
    onOpen: () -> Unit
) {
    val label = stringResource(settingsPageLabelRes(data.page))
    val seed = AccentSeeds[settingsPageSeedIndex(data.page) % AccentSeeds.size]
    val tint = rememberSeedAccentTint(seed)
    val icon = settingsPageIcon(data.page)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = tint.container,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint.content,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.ai_chat_open_settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HapticTextButton(
                text = stringResource(R.string.ai_chat_open_settings_format, label),
                onClick = onOpen
            )
        }
    }
}

private fun settingsPageLabelRes(page: String): Int = when (page) {
    "notifications" -> R.string.settings_notifications
    "general" -> R.string.settings_category_general
    "about" -> R.string.settings_about
    "ai" -> R.string.settings_ai_title
    else -> R.string.settings_title
}

private fun settingsPageIcon(page: String): ImageVector = when (page) {
    "notifications" -> Icons.Outlined.Notifications
    "general" -> Icons.Outlined.Settings
    "about" -> Icons.Outlined.Info
    "ai" -> Icons.Outlined.AutoAwesome
    else -> Icons.Outlined.Settings
}

private fun settingsPageSeedIndex(page: String): Int = when (page) {
    "notifications" -> 0
    "general" -> 1
    "about" -> 2
    "ai" -> 3
    else -> 4
}

private fun openSettingsPage(
    application: HabitPulseApplication,
    data: SettingsNavData
) {
    navigateToSettingsPage(application, data.page)
}

private fun navigateToSettingsPage(application: HabitPulseApplication, page: String) {
    val intent = when (page) {
        "notifications" -> android.content.Intent(
            application,
            io.github.darrindeyoung791.habitpulse.SettingsNotificationsActivity::class.java
        )
        "general" -> android.content.Intent(
            application,
            io.github.darrindeyoung791.habitpulse.SettingsGeneralActivity::class.java
        )
        "about" -> android.content.Intent(
            application,
            io.github.darrindeyoung791.habitpulse.SettingsAboutActivity::class.java
        )
        "ai" -> android.content.Intent(application, SettingsAIActivity::class.java)
        else -> android.content.Intent(
            application,
            io.github.darrindeyoung791.habitpulse.SettingsActivity::class.java
        )
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    application.startActivity(intent)
}

/** 提问卡：复用旧 QuestionComponent 的交互语义。 */
/** 提问选项卡（带按压触感）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionOptionCard(
    option: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatQuestionCard(
    question: PendingQuestionData,
    onAnswer: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (question.type == "text") {
                QuestionManualTab(onAnswer = onAnswer)
            } else {
                var activeTab by remember { mutableStateOf(0) }
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(stringResource(R.string.ai_chat_option_tab)) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(stringResource(R.string.ai_chat_manual_tab)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (activeTab == 0) {
                    QuestionOptionsTab(question = question, onAnswer = onAnswer)
                } else {
                    QuestionManualTab(onAnswer = onAnswer)
                }
            }
        }
    }
}

/** 选项页签：按问题类型渲染结构化选择，自带唯一提交按钮。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionOptionsTab(
    question: PendingQuestionData,
    onAnswer: (String) -> Unit
) {
    when (question.type) {
        "choice", "confirm" -> {
            var selectedOption by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEach { option ->
                    QuestionOptionCard(
                        option = option,
                        selected = option == selectedOption,
                        onClick = {
                            selectedOption = if (option == selectedOption) null else option
                        }
                    )
                }
                HapticButton(
                    text = stringResource(R.string.ai_submit),
                    onClick = { onAnswer(selectedOption!!) },
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        "time", "time_of_day" -> {
            var selectedTimes by remember { mutableStateOf<List<String>>(emptyList()) }
            var showTimePicker by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEach { time ->
                    val chipInteractionSource = remember { MutableInteractionSource() }
                    PressVibrationFeedback(interactionSource = chipInteractionSource)
                    AssistChip(
                        onClick = {
                            selectedTimes = if (time in selectedTimes)
                                selectedTimes - time
                            else
                                selectedTimes + time
                        },
                        interactionSource = chipInteractionSource,
                        label = { Text(time) }
                    )
                }
                if (selectedTimes.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedTimes.sorted().forEach { time ->
                            val removeInteractionSource = remember { MutableInteractionSource() }
                            PressVibrationFeedback(interactionSource = removeInteractionSource)
                            InputChip(
                                selected = true,
                                onClick = { selectedTimes = selectedTimes - time },
                                interactionSource = removeInteractionSource,
                                label = { Text(time) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.ai_chat_remove_time),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                HapticOutlinedButton(
                    text = stringResource(R.string.ai_chat_add_time),
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
                HapticButton(
                    text = stringResource(R.string.ai_submit),
                    onClick = { onAnswer(selectedTimes.sorted().joinToString("，")) },
                    enabled = selectedTimes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTimePicker) {
                    TimePickerDialogWithTimes(
                        initialTimes = selectedTimes,
                        onConfirm = { newTimes ->
                            selectedTimes = newTimes.sorted()
                            showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false }
                    )
                }
            }
        }
        "day_of_week" -> {
            val labels = listOf(
                R.string.ai_day_mon, R.string.ai_day_tue, R.string.ai_day_wed,
                R.string.ai_day_thu, R.string.ai_day_fri, R.string.ai_day_sat,
                R.string.ai_day_sun
            )
            var selectedDays by remember { mutableStateOf<List<Int>>(emptyList()) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { index, res ->
                        val dayInteractionSource = remember { MutableInteractionSource() }
                        PressVibrationFeedback(interactionSource = dayInteractionSource)
                        val isSelected = index in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (index in selectedDays)
                                    selectedDays - index
                                else
                                    selectedDays + index
                            },
                            interactionSource = dayInteractionSource,
                            label = { Text(stringResource(res)) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                HapticButton(
                    text = stringResource(R.string.ai_submit),
                    onClick = {
                        onAnswer(selectedDays.joinToString(",") { it.toString() })
                    },
                    enabled = selectedDays.isNotEmpty()
                )
            }
        }
        "multi_choice" -> {
            var selected = remember { mutableStateOf<List<String>>(emptyList()) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEach { option ->
                    QuestionOptionCard(
                        option = option,
                        selected = option in selected.value,
                        onClick = {
                            selected.value = if (option in selected.value)
                                selected.value - option
                            else
                                selected.value + option
                        }
                    )
                }
                HapticButton(
                    text = stringResource(R.string.ai_submit),
                    onClick = { onAnswer(selected.value.joinToString("，")) },
                    enabled = selected.value.isNotEmpty()
                )
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

/** 手动输入页签：单个输入框 + 唯一提交按钮。 */
@Composable
private fun QuestionManualTab(
    onAnswer: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.ai_custom_input_placeholder)) }
        )
        HapticButton(
            text = stringResource(R.string.ai_chat_manual_submit),
            onClick = { onAnswer(textInput.trim()) },
            enabled = textInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 多时间选择对话框：可逐个添加/删除，确认时按时间排序。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogWithTimes(
    initialTimes: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var times by remember { mutableStateOf(initialTimes.distinct().toMutableList()) }
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ai_chat_pick_times))
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = displayMode
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (displayMode == TimePickerDisplayMode.Picker) {
                    TimePicker(state = timePickerState)
                } else {
                    TimeInput(state = timePickerState)
                }
                OutlinedButton(
                    onClick = {
                        val hh = "%02d".format(timePickerState.hour)
                        val mm = "%02d".format(timePickerState.minute)
                        val newTime = "$hh:$mm"
                        if (newTime !in times) {
                            times = (times + newTime).toMutableList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_chat_add_this_time))
                }
                if (times.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.ai_chat_selected_times),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        times.sorted().forEach { time ->
                            InputChip(
                                selected = true,
                                onClick = { times = (times - time).toMutableList() },
                                label = { Text(time) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.ai_chat_remove_time),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                }
            }
        }
    }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(times.sorted()) },
                enabled = times.isNotEmpty()
            ) {
                Text(stringResource(R.string.ai_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
