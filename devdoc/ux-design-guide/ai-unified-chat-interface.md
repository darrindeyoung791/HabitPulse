# 统一 AI 交互界面（AI Chat）设计规范

> 本文档描述 HabitPulse 新版统一 AI 对话界面的设计与实现规范，作为后续开发的唯一参考。
> 该界面取代旧版 `AICreateHabitScreen`，从「仅 AI 创建习惯」扩展为「创建 / 编辑 / 删除习惯 +
> 开/关应用设置 + 打开设置子页」的全场景对话助手。改动交互或协议前请先同步更新本文档。

## 概览

| 项 | 值 |
|----|-----|
| 目标文件 | `ui/screens/ai/AIChatScreen.kt`（取代 `AICreateHabitScreen.kt`） |
| ViewModel | `viewmodel/AIChatViewModel.kt`（取代 `AICreateHabitViewModel.kt`） |
| 导航 | `Route.AICreateHabit` 改名为 `Route.AIChat`（保留入口：首页 FAB「AI」选项） |
| 工具协议 | **原生 function-calling**（`tool_calls` 字段），废弃 ````json 代码块文本协议 |
| 提供商切换 | 顶栏快捷切换器（会话级，默认取全局活跃配置） |
| 习惯编辑 | 保留跳转手动编辑屏（`Route.EditHabit`），废除全局 `AIPrefillHabitHolder` |
| 设置控制 | 除 AI 配置外的全部开关型设置；支持返回「打开设置子页」导航卡片 |
| 深度思考 | 流式捕获 `reasoning_content` → 思考块展示 |
| Token 统计 | 会话内累计提供商 `usage`（流式/非流式，仅精确值，无估算兜底）+ 顶栏计数展示 |

### 为什么重构

旧版 `AICreateHabitScreen.kt`（1414 行）存在一系列问题（详见 §3.1、§6.1），其中与本需求直接相关：

1. **多模型支持缺失**：`ConversationManager` 只在首次发消息时用当时的活跃配置构建一次
   `LLMClient`，之后中段切换全局活跃配置无效（引擎与 UI 状态不一致）。
2. **工具协议脆弱**：````json 代码块文本协议依赖 LLM 严格遵守 markdown 代码块格式，且
   `ResponseParser` 用正则猜工具名/参数，误解析率高；不支持原生 `tool_calls` 的
   `role=tool` 回环，多工具链式调用（如「查询习惯 → 确认删除」）无法可靠实现。
3. **编辑习惯中断上下文**：编辑跳转依赖全局单例 `AIPrefillHabitHolder` 传临时数据，
   状态泄漏风险高，且 `PartialHabit` 无监督人字段，预填丢数据。
4. **能力单一**：只能建习惯，不能改/删，也不能动设置。

---

## 1. 页面骨架与导航

### 1.1 布局

```
┌────────────────────────────────────────────┐
│ TopAppBar                                  │
│  [←]  AI 助手        [提供商chips▼] [🗑]  │
├────────────────────────────────────────────┤
│  LazyColumn（消息列表，单一数据源 messages）│
│  · UserChatBubble / AIChatBubble           │
│  · ThinkingBlock（深度思考）               │
│  · QuestionComponent（提问卡片）           │
│  · HabitCreatedCard（新建习惯卡）          │
│  · HabitPickerCard（搜索既有习惯）         │
│  · HabitDeleteCard（删除确认）             │
│  · SettingStatusCard / SettingChangeCard   │
│  · SettingsNavCard（打开设置子页）         │
│  [↧ 滚动到底部 FAB]                        │
├────────────────────────────────────────────┤
│  [清除对话]                                │
│  AIChatInputBox（输入框 + 发送/停止）      │
│  免责声明                                  │
└────────────────────────────────────────────┘
```

- 消息列表仅由 ViewModel 的 `messages: StateFlow<List<ChatMessageUIItem>>` 驱动，
  **不再维护平行的 `collectedHabits` 列表**（消除双数据源漂移）。
- 所有卡片都是 `ChatMessageUIItem` 的一种 `type`，卡片自身状态（如删除确认中、设置已改）
  由 `item` 内 `remember` 持有或回写到 VM 的轻量 `pendingActions`。

### 1.2 顶栏

