## ADDED Requirements

### Requirement: 工具注册表
系统 SHALL 提供 `ToolRegistry`，注册以下 9 个工具：`create_habit`、`search_habits`、`edit_habit`、`delete_habit`、`ask_question`、`get_settings_status`、`update_setting`、`open_settings_page`、`reply`。工具执行结果 SHALL 以 `ToolResult.Success` / `ToolResult.Error` 返回，供引擎回灌。

#### Scenario: 注册全部工具
- **WHEN** 引擎启动
- **THEN** 注册表包含上述全部 9 个工具

### Requirement: 创建习惯校验
`create_habit` SHALL 校验：`reminder_times` 每项匹配 `HH:mm` 且去重；`repeat_days` 每项 ∈ 0..6 且去重；WEEKLY 且 `repeat_days` 为空时 SHALL 返回错误并提示 `ask_question`；`title` ≤ 30、`notes` ≤ 200，超限截断并返回提示。

#### Scenario: 非法时间
- **WHEN** `reminder_times` 含非法格式
- **THEN** 工具返回 `ToolResult.Error`

#### Scenario: 周循环缺天
- **WHEN** `repeatCycle` 为 WEEKLY 且 `repeat_days` 为空
- **THEN** 工具返回错误并建议提问澄清

### Requirement: 搜索既有习惯
`search_habits` SHALL 按关键字模糊匹配标题或备注；无关键字时返回最近创建的 10 条。返回 `HabitSearchData(habits)`，每条含 id / title / summary，SHALL 渲染 `HabitPickerCard`（标题 + 摘要 + 编辑 / 删除按钮）。

#### Scenario: 关键字命中
- **WHEN** 用户提供关键字
- **THEN** 工具返回匹配习惯并渲染选择卡

#### Scenario: 无关键字
- **WHEN** 关键字为空
- **THEN** 工具返回最近 10 条习惯

### Requirement: 编辑习惯
`edit_habit` SHALL 校验 `habit_id` 存在且 `title` 与库中一致，否则返回 `ToolResult.Error`。成功后返回 `HabitEditData(habitId)` 并渲染「前往编辑」卡，点击进入 `Route.EditHabit.createRoute(id)`。

#### Scenario: id 校验失败
- **WHEN** `habit_id` 不存在或 title 不匹配
- **THEN** 工具返回 `ToolResult.Error`

#### Scenario: 前往编辑
- **WHEN** 用户点击编辑跳转卡
- **THEN** 系统导航到对应习惯的编辑页，由编辑器从数据库加载完整字段

### Requirement: 删除习惯确认
`delete_habit` SHALL 校验与 `edit_habit` 相同，返回 `HabitDeleteData(habitId, title)` 并渲染删除确认卡。确认后 SHALL 调用 `repository.deleteHabit`（级联删除打卡记录）并回灌成功消息；取消则回灌未删除消息。系统 SHALL NOT 未经确认直接删除习惯。

#### Scenario: 确认删除
- **WHEN** 用户在删除确认卡点击确认
- **THEN** 系统删除习惯与打卡记录，并通知 AI 删除成功

#### Scenario: 取消删除
- **WHEN** 用户取消删除
- **THEN** 系统不删除并通知 AI 未删除

### Requirement: 提问工具
`ask_question` SHALL 复用现有 `QuestionTool`，向用户提问并暂停等待回答。

#### Scenario: 提问
- **WHEN** AI 需要澄清意图
- **THEN** 系统渲染提问卡并暂停等待用户回答

### Requirement: 设置白名单
系统 SHALL 通过 `ControllableSetting` 枚举限定可控制设置：`reminder_enabled`、`dnd_enabled`、`persistent_notification`、`haptic_feedback_enabled`、`show_splash_ad`、`force_tablet_landscape`。AI 配置列表、当前模型、提供商相关设置、免打扰时段、通知模板、调试页与震动参数 SHALL 不在白名单内。

#### Scenario: 白名单外设置
- **WHEN** AI 尝试修改白名单外设置
- **THEN** 工具返回 `ToolResult.Error` 并计入纠错计数

### Requirement: 设置状态与变更
`get_settings_status` SHALL 返回全部可控制设置当前开关状态并渲染 `SettingStatusCard`。`update_setting` 仅接受白名单 key，成功后返回 `SettingChangeData(key, oldValue, newValue)` 并渲染 `SettingChangeCard`（含撤销按钮，撤销调用同一 setter 写回旧值）。

#### Scenario: 查询设置状态
- **WHEN** 用户询问设置状态
- **THEN** 系统渲染设置状态卡列出各设置与当前值

#### Scenario: 修改设置
- **WHEN** AI 修改白名单内设置
- **THEN** 系统渲染变更卡并可一键撤销

### Requirement: 打开设置子页
`open_settings_page` SHALL 仅接受枚举 `page`（notifications / general / about / ai / home），映射到对应 Activity 并渲染 `SettingsNavCard`。非法 page 返回 `ToolResult.Error`。

#### Scenario: 打开通知设置
- **WHEN** `page` 为 "notifications"
- **THEN** 系统渲染导航卡，点击打开 `NewSettingsNotificationsActivity`

#### Scenario: 非法 page
- **WHEN** `page` 不在枚举
- **THEN** 工具返回 `ToolResult.Error` 并计入纠错计数

### Requirement: 回复工具
`reply` SHALL 提供纯文本回复能力（复用现有 `ReplyTool`）。

#### Scenario: 纯文本
- **WHEN** AI 需要直接回复文本
- **THEN** 系统渲染普通助手气泡