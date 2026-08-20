## Context

旧版 `AICreateHabitScreen.kt`（1414 行）+ `AICreateHabitViewModel.kt` 是全场景对话助手的前身，但存在多模型缺失、工具协议脆弱、编辑中断上下文、能力单一四大问题（详见 `devdoc/ux-design-guide/ai-unified-chat-interface.md` §「为什么重构」）。该 devdoc 已是新界面的唯一实现参考，定义从「仅 AI 创建习惯」扩展为「创建/编辑/删除习惯 + 开关设置 + 打开设置子页」的全场景助手。

本改变的目标实现方式与 devdoc 略有出入：**不替换、只新增**。旧 `AICreateHabitScreen`、`Route.AICreateHabit`、首页 FAB「AI 创建」入口全部保留，只在旧界面顶栏放一个进新版 `AIChat` 的入口。因此 devdoc 中所有「删除 / 废弃 / 改名旧结构」的条目在本改变里**全部暂缓**，留到未来「全面替换」改变。

## Goals / Non-Goals

**Goals:**

- 落地新版 `AIChatScreen` + `AIChatViewModel`，实现 9 个工具的助手会话。
- 原生 function-calling 协议，支持多轮工具链、流式分片累积、流式降级、深度思考、Token 精确统计。
- 新增 `Route.AIChat`；旧 AI 顶栏新增进入入口。
- 复用现有 `LLMClient` / Room / DataStore / 设置组件体系，不新增依赖。

**Non-Goals:**

- **不删除旧 AI**：`. AICreateHabitScreen`、`AIPrefillHabitHolder`、`ConfirmTool.kt`、`NoApiKeyPrompt`、`ConfirmationDialog`、`Route.AICreateHabit`、首页 FAB 入口全部保留（清理列入未来替换改变）。
- **不写回全局配置**：会话内切换提供商只影响本会话（`LLM_ACTIVE_CONFIG_ID` 不变）。
- **不实现** §11.4 Token 持久化（`LLM_TOTAL_TOKENS` 累计用量）为本次范围（列为可选增强）。
- **不迁移旧消息**：新对话不读取旧 AI 的历史会话（旧 AI 本身无持久会话）。

## Decisions

### D1：新界面独立成 Activity/Screen + 新路由，旧界面不动
- 新增 `Route.AIChat`，在 `HabitPulseNavGraph` 注册目的地与进出场动画（沿用现有 spring slide 风格）。
- `AICreateHabitScreen` 顶栏（`actions`）加一个入口（图标 + contentDescription「新版 AI 对话」），`onClick = navController.navigate(Route.AIChat)`。
- 理由：不与旧界面耦合，风险最小；后续「替换」时只改首页入口指向。
- 备选：直接改首页 FAB 入口 —— 被否，旧 AI 仍需保留与验证。

### D2 消息单一数据源
- `AIChatViewModel.messages: StateFlow<List<ChatMessageUIItem>>` 为唯一数据源；不再维护平行的 `collectedHabits`。
- 卡片自身状态（确认中/已撤销）由 item 内 `remember` 或轻量 `pendingActions` 管理。
- 理由：消除双数据源漂移（旧 bug）。