| 元素 | 行为 |
|------|------|
| 返回键 | 无消息直接返回；有消息弹「确认离开」对话框（未确认的新习惯提示「仍有习惯尚未保存」） |
| 标题 | 静态「AI 助手」；AI 输出中显示「AI 正在输出…」 |
| **提供商切换器** | 见 §3，显示当前 `配置名 · 模型`，点开下拉切换；管理入口在菜单底部 |
| 清除对话 | 仅当 `hasMessages` 显示；弹确认对话框；清除前清理未确认临时数据 |

### 1.3 横屏与键盘（修复旧缺陷）

- **禁止**用 `isLandscape && imeVisible` 整体隐藏 TopAppBar（旧实现丢失返回键）。
- 横屏 + 键盘时：TopAppBar 保持显示，仅将 `AIChatInputBox` 压为 2 行（沿用现有
  `maxLines = if (isLandscape) 2`），免责声明隐藏。

### 1.4 入口

- 首页 FAB「用 AI 创建习惯」选项 → `Route.AIChat`（沿用 `popUpTo(Home)` 导航参数）。
- 顶栏齿轮不再直接跳设置；改为在提供商下拉菜单底部提供「管理提供商」入口（进
  `NewSettingsAIActivity`）。

---

## 2. 提供商 / 模型快捷切换

### 2.1 交互

- 顶栏渲染一个 `AssistChip`：文本 `配置名 · 模型名`，点击展开 `DropdownMenu`。
- 菜单项：每个已配置 `AIConfig` 一行，显示 `名称` + `模型 · 端点host`，当前项带选中标记；
  底部固定一项「管理提供商 →」（进 `NewSettingsAIActivity`）。
- 数据源：`UserPreferences.aiConfigsFlow`（响应式，新增/删除配置实时反映）。
- 当前无任何配置：chip 显示「未配置」，点击直接进 `NewSettingsAIActivity`（即旧
  `NoApiKeyPrompt` 的等价交互，移除独立全屏提示页）。

### 2.2 切换语义（会话级）

| 状态 | 切换行为 |
|------|----------|
| 无消息（空会话） | 直接切换，重建会话引擎 |
| 有消息 | 弹确认框「切换提供商将开始新对话，当前对话与未确认内容将被清除」；确认后重建 |
| 有未确认的新习惯卡 | 同上，确认后按 §6.4 清理临时数据 |

- **会话级选择**存于 VM `selectedConfigId`（`MutableStateFlow`），默认取全局活跃配置
  `activeConfigFlow` 的 id；**不写回**全局 `LLM_ACTIVE_CONFIG_ID`（切配置不影响其他入口）。
- 切换动作：`conversationManager?.reset()` + 置空重建（§4.4 的 `rebuildConversation(config)`）。

### 2.3 实现要点

```kotlin
// 会话引擎按“当前选中的配置”重建，不再缓存首条消息的配置
private fun rebuildConversation(config: AIConfig) {
    conversationManager?.reset()
    conversationManager = null
    conversationManager = ConversationManager(
        llmClient = LLMClient(config.toLLMConfig()),   // toLLMConfig 已含 thinkingEnabled
        toolRegistry = toolRegistry,
        streamingEnabled = config.streamingEnabled,
        scope = viewModelScope,
        systemPrompt = SystemPrompt.getSystemPrompt(app)
    )
    observeConversation()
}
```

- `sendMessage` 改为每次都用「当前选中配置」确认 `isValid()` 与 `hasApiKeyConfigured()`，
  但引擎只在**切换或会话为空**时重建，避免每轮重建。

---

## 3. 工具协议重构（function-calling）

### 3.1 现状问题（作为重构依据）

1. `ConversationManager.sendToLLMStreaming` 靠 `Regex("```json[\\s\\S]*?```")` 在流式文本中
   检测完整代码块后 `processResponse` 并抛 `CancellationException` 提前终止——脆弱且丢弃尾文。
2. `ResponseParser` 用正则猜测工具名/参数，`confirm` 工具名被 `isValidToolName` 放行但
   `ToolRegistry` 未注册，调用被静默跳过（`ConfirmTool.kt` 孤儿文件）。
