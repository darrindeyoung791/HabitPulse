## Context

当前 HabitPulse 的首次引导为单页 WelcomeScreen，仅展示权限说明和同意/不同意按钮。随着通知免打扰（DND）和 AI 创建习惯功能的加入，用户首次使用时无法了解或配置这些功能，需等进入主界面后在设置页逐一配置。

Issue #14 要求将引导扩展为多步骤流程，让用户在进入主界面前完成关键配置。

## Goals / Non-Goals

**Goals:**
- 从单页改为三步骤底部导航向导（欢迎 → 通知设置 → AI 设置）
- Step 1 保留现有权限说明，新增使用条款/隐私政策链接
- Step 2 支持免打扰时段配置（开关 + RangeSlider）
- Step 3 支持 AI 端点/密钥/模型配置（预填默认值，可跳过）
- 所有配置通过 UserPreferences 持久化，与设置页共享数据源
- 仅首次安装时触发，现有受限模式/通知权限请求逻辑不变

**Non-Goals:**
- 不改动现有 DND 和 AI 设置页的行为和 UI
- 不引入步骤回退导航
- 不修改通知权限请求机制（仍然在 onAgree 后、进 MainActivity 前请求）
- 不实现真实的使用条款和隐私政策页面（仅占位链接）

## Decisions

| 决策 | 方案 | 备选方案 | 理由 |
|---|---|---|---|
| 步骤管理 | WelcomeActivity 中用 `remember { mutableIntStateOf(1) }` 管理当前步骤 | NavHost 多路由 | 避免引入 Navigation Compose 复杂性，三步骤无需路由栈 |
| 步骤 UI 组织 | 每个步骤一个独立 Composable 文件 | 单文件 switch-case | 各步骤 UI 逻辑独立，文件大小可控，便于维护 |
| DND 配置复用 | 将 ReminderSettingsScreen 中的 DndRangeSlider 提取为独立 Composable | 直接复制代码 | 避免重复代码，设置页和引导页共用同一组件 |
| AI 配置简化 | 仅包含 API 端点、密钥、模型三字段 | 完整版 AISettingsScreen | 引导流程应轻量，测试连接和流式开关可在设置页精细调整 |
| TOS/隐私政策 | 文字链接，点击弹出 Snackbar "即将推出" | WebView 占位页 | 最轻量实现，后续可替换为真实页面 |
| 步骤指示器 | Composable `WelcomeStepIndicator` 渲染 "① ② ③" | Text 直接拼接 | 独立组件可复用，视觉统一 |

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| DndRangeSlider 当前与 ReminderSettingsScreen 强耦合，提取可能引入回归 | 提取为独立 Composable 后，ReminderSettingsScreen 改为引用同一组件，通过测试验证 |
| AI 设置与 AISettingsScreen 同时写 UserPreferences 可能覆盖 | 两者写同一 DataStore Key，无冲突；后保存的值覆盖前值，符合预期 |
| 屏幕旋转导致步骤状态丢失 | 使用 `rememberSaveable` 保存当前步骤 |
| 引导步骤突变（未来增减步骤） | 步骤数和编号用常量定义在 WelcomeActivity 中，修改一处即可 |
