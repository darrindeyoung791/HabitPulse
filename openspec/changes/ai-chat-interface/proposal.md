## Why

旧版 `AICreateHabitScreen`（1414 行）能力单一且协议脆弱：只能「创建习惯」，依赖 ```json 代码块文本协议解析工具调用，误解析率高；不支持原生 function-calling 的多轮工具链、改/删习惯与设置控制。`devdoc/ux-design-guide/ai-unified-chat-interface.md` 已定义新版统一 AI 对话界面（AI Chat）的设计规范，但目前仅有设计、无实现。

本次改变按规范落地新版 AI 对话界面。**但不替换旧 AI**：旧 `AICreateHabitScreen` 与首页 FAB 入口保持不变，先在旧 AI 创建习惯界面内留一个进入新界面的入口，让新界面先在真实使用中验证，后续再考虑全面替换。

## What Changes

- **新增新版 AI 对话界面 `AIChatScreen` + `AIChatViewModel`**：完整对话助手（创建/编辑/删除习惯 + 开关设置 + 打开设置子页），消息列表由 ViewModel 的 `messages: StateFlow<List<ChatMessageUIItem>>` 单一数据源驱动。
- **原生 function-calling 协议**：请求带 `tools` + `tool_choice: "auto"`，响应用 `tool_calls`，工具结果以 `role=tool` 回灌驱动多轮工具链；流式累积 `delta.tool_calls` 分片、捕获 `reasoning_content`（深度思考）；`Done` 空内容时回退一次非流式。废弃 ```json 代码块文本协议。
- **9 个工具**：`create_habit` / `search_habits` / `edit_habit` / `delete_habit` / `ask_question` / `get_settings_status` / `update_setting` / `open_settings_page` / `reply`；设置控制走白名单（6 个开关），删除习惯必须经 UI 确认卡。
- **顶栏提供商快捷切换器**（会话级，默认全局活跃配置，不写回全局）+ 清除对话。
- **Token 精确统计**：只累计提供商返回的 `usage`，顶栏常驻计数，无估算兜底。
- **新入口**：旧 `AICreateHabitScreen` 顶栏新增「新版 AI 对话」入口（图标/按钮），`navigate(Route.AIChat)`。旧 AI、`Route.AICreateHabit`、首页 FAB 入口全部保留。
- **新增 `Route.AIChat`** 导航路由（带与新界面匹配的进出场动画）。
- **清理暂缓**（本改变不替换旧 AI，故不删除）：`AIPrefillHabitHolder`、`ConfirmTool.kt`、`NoApiKeyPrompt`、`ConfirmationDialog`、旧 `AICreateHabitScreen`。这些属于未来「全面替换」改变的范围。

## Capabilities

### New Capabilities

- `ai-chat-entry`: 新版 AI 对话界面的独立导航路由（`Route.AIChat`），以及旧 `AICreateHabitScreen` 顶栏进入新界面的入口。旧 AI 与首页 FAB 入口保持不变。
- `ai-chat-screen`: 新版对话界面 UI —— 顶栏（返回/标题/提供商切换/清除对话）、消息列表（用户/助手气泡、思考块、提问卡、新建/选择/编辑/删除习惯卡、设置状态/变更/导航卡）、输入框、免责声明、滚动到底部 FAB、横屏与键盘适配。
- `ai-chat-conversation`: 会话引擎 —— 原生 function-calling 协议、流式 `tool_calls` 分片累积、多轮工具链、流式降级、深度思考捕获、`ConversationGuard` 扩展（设置纠错）、Token 精确统计（仅 `usage`）。
- `ai-chat-tools`: 工具注册表与工具定义 —— 9 个工具（习惯类 4 + 提问 1 + 设置类 3 + 回复 1）、设置白名单 `ControllableSetting`、`ToolRegistry` 执行与结果回灌、`HabitPickerCard`/`HabitDeleteCard` 等 UI 卡片的数据结构。

### Modified Capabilities

<!-- 现有 spec 仅 doc-readme，与本改变无关。 -->

## Impact

- **新增文件**：
  - `ui/screens/ai/AIChatScreen.kt`（+ `ui/screens/ai/components/` 下的 ProviderSwitcher、AIChatInputBox、ThinkingBlock、各卡片组件）
  - `viewmodel/AIChatViewModel.kt`
  - 工具定义文件（`ToolDef`、工具注册、`ControllableSetting` 等，放 `ai/tools/`）
  - 消息 UI 数据模型 `ChatMessageUIItem`（含各卡片类型）
- **修改文件**：
  - `navigation/Route.kt`：新增 `Route.AIChat`
  - `navigation/HabitPulseNavGraph.kt`：注册 `Route.AIChat` 目的地 + 进出场动画
  - `ui/screens/ai/AICreateHabitScreen.kt`：顶栏新增新界面入口
  - `ai/llm/LLMClient.kt`：`ChatRequest` 增 `tools`/`tool_choice`、`ToolDef`、流式 `tool_calls` 分片与 `reasoning_content` 解析、`StreamChunk.Usage`
  - `ai/conversation/`：`ChatMessage`/`Message` 增 `tool_calls` 与 `role=tool`、`ConversationManager` 多轮工具循环与暂停点、`ConversationGuard` 设置纠错、`ResponseParser` 降级为兼容层
  - `ai/tools/ToolRegistry.kt`：注册新工具集
  - `assets/prompts/system_prompt.md`：按 §8 更新工具列表与设置/编辑/删除规则
  - `UserPreferences`（若实现 §11.4 可选持久化，则新增 `LLM_TOTAL_TOKENS`）
- **资源**：`values/strings.xml` + `values-en-rUS` + `values-en-rGB` + `values-zh-rHK` + `values-zh-rTW` 新增全部界面与卡片文案。
- **依赖**：无新增依赖（沿用现有 Gson / Room / DataStore / Navigation Compose）。
- **不涉及**：删除旧 AI 相关文件；`Route.AICreateHabit`、首页 FAB「AI 创建」入口。