3. `LLMClient.chatStream` 不解析流式 `tool_calls`（原生 function-calling 走非流式才可用）。
4. `chatStream` 收到非 `data:` 前缀的 JSON 响应（不支持流式的端点）时静默产出空内容。

### 3.2 目标协议

- 请求体新增 `tools`（函数定义数组）+ `tool_choice: "auto"`；工具定义集中生成（见 §5）。
- 响应用 LLM 原生 `tool_calls`；**废弃** ````json 代码块文本协议，`SystemPrompt` 中删除
  「必须用代码块包裹工具调用」的全部规则。
- `ChatMessage` / `Message` 增加 `tool_calls` 与 `role="tool"` 支持；
  执行工具后把结果作为 `role=tool` 消息回灌，驱动多轮工具链。
- 纯文本回复（无 tool_calls）仍走普通 AI 气泡。

### 3.3 LLMClient 改造

- `ChatRequest` 增加字段：`tools: List<ToolDef>?`、`tool_choice: String?`。
- 新增 `ToolDef` 数据类：`name / description / parameters`（JSON Schema）。
- 非流式：`ChatResponse.message.toolCalls` 已解析；若 `content` 为空且 `toolCalls` 非空，
  原样返回整段响应（保留现逻辑）。
- 流式：累积 `delta.tool_calls`（`id`、`function.name`、`function.arguments` 分片），
  `Done` 时若有完整 tool_call 则触发工具执行；同时捕获 `delta.reasoning_content`（§8）。
- **流式降级**：`Done` 时 `fullContent` 为空或无法解析出内容/tool_calls，回退一次非流式请求
  （而非静默失败）。

### 3.4 ConversationManager 改造

```kotlin
// 单轮处理：执行 assistant 消息里的所有 tool_calls，结果回灌，循环直到无工具或遇暂停点
while (true) {
    val result = llmClient.chat(messages)          // 非流式路径；流式路径见 3.5
    messages.add(Message(role="assistant", content=..., toolCalls=...))
    val toolCalls = result.toolCalls
    if (toolCalls.isEmpty()) {
        _events.value = ConversationEvent.AIMessageReceived(result.text, result.thoughts)
        break
    }
    var needUserPause = false
    for (tc in toolCalls) {
        val outcome = toolRegistry.execute(tc.name, tc.arguments)
        when (outcome) {
            is ToolResult.Success -> {
                when (tc.name) {
                    "ask_question" -> needUserPause = true   // 提问：暂停等用户
                    "create_habit" -> needUserPause = true   // 建卡：暂停等确认
                    "delete_habit" -> needUserPause = true   // 删除确认卡
                    "reply" -> {}                             // 纯文本
                    else -> {}                                // 查询/设置类：无 UI 卡则自动续
                }
                messages.add(Message(role="tool", content=encodeResult(outcome), toolCallId=tc.id))
            }
            is ToolResult.Error -> {
                retryCount++
                messages.add(Message(role="tool", content="错误：${outcome.message}", toolCallId=tc.id))
                if (retryCount >= MAX_RETRIES) { _events.value = Error(...); return }
            }
        }
    }
    if (needUserPause) break   // 等用户确认后，由 UI 发“继续”信号（沿用 resume + continueConversation）
}
```

- 保留 `ConversationGuard`（问题上限 20 / 同题连续 3 次），并扩展设置纠错（§7.4）。
- `retryLastTurn`：删除最后一个 assistant turn 与其 tool 结果消息后重发（沿用现有
  `removeLastAssistantTurn`，但需同时移除 `role=tool` 尾消息）。

### 3.5 流式 + 深度思考适配

- 流式工具调用分片累积完成后执行；思考内容独立于正文流（`reasoning_content`）。
- `thinkingEnabled` 时（配置项 `AIConfig.thinkingEnabled` → `LLMConfig.thinkingEnabled`）：
  - `ThinkingStarted / ThinkingUpdated / ThinkingEnded` 事件按 `reasoning_content` 分片触发；
  - 思考文本到达后正文 `content` 才开始（Zhipu GLM 顺序）。
- 关闭思考时无 `reasoning_content`，思考块不渲染（沿用现有判断）。

### 3.6 ResponseParser 定位

