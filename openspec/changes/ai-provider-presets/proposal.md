## Why

多 AI 提供商支持上线后，新用户从零配置（填端点、选模型）门槛高、易填错，且大多不熟悉 OpenAI 兼容格式。
提供 3 个开箱即用的预置组合（DeepSeek / 小米 MiMo / 智谱 GLM），让用户选卡片即可填入正确端点与模型、只输 API 密钥即可开始使用。
同时补一条「配置信息可能随时间变化」的提示，避免服务商调整后用户困惑。

## What Changes

- **添加 AI 配置页改为页内 Tab**（仅新建模式）：`[预置方案] [自定义]`，不新增二级页面。
  - **BREAKING**：新建 AI 配置的交互从「直接完整表单」变为「Tab 切换：预置方案 / 自定义」。
- **预置方案 Tab**：3 个开箱即用组合卡片（radio 选中态，GLM 带「免费」角标），选中即就地预填
  名称 / 端点 / 模型 / 流式 / 深度思考默认值；保留 API 密钥输入、流式输出与深度思考开关、测试连接。
- **自定义 Tab**：现有完整表单原样保留（名称 / 端点 / API 密钥 / 模型下拉 / 开关 / 测试连接）。
- **新增 `AIPreset` 数据类**与预置常量表（纯数据，含模型名 / 端点 / 免费标记 / 默认开关）。
- **新增提示文案**「配置信息可能随时间调整」，预置与自定义两个 Tab 均展示。
- 编辑已有配置（`configId != null`）**不加 Tab**，保持现有单表单。
- 新增 8 条字符串资源，同步 5 个语言文件（zh 默认 / zh-HK / zh-TW / en-US / en-GB）。
- 密钥保存沿用现有 `ApiKeyCrypto` 加密链路（`addAIConfig`），不改动。

## Capabilities

### New Capabilities

- `ai-config-presets`: 「添加 AI 配置」页的预置方案选择、页内 Tab 切换、预置信息填写与提示文案行为。

### Modified Capabilities

<!-- 无现有 spec 需求变更（openspec/specs/ 目前仅 doc-readme，与本次无关） -->

## Impact

- **代码**：
  - 新增 `app/src/main/java/io/github/darrindeyoung791/habitpulse/data/model/AIPreset.kt`（数据类 + 预置常量表）
  - 修改 `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/settings/NewSettingsAIEditScreen.kt`（add 模式加 TabRow、预置卡片组、共享表单状态、提示文案）
  - `NewSettingsAIActivity.kt` / `NewSettingsAIEditActivity.kt`：无导航改动（否决二级界面，不新增 Activity）
- **资源**：`values/strings.xml`、`values-en-rUS`、`values-en-rGB`、`values-zh-rHK`、`values-zh-rTW` 各新增 8 条字符串。
- **不影响**：`AIConfig` 结构、`UserPreferences` DataStore schema、`ApiKeyCrypto` 加密、导航路由。
