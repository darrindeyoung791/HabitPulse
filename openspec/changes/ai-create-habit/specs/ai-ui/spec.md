## ADDED Requirements

### Requirement: 用户可以通过 FAB 入口选择 AI 创建
The HomeScreen FAB SHALL show a selection dialog with "手动填写" and "AI 智能创建" options.

#### Scenario: 点击 FAB 显示选项
- **WHEN** user taps the FAB on HomeScreen
- **THEN** system displays a bottom sheet/dialog with two options: "手动填写" and "AI 智能创建"

#### Scenario: 选择 AI 创建
- **WHEN** user taps "AI 智能创建" option
- **THEN** system navigates to AICreateHabitScreen

#### Scenario: 选择手动填写
- **WHEN** user taps "手动填写" option
- **THEN** system navigates to HabitCreationScreen (existing behavior)

### Requirement: AI 创建界面显示欢迎语
When no conversation has started, the AI Create screen SHALL display a welcome message.

#### Scenario: 初始状态显示欢迎语
- **WHEN** user opens AICreateHabitScreen and no messages exist
- **THEN** system displays a large welcome message in the center of the content area

#### Scenario: 开始对话后隐藏欢迎语
- **WHEN** user sends first message
- **THEN** welcome message disappears and conversation UI takes over

### Requirement: AI 创建界面显示对话历史
The AI Create Habit screen SHALL display a conversation list with user messages, AI messages, and question components.

#### Scenario: 用户消息气泡
- **WHEN** user sends a message
- **THEN** system displays the message as a right-aligned bubble with accent background

#### Scenario: AI 消息气泡
- **WHEN** AI sends a reply (non-question)
- **THEN** system displays the text as a left-aligned bubble with surfaceVariant background

#### Scenario: AI 问题组件
- **WHEN** AI outputs ask_question tool
- **THEN** system displays the question with appropriate component (choice, time, day_of_week, etc.)

### Requirement: 系统显示时间选择问题
When AI outputs ask_question with type="time", the system SHALL render a time selection UI.

#### Scenario: 时间选择 UI 渲染
- **WHEN** AI sends type="time" question with options ["06:00", "07:00", "08:00"]
- **THEN** system displays:
  - Question text prompt
  - Chip row with quick options (6点, 7点, 8点)
  - Time picker icon button that opens Material TimePickerDialog

#### Scenario: 用户选择快捷选项
- **WHEN** user taps on a time chip (e.g., "7点")
- **THEN** system captures the answer and submits it to continue conversation

#### Scenario: 用户打开时间滚轮
- **WHEN** user taps the time picker icon
- **THEN** system displays Material TimePickerDialog for precise selection

### Requirement: 系统显示星期选择问题
When AI outputs ask_question with type="day_of_week", the system SHALL render a day-of-week selection UI.

#### Scenario: 星期选择 UI 渲染
- **WHEN** AI sends type="day_of_week" question
- **THEN** system displays:
  - Question text prompt
  - 7 day buttons (M T W T F S S in English, or 一 二 三 四 五 六 日 in Chinese)
  - Clicking a day toggles its selected state

#### Scenario: 快捷模式 Chip 显示
- **WHEN** AI provides options like ["每天", "工作日", "周末"]
- **THEN** system displays these as clickable Chips below the day buttons

### Requirement: 系统显示单选问题
When AI outputs ask_question with type="choice", the system SHALL render a choice selection UI.

#### Scenario: 单选卡片渲染
- **WHEN** AI sends type="choice" question with options ["每天", "每周特定几天"]
- **THEN** system displays each option as a selectable card in a Column
- **AND** selected card shows highlighted background

### Requirement: 系统显示多选确认问题
When AI outputs ask_question with type="multi_choice" or "confirm", the system SHALL render a confirmation UI.

#### Scenario: 多选确认 UI 渲染
- **WHEN** AI sends type="multi_choice" question with options about habits
- **THEN** system displays:
  - Question text prompt
  - List of habits with checkboxes
  - "确认创建" and "取消" buttons

### Requirement: 系统显示文本输入问题
When AI outputs ask_question with type="text", the system SHALL render a text input field.

#### Scenario: 文本输入 UI 渲染
- **WHEN** AI sends type="text" question
- **THEN** system displays:
  - Question text prompt
  - OutlinedTextField supporting multi-line input

### Requirement: 系统显示思考状态
While AI is processing, the system SHALL show a loading indicator.