- 原生 `tool_calls` 成为唯一协议；`ResponseParser` 代码块解析仅保留为
  **`isValidToolName` / `extractToolName` 等兼容层**，供旧端点回退，不再进入主流程。
- `isValidToolName` 集合更新为 §5 全部工具名，并删除 `confirm`（不再作为工具暴露）。

---

## 4. 工具注册表与工具定义

### 4.1 注册表

`ToolRegistry` 注册以下工具（全部实现 `Tool` 接口）：

| 工具 | 名称 | 作用 |
|------|------|------|
| 习惯 | `create_habit` | 创建新习惯（复用现有 `CreateHabitTool`，补强校验） |
| 习惯 | `search_habits` | 按关键字查询既有习惯 → `HabitPickerCard` |
| 习惯 | `edit_habit` | 定位要编辑的习惯 → 返回「前往编辑」卡片（跳 `Route.EditHabit`） |
| 习惯 | `delete_habit` | 删除习惯 → 返回删除确认卡 |
| 提问 | `ask_question` | 向用户提问（复用 `QuestionTool`） |
| 设置 | `get_settings_status` | 返回全部可控制设置当前状态 → `SettingStatusCard` |
| 设置 | `update_setting` | 修改一项开关设置 → `SettingChangeCard`（可撤销） |
| 设置 | `open_settings_page` | 打开指定设置子页 → `SettingsNavCard` |
| 回复 | `reply` | 纯文本回复（复用 `ReplyTool`） |

`ConfirmTool.kt` 删除（确认改由 UI 卡片承担，不再作为工具）。

### 4.2 习惯类工具

**create_habit**：沿用 `CreateHabitTool`，补强校验：

- `reminder_times` 每项必须匹配 `HH:mm`（`Regex("^([01]\\d|2[0-3]):[0-5]\\d$")`），去重；
- `repeat_days` 每项 ∈ 0..6，去重；WEEKLY 且 `repeat_days` 为空 → 错误并提示 `ask_question`；
- `title` 长度 ≤ 30、`notes` ≤ 200，超限截断并返回提示（与手动创建屏一致）。

**search_habits**：

```json
{
  "description": "按关键字模糊查询现有习惯（匹配标题或备注）。命中后返回选择卡片，供用户编辑或删除。无关键字时返回最近创建的前 10 条。",
  "properties": {
    "keyword": { "type": "string", "description": "标题或备注包含的关键字；留空返回最近 10 条" },
    "limit":   { "type": "integer", "description": "最多返回条数，默认 10，最大 20" }
  }
}
```

- 实现：走 `HabitRepository.searchHabitsFlow`（一次性 `first()`），按 `sortOrder` 排序。
- 返回 `HabitSearchData(habits: List<HabitBrief>)`；`HabitBrief` 含 `id/title/summary`。
- UI 渲染 `HabitPickerCard`：标题 + 摘要 + `编辑`/`删除` 两个按钮（§6.2）。

**edit_habit**：

```json
{
  "description": "用户要求编辑某个现有习惯时调用。参数为 habit_id。返回一张「前往编辑」卡片，点击进入手动编辑页。",
  "properties": {
    "habit_id": { "type": "string", "description": "目标习惯的 UUID" },
    "title":    { "type": "string", "description": "习惯名称，用于校验回显，必须与库中一致" }
  }
}
```

- 校验 `habit_id` 存在且 `title` 与库中一致，否则 `ToolResult.Error`。
- 返回 `HabitEditData(habitId)`；UI 渲染 `HabitEditCard`（「将打开编辑页」+ 前往按钮）。
- 跳转后由 `HabitCreationScreen`（EDIT 模式）从 DB 加载完整字段，**无需 prefill**（§6.3）。

**delete_habit**：

```json
{
  "description": "用户要求删除某个现有习惯时调用。返回删除确认卡，用户确认后才真正删除。",
  "properties": {
    "habit_id": { "type": "string", "description": "目标习惯的 UUID" },
    "title":    { "type": "string", "description": "习惯名称，用于校验回显" }
  }
}
```

- 校验同上；返回 `HabitDeleteData(habitId, title)` → `HabitDeleteCard`（§6.2）。
- 确认按钮调用 `HabitRepository.deleteHabit`（级联删除打卡记录），并回灌一条 `role=tool`
  成功消息让 AI 收尾；取消则通知 AI 未删除。

