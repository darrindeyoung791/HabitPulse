## Why

用户创建习惯时需要手动填写多个字段（名称、重复周期、提醒时间、重复星期等），过程繁琐。通过自然语言描述习惯，AI解析意图并逐步引导用户完善信息，可以大幅提升创建效率。

## What Changes

- 新增 AI 设置模块：支持配置 API 端点、API 密钥、模型名称
- 新增 AI 创建习惯界面：对话式交互，AI 解析用户输入并询问必要信息
- 集成现有 HabitRepository：AI 确认后批量保存习惯到数据库
- 复用 HabitCreationScreen：支持预填参数进行编辑
- 修改 HomeScreen FAB：新增 AI 创建入口，与手动创建并列

## Capabilities

### New Capabilities

- `llm-config`: LLM API 配置管理，支持端点、密钥、模型的自定义设置（存储在 UserPreferences）
- `ai-settings-screen`: AI 设置子页面，支持配置 API 端点、API 密钥、模型名称，兼容 OpenAI 格式（如智谱 GLM、DeepSeek 等），默认使用智谱 glm-4.7-flash
- `ai-conversation`: AI 对话引擎，支持工具调用解析、安全守卫、死循环检测
- `ai-ui`: AI 创建习惯界面，包含对话气泡、各类问题组件（时间选择、星期选择、单选卡片等）
- `habit-creation-prefill`: 习惯创建预填机制，支持从 AI 确认卡片跳转到编辑页面

### Modified Capabilities

- `settings-navigation`: 在设置页面新增 AI 设置子页面入口
- `home-navigation`: FAB 入口增加 AI 创建选项

## Impact

- 新增 `ai/` 目录存放 AI 相关代码（llm、tools、conversation、ui 子模块）
- 修改 `UserPreferences` 添加 LLM 配置字段
- 新增 `AISettingsActivity` AI 设置子页面（从 Settings 跳转）
- 修改 `SettingsActivity` 添加 AI 设置入口（NavigationLink）
- 修改 `HomeScreen` FAB 入口
- 复用 `HabitCreationScreen` 接收预填参数
- 新增 `AICreateHabitScreen` 独立页面