#### Scenario: AI 思考中
- **WHEN** user sends a message and system is waiting for AI response
- **THEN** system displays "思考中..." with animated dots

### Requirement: 输入框高度根据内容动态调整
The input box height SHALL adjust based on the number of lines in the input text.

#### Scenario: 空状态高度
- **WHEN** input text is empty
- **THEN** input box displays with height for 3 lines

#### Scenario: 1-3 行文本
- **WHEN** input text has 1 to 3 lines
- **THEN** input box displays height matching the actual line count

#### Scenario: 3-7 行文本
- **WHEN** input text has more than 3 lines (up to 7)
- **THEN** input box displays height matching the actual line count

#### Scenario: 超过 7 行
- **WHEN** input text has more than 7 lines
- **THEN** input box displays height for exactly 7 lines and becomes scrollable

### Requirement: 发送按钮与停止按钮切换
The button next to the input box SHALL toggle between "Send" and "Stop" based on AI output state.

#### Scenario: 发送模式
- **WHEN** input has text and AI is not processing
- **THEN** button shows "发送" (Send) icon/label

#### Scenario: 停止模式
- **WHEN** AI is generating a response (streaming)
- **THEN** button changes to "停止" (Stop) label, tapping stops the AI output

### Requirement: AI 流式输出原始文本
The AI response SHALL be displayed in a streaming manner, showing raw text as it arrives.

#### Scenario: 流式显示
- **WHEN** AI sends a response
- **THEN** system displays text incrementally (character by character or word by word)

#### Scenario: 完整代码块后渲染组件
- **WHEN** a complete tool call code block (```xxx ... ```) is detected in the streaming response
- **THEN** system replaces the raw code block text with the rendered UI component for that tool

### Requirement: 清空对话按钮
After AI has output, a button to clear the conversation SHALL appear above the input box.

#### Scenario: 显示清空按钮
- **WHEN** AI has generated at least one message
- **THEN** system displays a clear conversation button above the input area

#### Scenario: 清空确认
- **WHEN** user taps the clear button
- **THEN** system shows a confirmation dialog: "确认清空对话？"

#### Scenario: 确认清空
- **WHEN** user confirms the clear action
- **THEN** all conversation messages are cleared and welcome message reappears

### Requirement: 退出确认对话框
When user attempts to exit the AI Create screen, the system SHALL confirm if they want to leave.

#### Scenario: 返回时询问
- **WHEN** user taps back or close button while in conversation
- **THEN** system shows confirmation: "确认离开？离开后对话将不保留。"

#### Scenario: 确认离开
- **WHEN** user confirms leaving
- **THEN** system navigates back to previous screen, discarding current conversation

#### Scenario: 取消离开
- **WHEN** user cancels the confirmation
- **THEN** system returns to the conversation screen

### Requirement: 进入 AI 设置时确认
When user taps the settings icon to enter AI Settings, the system SHALL confirm if they want to leave the conversation.

#### Scenario: 进入设置前确认
- **WHEN** user taps the settings icon in AI Create screen
- **THEN** system shows confirmation: "确认离开？离开后对话将不保留。"

#### Scenario: 确认进入设置
- **WHEN** user confirms
- **THEN** system navigates to AI Settings screen

### Requirement: 新会话不保留历史
Every time user enters the AI Create screen, it SHALL start with a fresh conversation.

#### Scenario: 重新进入是新对话
- **WHEN** user exits AI Create screen and later returns
- **THEN** conversation history is empty, welcome message is displayed

### Requirement: 系统显示错误状态
When an error occurs, the system SHALL display the error message in a system bubble.

#### Scenario: API 连接错误
- **WHEN** API call fails due to network issues
- **THEN** system displays error message with retry and stop buttons

#### Scenario: API 密钥错误
- **WHEN** API returns 401 Unauthorized
- **THEN** system displays "API 密钥无效，请前往设置检查"

### Requirement: 用户可以停止对话
The AI Create screen SHALL include a stop button to manually end the conversation.

#### Scenario: 用户点击停止
- **WHEN** user taps the stop button in the top app bar
- **THEN** system ends the conversation and displays the collected habits summary

### Requirement: 用户可以在确认阶段编辑习惯
At the confirmation stage, user SHALL be able to tap on a habit to edit it.

#### Scenario: 点击编辑习惯
- **WHEN** user taps on a habit summary in the confirm dialog
- **THEN** system navigates to HabitCreationScreen with pre-filled data

---

## ADDED Implementation Details (A2UI + AGenUI)