### 4.3 设置类工具

**可控制设置白名单**（`enum class ControllableSetting(key, labelRes, default)`）：

| key | 设置 | 所在子页 | 值类型 |
|-----|------|----------|--------|
| `reminder_enabled` | 习惯提醒开关 | 通知 | Boolean |
| `dnd_enabled` | 免打扰开关 | 通知 | Boolean |
| `persistent_notification` | 常驻通知（保活） | 通知 | Boolean |
| `haptic_feedback_enabled` | 应用内震动 | 通用 | Boolean |
| `show_splash_ad` | 开屏广告 | 通用 | Boolean |
| `force_tablet_landscape` | 平板强制横屏 | 通用 | Boolean |

**明确排除**：AI 配置列表 / 当前模型 / 提供商相关全部设置（AI 设置页内容不可被 AI 改）、
免打扰时段、通知模板、调试页与震动参数。AI 无法直接改的，只能用 `open_settings_page`
引导用户去手动改。

**get_settings_status**：

```json
{ "description": "返回当前所有可控制设置的开/关状态。在用户询问设置状态或修改设置前，建议先调用一次。", "properties": {} }
```

- 返回 `SettingsStatusData(pairs: List<SettingPair>)` → `SettingStatusCard`（列出 key 的中文名 + 当前开/关）。

**update_setting**：

```json
{
  "description": "修改一项开关设置（仅限白名单 key）。返回变更确认卡，用户可一键撤销。",
  "properties": {
    "key":   { "type": "string", "enum": ["reminder_enabled","dnd_enabled","persistent_notification","haptic_feedback_enabled","show_splash_ad","force_tablet_landscape"] },
    "value": { "type": "boolean" }
  }
}
```

- key 不在白名单 → `ToolResult.Error("不支持修改的设置项，可用 get_settings_status 查看可控制项")`，
  计入纠错（§7.4）。
- 实现：映射到 `UserPreferences` 的 `setReminderEnabled / setDndEnabled / setPersistentNotification /
  setHapticsEnabled / setShowSplashAd / setForceTabletLandscape`。
- 返回 `SettingChangeData(key, oldValue, newValue)` → `SettingChangeCard`（展示变更 + `撤销`按钮，
  撤销调用同一 setter 写回旧值）。

**open_settings_page**：

```json
{
  "description": "当用户询问某设置在哪个页面、或 AI 无法直接修改时调用。返回一张导航卡片，点击跳转到对应设置子页。",
  "properties": {
    "page": { "type": "string", "enum": ["notifications","general","about","ai","home"] }
  }
}
```

- `page → Activity` 映射：`notifications → NewSettingsNotificationsActivity`、
  `general → NewSettingsGeneralActivity`、`about → NewSettingsAboutActivity`、
  `ai → NewSettingsAIActivity`、`home → NewSettingsActivity`。
- 返回 `SettingsNavData(page, activityLabel)` → `SettingsNavCard`（`打开 <页面名> 设置` 按钮，
  `context.startActivity(Intent(context, XxxActivity))`）。
- 若 `open_settings_page` 的 `page` 不在枚举 → `ToolResult.Error`，计入纠错。

### 4.4 提问类工具

- `ask_question` 沿用 `QuestionTool`；`QuestionComponent` 修复见 §6.5。

---

## 5. 习惯卡片交互

### 5.1 新建习惯卡片（HabitCreatedCard）

- 沿用现有样式：勾选图标 + 标题 + 摘要 + 时间行。
- 按钮改为三个：`确认` / `编辑` / `删除`。
  - `确认`：插入 DB（`repository.insertHabit`），卡片变「已确认」态；全部确认后
    `continueConversation("如有剩余习惯等待建立，请继续。若无，与用户道别")`。
  - `编辑`：仅确认后可用（未确认先自动确认再编辑）；直接
    `navController.navigate(Route.EditHabit.createRoute(dbId))`。
  - `删除`：直接删除已插入的 DB 行并移除卡片消息（复用现有撤销语义，替换「重试」暗示）。
- **不再需要**「确认对话框批量创建」：卡片逐个确认即保存，删除 `ConfirmationDialog` 与
  `showConfirmDialog` / `ConfirmationRequested` 事件。

