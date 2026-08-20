## ADDED Requirements

### Requirement: 页面骨架
系统 SHALL 提供 `AIChatScreen`，由三部分构成：顶部 `TopAppBar`、中部消息列表、底部输入区。消息列表 SHALL 仅由 ViewModel 的 `messages: StateFlow<List<ChatMessageUIItem>>` 驱动，且 SHALL NOT 维护平行的 `collectedHabits` 列表。

#### Scenario: 空会话初始状态
- **WHEN** 用户首次进入 `AIChatScreen`
- **THEN** 消息列表为空，输入框可输入，顶栏标题为「AI 助手」

#### Scenario: 消息按顺序渲染
- **WHEN** ViewModel 的 `messages` 更新
- **THEN** 消息列表按顺序渲染对应的气泡或卡片组件

### Requirement: 顶栏
顶栏 SHALL 包含：返回键、标题（AI 输出中显示「AI 正在输出…」）、提供商切换 chip（`ProviderSwitcher`）、清除对话按钮。仅当 `hasMessages` 为真时显示清除对话按钮。返回键在存在消息时 SHALL 弹出「确认离开」对话框；存在未确认的新习惯时 SHALL 附加提示。

#### Scenario: 空会话返回
- **WHEN** 消息列表为空且用户点击返回
- **THEN** 系统直接返回，不弹确认

#### Scenario: 有消息返回
- **WHEN** 消息列表非空且用户点击返回
- **THEN** 系统弹出「确认离开」对话框，确认后返回

#### Scenario: 未确认新习惯提示
- **WHEN** 存在未确认的新习惯卡且用户点击返回
- **THEN** 确认对话框 SHALL 提示「仍有习惯尚未保存」

#### Scenario: 清除对话
- **WHEN** 存在消息且用户点击清除对话
- **THEN** 系统弹出确认对话框，确认后清空消息并重置会话 Token 统计

### Requirement: 提供商快捷切换
顶栏 SHALL 渲染 `AssistChip` 显示当前 `配置名 · 模型`，点击展开 `DropdownMenu`。菜单 SHALL 列出每个已配置 `AIConfig`（名称 + 模型 · 端点 host，当前项带选中标记），底部有「管理提供商 →」项。菜单数据来自 `UserPreferences.aiConfigsFlow`。无任何配置时 chip 显示「未配置」，点击直接进 `NewSettingsAIActivity`。

#### Scenario: 切换提供商空会话
- **WHEN** 会话无消息且用户选择另一配置
- **THEN** 系统直接重建会话引擎并使用新配置

#### Scenario: 切换提供商有消息
- **WHEN** 会话有消息且用户选择另一配置
- **THEN** 系统弹出确认框，确认后重建引擎并清除当前对话与未确认内容

#### Scenario: 管理提供商
- **WHEN** 用户点击菜单底部「管理提供商」
- **THEN** 系统打开 `NewSettingsAIActivity`

### Requirement: 消息与卡片组件
系统 SHALL 支持渲染以下消息类型：用户气泡 `UserChatBubble`、助手气泡 `AIChatBubble`、思考块 `ThinkingBlock`、提问卡 `QuestionComponent`、新建习惯卡 `HabitCreatedCard`、既有习惯选择卡 `HabitPickerCard`、编辑跳转卡 `HabitEditCard`、删除确认卡 `HabitDeleteCard`、设置状态卡 `SettingStatusCard`、设置变更卡 `SettingChangeCard`、设置导航卡 `SettingsNavCard`。

#### Scenario: 新建习惯卡三按钮
- **WHEN** 渲染 `HabitCreatedCard`
- **THEN** 显示 确认 / 编辑 / 删除 三个按钮，点击各自执行定义的语义

#### Scenario: 删除确认
- **WHEN** 用户在 `HabitDeleteCard` 点击确认
- **THEN** 系统删除对应习惯并回灌一条 `role=tool` 成功消息

### Requirement: 输入栏与免责声明
底部输入栏 SHALL 支持发送与停止，横屏下压缩为 2 行。免责声明在页内显示，横屏 + 键盘时 SHALL 隐藏。

#### Scenario: 横屏键盘
- **WHEN** 设备横屏且输入法可见
- **THEN** 顶栏保持显示，输入框最多 2 行，免责声明隐藏

### Requirement: 错误展示
`errorMessage` SHALL 直接渲染为助手气泡（带错误前缀），不通过空操作延迟隐藏。

#### Scenario: 出错
- **WHEN** 会话发生错误
- **THEN** 系统将错误渲染为助手气泡