### A. 技术选型说明

本功能使用 **AGenUI SDK** (基于 Google A2UI v0.9 协议) 进行 AI 流式 UI 渲染。

**参考项目**：
- AGenUI SDK: `ref/AGenUI/`
- A2UI Skill: `ref/AGenUI/skills/a2ui-generation/`
- 对话 UI 参考: `ref/gpt_mobile/app/src/main/kotlin/dev/chungjungsoo/gptmobile/presentation/ui/chat/`

### B. AGenUI 初始化代码

**Application 初始化**（`HabitPulseApplication.kt`）：

```kotlin
class HabitPulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AGenUI.getInstance().initialize(this)

        try {
            val themeJson = assets.open("a2ui_theme.json").bufferedReader().use { it.readText() }
            val designToken = assets.open("a2ui_design_token.json").bufferedReader().use { it.readText() }
            AGenUI.getInstance().registerDefaultTheme(themeJson, designToken)
        } catch (e: ThemeException) {
            Log.e("HabitPulse", "AGenUI theme registration failed", e)
        }
    }
}
```

### C. SurfaceManager 集成封装

创建 `ai/ui/AGENUISurface.kt`，封装 AGenUI SurfaceManager 为 Compose 友好接口：

```kotlin
@Composable
fun rememberAGenUISurface(
    activity: ComponentActivity,
    surfaceId: String = "habit_chat",
    onAction: (String) -> Unit = {}
): AGenUISurfaceState {
    val context = LocalContext.current
    val surfaceManager = remember {
        SurfaceManager(activity).apply {
            addListener(object : ISurfaceManagerListener {
                override fun onCreateSurface(surface: Surface) {
                    // Surface 创建回调
                }
                override fun onDeleteSurface(surface: Surface) {
                    // Surface 销毁回调
                }
                override fun onReceiveActionEvent(event: String) {
                    onAction(event)
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            surfaceManager.destroy()
        }
    }

    return AGenUISurfaceState(surfaceManager)
}

class AGenUISurfaceState(val surfaceManager: SurfaceManager) {
    fun beginStream() = surfaceManager.beginTextStream()
    fun receiveChunk(chunk: String) = surfaceManager.receiveTextChunk(chunk)
    fun endStream() = surfaceManager.endTextStream()
}
```

### D. 对话气泡组件代码

**用户气泡**（`ai/ui/components/UserBubble.kt`）：

```kotlin
@Composable
fun UserChatBubble(
    modifier: Modifier = Modifier,
    text: String,
    onLongPress: () -> Unit = {}
) {
    val cardColor = CardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContentColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
    )

    Column(horizontalAlignment = Alignment.End) {
        Card(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                },
            shape = RoundedCornerShape(32.dp),
            colors = cardColor
        ) {
            ChatMarkdown(
                content = text,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
```

**AI 气泡**（`ai/ui/components/AIBubble.kt`）：