### 5.2 既有习惯选择 / 编辑 / 删除卡片

- `HabitPickerCard`（`search_habits` 返回）：标题 + 摘要 + `编辑` / `删除`。
- `编辑` → `Route.EditHabit.createRoute(id)`（手动编辑屏从 DB 加载，含监督人等全部字段）。
- `删除` → 展开 `HabitDeleteCard`（内联确认）或弹 `AlertDialog`；确认后
  `repository.deleteHabit` 并回灌工具结果。

### 5.3 编辑跳转机制修复（废除 AIPrefillHabitHolder）

- **删除** `viewmodel/AIPrefillHabitHolder.kt` 全局单例。
- `HabitPulseNavGraph` 中 `Route.CreateHabit` 的 prefill 逻辑（`isAiEdit` 分支）删除，
  `HabitCreationScreen` 的 `prefillHabit` 参数删除。
- 编辑一律以 `Route.EditHabit.createRoute(habitId)` 进入，`HabitCreationScreen` 的
  `LaunchedEffect(habitId)` 已能从 DB 加载全字段（`HabitViewModel.getHabitById`）。
- 原 `toHabitEntity` 需补齐非默认字段：AI 确认插入时 `createdDate/modifiedDate` 用当前时间，
  `sortOrder` 沿用 `saveHabit` 的「置顶」逻辑（`getTopSortOrder() - 1`），其余字段保持默认。

### 5.4 数据一致性（修复临时→DB 双存储）

- **只有确认时才插库**：取消「编辑先插库」与「撤销再删库」的来回写。
- 未确认的新习惯仅存在于 `messages` 的内存卡片；退出 / 清除 / 切换提供商时直接丢弃，
  不再需要 `cleanupUnconfirmedHabits` 的 DB 清理（进程被杀也不会留孤儿行）。

---

## 6. 设置卡片与纠错机制

### 6.1 交互流

1. 用户：「帮我关掉开屏广告」/「提醒是开着的吗」/「免打扰设置在哪？」
2. AI 先调 `get_settings_status`（若未持有状态）→ 再按需 `update_setting` /
   `open_settings_page`。
3. `update_setting` 成功 → `SettingChangeCard` 展示「开屏广告：开 → 关」+ `撤销`。
4. `open_settings_page` → `SettingsNavCard`，用户点击进入对应 Activity。

### 6.2 设置卡片样式

- 一律走 `SettingsSegmentedGroup` / `SettingsSegmentedSwitch` 组件体系（与设置页一致，
  见 `listitem-style.md`），不使用旧式 `Surface + Row` 拼装。
- `SettingChangeCard` 高亮变化值，`撤销` 按钮带 `PressVibrationFeedback`。

### 6.3 打开设置子页卡片（SettingsNavCard）

- 图标用 `AccentSeeds` 种子色（§custom-color-scheme.md 的 `rememberSeedAccentTint`）。
- 副标题显示目标子页的 activity 名称（本地化），按钮文案「打开 <页面名> 设置」。

### 6.4 纠错机制（Correction Guard）

- 扩展 `ConversationGuard`：新增字段 `invalidSettingTries`；`update_setting` / `open_settings_page`
  返回 `ToolResult.Error`（未知 key / 非法 page / 越权尝试改 AI 配置）时累计；
  连续 ≥ 3 次 → 触发 `GuardBlocked` 停止，并提示「涉及设置的操作请检查后重试」。
- **系统提示词**明确：AI 不能自行打开任意 Activity，只能通过 `open_settings_page` 的枚举
  `page`；提示词中给出 §4.3 完整映射表（key ↔ 中文名 ↔ 所在子页），降低 LLM 猜错概率。
- 每次 `update_setting` 成功后重置计数。

---

## 7. 深度思考适配

### 7.1 流式 reasoning_content

```kotlin
// LLMClient.chatStream：解析 delta 中的 reasoning_content
when {
    delta.reasoningContent != null -> _events.value = ThinkingUpdated(thinking = accReasoning)
    delta.toolCalls != null        -> accumulateToolCallFragment()
    delta.content != null          -> emit(StreamChunk.Content(delta.content))
}
```

