## 1. 协议层改造（LLMClient / Message）

- [x] 1.1 新增 `ToolDef` 数据类（name / description / parameters JSON Schema），`ChatRequest` 增 `tools: List<ToolDef>?` 与 `toolChoice: String?`；`ChatResponse`/`Message` 增 `toolCalls`（保留旧文本逻辑）
- [x] 1.2 `LLMClient.chatStream` 累积 `delta.tool_calls` 分片（id/name/arguments），仅在 `Done` 时判定完整；新增 `StreamChunk.Usage` 捕获 `[DONE]` 前末 chunk 的 `usage`；捕获 `delta.reasoning_content`
- [x] 1.3 非流式 `chat` 透传 `usage` 与 `toolCalls`；`ChatMessage` 增加 `role="tool"` 与 `toolCallId` 支持（针对分类新增，不破坏旧文本路径）
- [x] 1.4 单元测试：`ToolDef` 序列化、流式 tool_calls 分片累积、usage 透传、reasoning_content 捕获

## 2. 会话引擎（新增 AIChat 专用引擎）

- [x] 2.1 新增 `AIChatConversationManager`（保留旧 `ConversationManager` 路径）：单轮 while 循环执行全部 tool_calls，结果以 `role=tool` 回灌，直到无工具或命中暂停点
- [x] 2.2 暂停点：`ask_question` / `create_habit` / `delete_habit` 置 `needUserPause` 等待 UI「继续」信号，其余自动续；`reply` / 查询类无卡片自动续
- [x] 2.3 `ToolResult.Error` 累计重试计数，≥ MAX_RETRIES 触发错误事件并停止
- [x] 2.4 流式 `Done` 空内容或无法解析工具时回退一次非流式请求（避免静默失败）
- [x] 2.5 深度思考事件：`ThinkingStarted/Updated/Ended` 按 `reasoning_content` 分片触发，思考关闭时不触发
- [x] 2.6 `ConversationGuard` 扩展：`invalidSettingTries`，设置类工具连续报错 ≥3 触发 `GuardBlocked`；`update_setting` 成功重置
- [x] 2.7 `retryLastTurn`：删除最后一个 assistant 轮与其 `role=tool` 尾消息后重发
- [x] 2.8 单元测试：工具循环、暂停点、重试上限、流式降级、纠错计数

## 3. 工具注册表与工具实现

- [x] 3.1 新增 `ai/tools/` 工具定义与注册表，注册 9 个工具；`ToolRegistry` 覆盖新工具集
- [x] 3.2 `create_habit`：补强校验（`HH:mm` 去重、`repeat_days` 0..6 去重、WEEKLY 缺天报错提问、title≤30/notes≤200 截断）
- [x] 3.3 `search_habits` → `HabitSearchData`（走 `HabitRepository.searchHabitsFlow` first()，按 sortOrder，无关键字最近 10 条）
- [x] 3.4 `edit_habit` / `delete_habit`：校验 id+title 回显，返回编辑跳转/删除确认数据，id 不存在返回 Error
- [x] 3.5 `ControllableSetting` 枚举（6 开关 + labelRes/default）与白名单；`get_settings_status` / `update_setting` / `open_settings_page` 实现，映射到 `UserPreferences` setter 与 Activity
- [x] 3.6 `ask_question` / `reply` 复用现有工具；`ai/tools` 消息结果数据结构（HabitBrief 等）
- [x] 3.7 单元测试：工具校验、设置白名单与错误、状态数据生成

## 4. ViewModel（AIChatViewModel）

- [x] 4.1 新增 `AIChatViewModel`：`messages: StateFlow<List<ChatMessageUIItem>>` 单一数据源，`sessionUsage` 精确统计（仅 usage，无 usage 不计）
- [x] 4.2 `selectedConfigId`（默认取 `activeConfigFlow`），`rebuildConversation(config)` 重建引擎；切换有消息先确认
- [x] 4.3 `sendMessage` 用当前配置校验 `isValid`/`hasApiKeyConfigured`；`clearConversation` 重置消息与 Token
- [x] 4.4 未确认新习惯仅存内存卡片，确认时才 `repository.insertHabit`（无孤儿行）；继续信号与新建全部确认后收尾
- [x] 4.5 `ChatMessageUIItem` 数据模型：各气泡/思考块/提问卡/习惯类卡/设置类卡

## 5. 界面与导航

- [x] 5.1 `Route.kt` 新增 `Route.AIChat`；`HabitPulseNavGraph` 注册目的地与进出场动画
- [x] 5.2 旧 `AICreateHabitScreen` 顶栏新增「新版 AI 对话」入口（图标 + 无障碍描述），`navigate(Route.AIChat)`
- [x] 5.3 新增 `AIChatScreen`：TopAppBar / 消息列表 / 输入框 / 免责声明 / 滚动到底部 FAB；横屏键盘适配（顶栏保留，输入 2 行）
- [x] 5.4 顶栏 `ProviderSwitcher`（AssistChip → DropdownMenu，底部「管理提供商」）；清除对话按钮与确认；输出中标题变化
- [x] 5.5 消息与卡片组件：气泡/ThinkingBlock/QuestionComponent/HabitCreatedCard/HabitPickerCard/HabitEditCard/HabitDeleteCard/SettingStatusCard/SettingChangeCard/SettingsNavCard

## 6. 文案与系统提示词

- [x] 6.1 新增系统提示词 `assets/prompts/chat-system-prompt.md`（tool-calling 专用）；按 devdoc §8 明确工具与设置/编辑/删除规则
- [x] 6.2 `values/` + `values-en-rUS` + `values-en-rGB` + `values-zh-rHK` + `values-zh-rTW` 新增全部界面与卡片文案（含 ProviderSwitcher、清除对话、确认离开、Token 展示、设置白名单名）
- [ ] 6.3 卡片触感：可点击卡片/开关复用 `PressVibrationFeedback`；设置图标用 AccentSeeds 种子色（本次省略——旧 AI 界面亦未接入触感，且不作为验收项；留待后续统一接入）

## 7. 集成与回归

- [x] 7.1 `assembleDebug` 编译通过、`testDebugUnitTest` 全部通过（含旧 AI 相关测试不回归）
- [ ] 7.2 手动回归：旧 `AICreateHabitScreen` 仍可创建习惯；新版入口可进入 `AIChat` 并完成基本对话
- [x] 7.3 更新 `AGENTS.md` / `QWEN.md` 项目上下文（新增 AI Chat 进入「已完成」清单）
- [x] 7.4 验收规格对应：`ai-chat-entry`/`ai-chat-screen`/`ai-chat-conversation`/`ai-chat-tools` 全部 Requirement 逐条过