```kotlin
@Composable
fun AIChatBubble(
    modifier: Modifier = Modifier,
    text: String,
    thoughts: String = "",
    isLoading: Boolean = false,
    onCopyClick: () -> Unit = {}
) {
    val cardColor = CardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    )

    Column(modifier = modifier) {
        if (thoughts.isNotBlank()) {
            ThinkingBlock(
                modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
                thoughts = thoughts,
                contentIdentity = text,
                isLoading = isLoading && thoughts.isNotBlank() && text.isBlank()
            )
        }

        Card(
            shape = RoundedCornerShape(0.dp),
            colors = cardColor
        ) {
            Column {
                val displayText = if (isLoading) text + "●" else text
                ChatMarkdown(
                    content = displayText,
                    contentIdentity = text,
                    modifier = Modifier.padding(16.dp)
                )

                if (!isLoading) {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        IconButton(onClick = onCopyClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(R.string.copy_text)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**AI 头像**：

```kotlin
@Composable
fun AIAvatarIcon(loading: Boolean = false) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xFF00A67D)),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.RocketLaunch,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}
```

### E. 思考块组件代码

**ThinkingBlock**（`ai/ui/components/ThinkingBlock.kt`）：

```kotlin
@Composable
fun ThinkingBlock(
    modifier: Modifier = Modifier,
    thoughts: String,
    contentIdentity: Any = thoughts,
    isLoading: Boolean = false
) {
    if (thoughts.isBlank()) return

    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💭", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isExpanded) stringResource(R.string.hide_thinking)
                       else stringResource(R.string.view_thinking),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                Text(
                    text = stringResource(R.string.thinking_in_progress),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse)
                                    else stringResource(R.string.expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val displayText = if (isLoading) thoughts + "●" else thoughts
            ChatMarkdown(
                content = displayText,
                contentIdentity = contentIdentity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }

        if (!isExpanded && thoughts.isNotBlank()) {
            Text(
                text = thoughts.take(100).replace("\n", " ") +
                       if (thoughts.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}
```

### F. 输入框组件代码

**AIChatInputBox**（`ai/ui/components/AIInputBox.kt`）：

```kotlin
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
        TextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 3 * lineHeight + 32.dp, max = targetHeight),
            placeholder = {
                Text(stringResource(R.string.ai_input_placeholder))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            trailingIcon = if (inputText.isNotEmpty()) {
                {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (isLoading) {
                    // 停止按钮逻辑由 ConversationManager 处理
                } else if (inputText.isNotBlank()) {
                    onSendClick()
                }
            }
        ) {
            if (isLoading) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.ai_stop_button),
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ai_send_button),
                    tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### G. 欢迎页组件代码

**AIWelcomeContent**（`ai/ui/components/AIWelcomeContent.kt`）：

```kotlin
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
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.RocketLaunch,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.ai_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.ai_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartChat,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_start_chatting))
        }
    }
}
```

### H. 主界面代码结构

**AICreateHabitScreen**（`ai/ui/AICreateHabitScreen.kt`）：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICreateHabitScreen(
    onBackAction: () -> Unit,
    onNavigateToSettings: () -> Unit,
    aiCreateHabitViewModel: AICreateHabitViewModel = hiltViewModel()
) {
    val uiState by aiCreateHabitViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val hasMessages = uiState.messages.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_create_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasMessages) {
                            aiCreateHabitViewModel.showExitConfirmation()
                        } else {
                            onBackAction()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasMessages) {
                            aiCreateHabitViewModel.showSettingsConfirmation()
                        } else {
                            onNavigateToSettings()
                        }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_ai_title))
                    }
                    if (uiState.isLoading) {
                        IconButton(onClick = { aiCreateHabitViewModel.stopGeneration() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ai_stop_button))
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
                    itemsIndexed(uiState.messages) { index, message ->
                        when (message) {
                            is ChatMessage.User -> UserChatBubble(
                                text = message.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            is ChatMessage.AI -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    AIAvatarIcon(loading = uiState.isLoading && index == uiState.messages.lastIndex)
                                    AIChatBubble(
                                        text = message.text,
                                        thoughts = message.thoughts,
                                        isLoading = uiState.isLoading && index == uiState.messages.lastIndex,
                                        onCopyClick = { /* copy logic */ }
                                    )
                                }
                            }
                            is ChatMessage.Question -> {
                                QuestionComponent(
                                    question = message.question,
                                    onAnswer = { answer -> aiCreateHabitViewModel.submitAnswer(answer) }
                                )
                            }
                        }
                    }
                }
            } else {
                AIWelcomeContent(
                    modifier = Modifier.weight(1f),
                    onStartChat = { /* focus input */ }
                )
            }

            if (uiState.showClearButton) {
                TextButton(
                    onClick = { aiCreateHabitViewModel.showClearConfirmation() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ai_clear_conversation))
                }
            }

            AIChatInputBox(
                inputText = uiState.inputText,
                onTextChange = { aiCreateHabitViewModel.updateInputText(it) },
                onSendClick = { aiCreateHabitViewModel.sendMessage() },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 对话框
    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { aiCreateHabitViewModel.dismissExitConfirmation() },
            title = { Text(stringResource(R.string.ai_exit_title)) },
            text = { Text(stringResource(R.string.ai_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    aiCreateHabitViewModel.dismissExitConfirmation()
                    onBackAction()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { aiCreateHabitViewModel.dismissExitConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { aiCreateHabitViewModel.dismissClearConfirmation() },
            title = { Text(stringResource(R.string.ai_clear_title)) },
            text = { Text(stringResource(R.string.ai_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    aiCreateHabitViewModel.clearConversation()
                    aiCreateHabitViewModel.dismissClearConfirmation()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { aiCreateHabitViewModel.dismissClearConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
```

### I. A2UI 问题组件映射

使用 AGenUI A2UI 组件映射到各问题类型：

| 问题 type | A2UI 组件 | 实现方式 |
|-----------|----------|---------|
| `choice` | ChoicePicker | displayStyle="chips", variant="mutuallyExclusive" |
| `day_of_week` | ChoicePicker | 7个 Chip 选项 + 快捷选项 |
| `multi_choice` | ChoicePicker | displayStyle="checkbox", variant="multipleSelection" |
| `time` | DateTimeInput + Row | enableTime=true + 快捷 Chip Row |
| `text` | TextField | variant="longText" |
| `confirm` | Card + List + Row | 组合组件 |

**A2UI ChoicePicker JSON 示例**（`choice` 类型）：

```json
{
  "surfaceId": "habit_chat",
  "updateComponents": {
    "choice_picker": {
      "id": "choice_picker",
      "component": "ChoicePicker",
      "label": "选择重复周期",
      "variant": "mutuallyExclusive",
      "displayStyle": "chips",
      "options": [
        { "label": "每天", "value": "DAILY" },
        { "label": "每周特定几天", "value": "WEEKLY" }
      ],
      "value": []
    }
  }
}
```

**A2UI DateTimeInput JSON 示例**（`time` 类型）：

```json
{
  "surfaceId": "habit_chat",
  "updateComponents": {
    "time_question": {
      "id": "time_question",
      "component": "DateTimeInput",
      "value": "",
      "enableTime": true,
      "label": "选择提醒时间"
    }
  }
}
```

**A2UI 确认卡片组合组件**（`confirm` 类型）：

```json
{
  "surfaceId": "habit_chat",
  "updateComponents": {
    "confirm_card": {
      "id": "confirm_card",
      "component": "Card",
      "child": { "componentId": "confirm_content" }
    },
    "confirm_content": {
      "id": "confirm_content",
      "component": "Column",
      "children": [
        { "componentId": "confirm_title" },
        { "componentId": "habit_list" },
        { "componentId": "confirm_row" }
      ]
    },
    "confirm_title": {
      "id": "confirm_title",
      "component": "Text",
      "text": "确认要创建以下习惯吗？",
      "variant": "h4"
    },
    "habit_list": {
      "id": "habit_list",
      "component": "List",
      "direction": "vertical",
      "children": [
        { "componentId": "habit_item_0" },
        { "componentId": "habit_item_1" }
      ]
    },
    "habit_item_0": {
      "id": "habit_item_0",
      "component": "CheckBox",
      "label": "① 每天 07:00 吃鸡蛋",
      "value": true
    },
    "confirm_row": {
      "id": "confirm_row",
      "component": "Row",
      "justify": "spaceBetween",
      "children": [
        { "componentId": "cancel_btn" },
        { "componentId": "confirm_btn" }
      ]
    },
    "cancel_btn": {
      "id": "cancel_btn",
      "component": "Button",
      "child": { "componentId": "cancel_text" },
      "variant": "borderless",
      "action": { "type": "Navigation", "route": "cancel" }
    },
    "confirm_btn": {
      "id": "confirm_btn",
      "component": "Button",
      "child": { "componentId": "confirm_text" },
      "variant": "primary",
      "action": { "type": "Navigation", "route": "confirm" }
    }
  }
}
```

### J. ViewModel 状态定义

```kotlin
data class AICreateHabitUIState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentQuestion: PendingQuestion? = null,
    val collectedHabits: List<PartialHabit> = emptyList(),
    val showExitConfirmation: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val showSettingsConfirmation: Boolean = false,
    val showClearButton: Boolean = false,
    val pendingHabitCount: Int? = null,  // null = unknown (single habit mode), 1+ = explicit count
    val isHabitRelated: Boolean = true    // tracks whether current input is habit-related
)

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AI(val text: String, val thoughts: String = "") : ChatMessage()
    data class Question(val question: PendingQuestion) : ChatMessage()
}

data class PendingQuestion(
    val questionId: String,
    val type: String,
    val prompt: String,
    val context: String? = null,
    val options: List<String> = emptyList(),
    val allowCustomInput: Boolean = false
)

data class PartialHabit(
    val title: String,
    val repeatCycle: String,
    val repeatDays: List<Int> = emptyList(),
    val reminderTimes: List<String> = emptyList(),
    val notes: String = ""
)
```

### K. 依赖集成

**AAR 位置**: `app/libs/AGenUI-Client-Android-release.aar`
**构建来源**: `ref/AGenUI/scripts/android/build.sh`

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(fileTree("libs" to { include("*.aar") }))
}
```