- 仅在 `thinkingEnabled` 时捕获；思考结束（正文开始）发 `ThinkingEnded`。
- 非流式：`ChatResponse.message.reasoningContent` 直接作为 `thoughts`。

### 7.2 思考块展示状态（复用 ThinkingBlock）

| 状态 | 表现 |
|------|------|
| 思考中 | 左侧小号 `CircularProgressIndicator` + 「深度思考中…」；可点开看已流出的推理文本 |
| 思考完成（未展开） | `ExpandMore` + 「查看思考过程」 |
| 展开 | `ExpandLess` + 推理文本（`bodySmall` / `onSurfaceVariant`） |

- 思考与正文分离：正文未开始时思考块占位，正文到达后二者上下排列（沿用现有 `AIChatBubble`）。

---

## 8. 系统提示词规范（增量）

在 `assets/prompts/system_prompt.md` 中：

### 8.1 新增规则

1. **可用工具**列表更新为 §4.1 的 9 个工具；删除「必须用 ```json 代码块包裹」的全部表述。
2. **设置操作规则**：
   - 修改设置前先 `get_settings_status`；
   - 只能改白名单开关（附 key 表与中文名）；改不了的一律 `open_settings_page` 引导；
   - 禁止尝试修改 AI 提供商/模型/密钥、免打扰时段、通知模板。
3. **编辑/删除规则**：
   - 编辑/删除既有习惯必须先 `search_habits` 定位（禁止凭空构造 habit_id）；
   - 删除必须等用户确认（UI 卡片），AI 不直接删除；
   - 用户意图含糊（如多个同名习惯）时用 `ask_question` 澄清。
4. **打开设置页规则**：只能通过 `open_settings_page` 的枚举 page，禁止编造 Activity 名。
5. 输出语言跟随用户（沿用）。

### 8.2 纠错示例

- AI 输出不存在的 key → 工具报错 + guard 计数 → AI 被要求改用合法 key 或引导用户。
- AI 连续报错 → `GuardBlocked`，界面提示，AI 停止。

---

## 9. 组件清单

| 组件 | 文件 | 说明 |
|------|------|------|
| `ProviderSwitcher` | `ui/screens/ai/components/` | 顶栏提供商切换 chip + 下拉 |
| `AIChatInputBox` | 沿用 `AICreateHabitScreen` 内实现，抽取到 components | 发送/停止、横屏 2 行 |
| `ThinkingBlock` | 沿用，抽取 | 深度思考状态机（§7.2） |
| `UserChatBubble` / `AIChatBubble` | 沿用 | |
| `QuestionComponent` | 沿用，重构 | §6.5 修复 |
| `HabitCreatedCard` | 沿用并扩展 | 确认 / 编辑 / 删除三按钮 |
| `HabitPickerCard` | 新增 | 既有习惯选择（编辑/删除） |
| `HabitEditCard` | 新增 | 「前往编辑」 |
| `HabitDeleteCard` | 新增 | 删除确认 |
| `SettingStatusCard` | 新增 | 设置状态清单 |
| `SettingChangeCard` | 新增 | 变更 + 撤销 |
| `SettingsNavCard` | 新增 | 打开设置子页 |
| `AIChatViewModel` | `viewmodel/AIChatViewModel.kt` | 取代旧 VM |

---

## 10. 使用约定（开发指南）

1. **i18n**：所有新增文本进 5 个 `strings.xml`（`values/`、`values-en-rUS/`、
   `values-en-rGB/`、`values-zh-rHK/`、`values-zh-rTW/`）；`ControllableSetting` 的中文名
   用资源 id。
2. **触感震动**：可点击卡片 / 开关行一律复用 `PressVibrationFeedback`（受
   `HAPTIC_FEEDBACK_ENABLED` 与调试参数控制），不在业务组件里直接调 `vibrator`。
3. **强调色**：设置类卡片图标走 `AccentSeeds` 种子色；习惯卡沿用现有语义色。
4. **导航**：习惯编辑用 `Route.EditHabit.createRoute(id)`；打开设置子页用
   `context.startActivity(Intent(...))`；均套 `rememberDebounceClickHandler` 防抖。
5. **状态单一来源**：消息列表 = `messages`；确认/删除等待决状态走 `pendingActions`。
6. **错误展示统一**：`errorMessage` 直接渲染为 AI 气泡（`ai_error_prefix`），删除现有
   `LaunchedEffect(error) { delay(3000) }` 空操作。
7. **清理**：删除 `AICreateHabitViewModel copy.kt`（同 FQN 未跟踪副本，编译重复定义）、
   `ConfirmTool.kt`、`AIPrefillHabitHolder`、`ConfirmationDialog`、`NoApiKeyPrompt`。

---

## 11. Token 统计（仅精确值）

> 采用「精确值」方案：**只统计提供商返回的 `usage`，不做估算兜底**。
> 三个预置提供商（DeepSeek / 小米 MiMo / 智谱 GLM）的非流式与流式响应均返回 `usage`，故统计为服务端精确值。
> 端点不返回 `usage` 时该次不计数，展示为「—」，绝不产生近似数字。

### 11.1 数据来源

| 路径 | 来源 | 现状 |
|------|------|------|
| 非流式 | `LLMResult.Success.usage`（`TokenUsage(promptTokens, completionTokens, totalTokens)`） | `LLMClient` 已解析，但 `ConversationManager.processResponse` 丢弃未透传，需透传 |
| 流式 | OpenAI 兼容端点通常在 `[DONE]` 前最后一个 `data:` chunk 携带 `usage`（此时 `choices` 为空） | `chatStream` 目前忽略，需新增 `StreamChunk.Usage(usage)` 事件 |

- 无估算逻辑、无 `isEstimated` 标记、无 `≈` 前缀。

### 11.2 会话累计

- `AIChatViewModel` 持有 `sessionUsage: StateFlow<SessionUsage>`：

```kotlin
data class SessionUsage(
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val totalTokens: Long = 0L,   // = prompt + completion
    val completedCalls: Int = 0   // 成功收到 usage 的调用次数
)
```

- 每次 AI 回复收尾（流式 `Done` 带 `usage` / 非流式返回带 `usage`）时累加；
  **本次无 `usage` 则不计数**（`completedCalls` 也不增）。
- `clearConversation` / 切换提供商时重置会话统计。

### 11.3 展示

- **顶栏常驻计数**：提供商 chip 左侧小字 `1,234 tokens`。无任何计数（`completedCalls == 0`）时
  显示「—」或隐藏该区域，不显示估算值。点击可展开 `DropdownMenu` 显示细分：
  `提示 tokens / 输出 tokens` 与已计入次数。
- **会话统计卡**（可选）：进入会话后第一条 AI 消息前渲染一张小卡，汇总
  「本次会话 token / 习惯创建 / 消息数」；数据来自 `SessionUsage` 派生。
- 建议顶栏计数为默认实现，统计卡作为后续增强。

### 11.4 持久化（可选增强）

- DataStore 新增 `LLM_TOTAL_TOKENS`（long），仅累加实测 usage；每次调用成功后幂等累加，
  在 `NewSettingsAIScreen`（AI 配置列表页）底部显示「累计用量」。
- 属于非阻塞增强，可在基础展示落地后再补。

---

## 12. 常见问题（踩坑）

- **切换提供商后仍在用旧模型？** → 引擎按会话级 `selectedConfigId` 重建；确认没有走
  `rebuildConversation`。中段切换必须重建，不能复用旧 `conversationManager`。
- **原生 tool_calls 流式为空？** → 部分端点流式不吐 `tool_calls` 完整分片；确保 `Done`
  时累积完成再执行，且回退非流式路径（§3.3）。
- **编辑跳转后字段丢失？** → 不要再经 `AIPrefillHabitHolder`；一律按 id 进
  `EditHabit`，由编辑器从 DB 加载。
- **AI 删除了不该删的习惯？** → 删除必须走确认卡；`search_habits` 结果带 id+title 校验回显，
  库中不存在或 title 不匹配则工具报错。
- **`get_settings_status` 每次都要调吗？** → 建议 AI 每次涉及设置操作前调用一次，保证
  读到最新状态；UI 层 `SettingStatusCard` 不做缓存。
- **设置改了没生效？** → 检查 key 是否在白名单且映射到正确的 `UserPreferences.setter`
  （对照 §4.3 表）。
