## ADDED Requirements

### Requirement: 独立导航路由
系统 SHALL 新增 `Route.AIChat` 导航路由，并在 `HabitPulseNavGraph` 中注册该目的地。该目的地 SHALL 渲染新版 AI 对话界面 `AIChatScreen`，并与现有界面风格一致的进出场动画（spring 垂直滑入 + 淡入）。

#### Scenario: 注册路由
- **WHEN** 用户从旧 AI 创建界面点击「新版 AI 对话」入口
- **THEN** 系统导航到 `Route.AIChat` 并显示 `AIChatScreen`

#### Scenario: 返回
- **WHEN** 用户在 `AIChatScreen` 点击返回键
- **THEN** 系统返回上一目的地（旧 AI 创建界面或首页），旧界面保持原状态

### Requirement: 旧 AI 界面入口
旧 `AICreateHabitScreen` 顶栏 SHALL 提供进入新版 AI 对话界面的入口（图标按钮，带可访问性描述）。该入口 SHALL 通过 `navController.navigate(Route.AIChat)` 跳转。

#### Scenario: 从旧界面进入新版
- **WHEN** 用户在旧 AI 创建界面顶栏点击新界面入口
- **THEN** 系统打开 `AIChatScreen`，旧界面不销毁

#### Scenario: 无障碍描述
- **WHEN** 无障碍服务聚焦入口按钮
- **THEN** 系统播报入口的可访问性文本

### Requirement: 保留旧 AI 与既有入口
系统 SHALL 保留旧 `AICreateHabitScreen`、`Route.AICreateHabit` 以及首页 FAB「AI 创建」入口的完整功能，本改变不删除、不修改其行为。

#### Scenario: 旧入口仍可用
- **WHEN** 用户在首页 FAB 选择「AI 创建」
- **THEN** 系统仍打开旧 `AICreateHabitScreen`，与改动前行为一致
