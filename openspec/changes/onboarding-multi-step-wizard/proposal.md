## Why

随着 HabitPulse 功能增多（通知免打扰、AI 创建习惯等），现有单页欢迎界面已不足以让用户在首次使用时了解并配置这些功能。扩展首次引导流程，让用户在进入主界面前完成关键配置，可降低后续使用门槛，提升用户体验。

## What Changes

- 将欢迎页从单页改为三步骤底部导航向导（Step 1: 欢迎/权限说明 → Step 2: 通知/DND 设置 → Step 3: AI 设置）
- Step 1 新增「使用条款」和「隐私政策」文字链接（当前为占位行为）
- Step 2 新增免打扰时段配置（复用现有 DND RangeSlider）
- Step 3 新增 AI 配置（端点、密钥、模型，预填默认值，可跳过）
- 步骤仅可前进不可后退；不同意/跳过均得到支持
- 现有同意/不同意逻辑、受限模式、通知权限请求时机不变

## Capabilities

### New Capabilities
- `onboarding-wizard`: 多步骤首次启动引导向导，包含步骤导航、DND 配置和 AI 配置

### Modified Capabilities
- （无）— DND 和 AI 设置本身行为不变，仅新增引导流程中的配置入口

## Impact

- **WelcomeActivity.kt**: 从单页容器改为多步骤引导管理，管理当前步骤状态
- **WelcomeScreen.kt**: 重构为按步骤渲染不同内容 + 底部步骤指示器
- 新增 `WelcomeAIStep.kt`、`WelcomeNotificationStep.kt`、`WelcomeStepIndicator.kt`
- 字符串资源：所有 locale 新增引导步骤相关字符串
- 现有设置页（SettingsActivity、ReminderSettingsScreen、AISettingsScreen）无需改动，因为共享同一 UserPreferences 数据源