### D3 原生 function-calling 协议（核心）
- 请求体带 `tools: List<ToolDef>` + `tool_choice: "auto"`；响应读 `tool_calls`；工具结果以 `role=tool` 回灌，循环直到无工具或命中暂停点。
- `ConversationManager` 单轮 while 循环：执行 assistant 的 all tool_calls，`ask_question` / `create_habit` / `delete_habit` 置 `needUserPause` 暂等用户，其余自动续；`ToolResult.Error` 累计 `retryCount`，≥ MAX_RETRIES 触发错误事件。
- 短信封：`Message` / `ChatMessage` 增 `toolCalls` 与 `role="tool"`；`LLMClient` 增 `ToolDef`、`ChatRequest.tools/toolChoice`。
- 流式：累积 `delta.tool_calls`（id/name/arguments 分片）与 `delta.reasoning_content`；`Done` 时空内容或无 tool_calls 时**回退一次非流式**。
- `ResponseParser` 降级为兼容层（`isValidToolName` 等），不进入主流程；`confirm` 从工具集合移除。
- 理由：解决旧正则误解析，`open_settings_page` 等需标准枚举；备选 = 维持 ```json 文本协议，被否（脆弱、无多轮）。

### D4 会话级提供商切换
- VM 持 `selectedConfigId: MutableStateFlow<String?>`，默认读 `activeConfigFlow` 的 id；`rebuildConversation(config)` 重建引擎。
- 无消息直接切；有消息弹确认后重建并清理未确认临时数据。**不写回**全局。
- 顶栏 `ProviderSwitcher`（`AssistChip` 显示 `配置名 · 模型` → DropdownMenu，底部「管理提供商 →」进 `NewSettingsAIActivity`）；无配置时 chip 显「未配置」，点击直接进设置页。
- 理由：解决旧「中段切配置无效」。

### D5 设置白名单工具
- `enum class ControllableSetting(key, labelRes, default)`：6 个开关（reminder_enabled / dnd_enabled / persistent_notification / haptic_feedback_enabled / show_splash_ad / force_tablet_landscape）。
- AI 不可改：AI 配置、当前模型、免打扰时段、通知模板、调试页、震动参数 → 只能 `open_settings_page` 引导。
- `get_settings_status` 返回全状态；现用 → `SettingChangeCard`（变更 + 撤销）；`open_settings_page(enum page)` → `SettingsNavCard`（`startActivity`）。
- 理由：把 AI 会改动严格限定在白名单，杜绝越权。

### D6（原文保留编号）编辑/删除依赖 `Route.EditHabit` 与数据库加载，不复用全局 `AIPrefillHabitHolder`
- 本值得 `search_habits` 定位 → `edit_habit`/`delete_habit` 校验 id+title 回显 → 跳 `Route.EditHabit.createRoute(id)` / 删除确认卡。
- devdoc §5.3 的「删除 `AIPrefillHabitHolder` / 改 `Route.CreateHabit` prefill」**不做**（见 Non-Goal）。
- 新建习惯在本地仅存内存卡片，**确认时才插 DB**（`insertHabit`，`sortOrder` 用现有置顶逻辑），无孤儿行。

### D7 `Token 精确统计`
- 只有流程中都出现 `LLM/SessionUsage(promptTokens, completionTokens, totalTokens)`，`completedCalls`。流式在 `StreamChunk.Usage`（`[DONE]` 前末 chunk）捕获；非流式透传 `LLMResult.Success.usage`（ConversationManager 当前丢弃，补齐）。
- 本次无 `usage` 则不计数；`clearConversation` / 切换重置；顶栏 chip 左侧小字累计；无计数显「—」。
- 理由：三预置（DeepSeek/小米/GLM）都返回 usage，不做估算。

### D8 双 AI 共存兼容
- 修改 `LLMClient` / `ChatRequest` / `ChatMessage` / `ToolRegistry` 时**向后兼容**旧 `AICreateHabitViewModel` 的文本协议：旧 `ConversationManager` 的 ```json Regex 路径保留（新 ConversationManager 新增，旧不改），LLMClient 的 `tool_calls` 解析为新增字段，文本流不变。
- 理由：避免破坏仍在使用的旧 AI。

## Risks / Trade-offs

- **共享 `LLMClient` 改动破坏旧 AI** → 所有改动「新增字段/新增事件」，不改旧行为；对旧路径做回归（编译 + 手动触发文本回复）。
- **新引擎与旧引擎 `ConversationManager` 并存** → 新建 `AIChatConversationManager`（或 `ConversationManager` 加可选的 function-call 模式），避免在同文件堆叠导致旧逻辑漂移；`ToolRegistry` 共享但新注册表新增工具，旧 `confirm` 移除仅影响新。
- **流式端点不吐完整 tool_calls** → 分片累积 + 非流式回退（D3）。
- **Token 可能不返回** → 不计数显「—」，不做估算（D7）。
- **重名 / 多工具并行** → `create_habit` 重度校验（HH:mm、重复天去重、长度截断）与 `ask_question` 辅助澄清。

## Migration Plan

- 本改变不触碰旧 AI 数据结构与存储；无 DB 迁移。
- 新 `Route.AIChat` 独立注册；入口从旧界面顶栏接入。
- 回滚：移除入口与 `Route.AIChat` 注册即可，旧 AI 不受影响。

## Open Questions

- 是否在本改变就实现 §11.4 Token 持久化？（当前：否，可选增强。）
- `AIChatInputBox` 是否复用旧 `AICreateScreen` 内实现（属标准抽取到 components/）？默认：抽取。