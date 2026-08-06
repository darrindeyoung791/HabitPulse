## Context

「添加 AI 配置」页当前是单一完整表单（`NewSettingsAIEditScreen` add 模式：名称 / 端点 / API 密钥 /
模型下拉 / 流式 / 深度思考 / 测试连接）。多提供商支持后，新用户需手动填 OpenAI 兼容端点与模型名，
门槛高且易填错。本变更引入 3 个开箱即用预置组合，并把新建页改为页内 Tab（预置方案 / 自定义），
同时补一条「配置信息可能随时间调整」的提示。编辑模式（传入 configId）保持现有单表单不变。

参考设计文档：`devdoc/ux-design-guide/ai-provider-presets.md`（含完整预置表、页面骨架、字符串清单与踩坑）。

## Goals / Non-Goals

**Goals:**

- 添加 AI 配置页在新建模式下提供 `预置方案 / 自定义` 两个 Tab，页内切换，不新增二级页面。
- 预置方案 Tab：3 个组合卡片（DeepSeek / 小米 MiMo / 智谱 GLM，GLM 带免费角标），选中就地预填。
- 预置方案 Tab 保留 API 密钥输入、流式输出 / 深度思考开关、测试连接。
- 两个 Tab 均显示「配置信息可能随时间调整」提示。
- 新增 `AIPreset` 数据类 + 预置常量表；密钥沿用现有 `ApiKeyCrypto` 加密链路。
- 新增 8 条字符串，同步 5 个语言文件。

**Non-Goals:**

- 不修改 `AIConfig` 结构、`UserPreferences` DataStore schema、`ApiKeyCrypto`、导航路由。
- 不新增 Activity / 不改 `AndroidManifest.xml`（否决二级界面）。
- 不涉及 AI 对话页（`AIChatScreen`）重构，那是独立变更。
- 不实现 token 统计相关的会话层改动（本变更仅改动配置页；token 统计在对话页变更范畴）。
- 编辑模式不加 Tab。

## Decisions

### 1. 用「页内 Tab」而非二级页面
在 `NewSettingsAIEditScreen` 内部用 `scrollableTabRow`（或 `TabRow`）呈现 `[预置方案] [自定义]`，
依据 `configId == null` 门控只在新建模式显示。
- **备选（否决）**：预置选择页 → 二级密钥页 / 自定义二级页。割裂「选模型 → 输密钥」心流，跳转多次。
- **备选（否决）**：仅一个页面顶部堆预置区 + 表单区。未能清晰区分「预置填写」与「自由自定义」两种心智，且难做 Tab 语义；用户明确要求 Tab。

### 2. 共享页面级表单状态
`nameInput / endpointInput / modelInput / apiKeyInput / streamingEnabled / thinkingEnabled` 保持页面级
`remember`/`rememberSaveable` 状态，两个 Tab 共用同一份，Tab 切换不丢已填内容。
- 备选：每 Tab 各自 `remember` → 切换丢输入，与需求冲突，否决。

### 3. `AIPreset` 纯数据类 + `AIPresets.ALL` 常量表
新增 `data/model/AIPreset.kt`：字段 `id / nameRes / modelName / apiEndpoint / isFree / defaultStreaming / defaultThinking`，
常量表引用 `R.string.*` 作显示名。模型名与端点是技术值，不入字符串资源，避免 5 语言文件散落可变量。
- 备选：把预置写死在 `NewSettingsAIEditScreen` 内局部 list → 无法复用、难单测，否决。

### 4. 预置选中 = 就地预填，不改后端语义
点卡片只写表单状态（覆盖 5 项，**保留已输密钥**）；保存逻辑不变：新建 `AIConfig` → `addAIConfig`
（内部 `ApiKeyCrypto.encryptConfig`）→ 设为活跃。不写回全局 active，注册成新配置再设为活跃。

### 5. 测试连接在预置 Tab 沿用现有实现
新建态密钥必为明文，`AiConnectionTester.test(context, endpointInput, apiKeyInput, modelInput)` 直接取当前输入，
无既有密文需解密，无需新增解密分支。

## Risks / Trade-offs

- [两个 Tab 重复渲染表单造成代码膨胀] → 抽公共表单段（`@Composable SectionForm(...)`），预置 Tab 传预填状态与摘要头，自定义 Tab 传空表单与模型下拉；避免复制粘贴两份字段。
- [Tab 切换丢输入] → 统一页面级状态，Tab 只用 `rememberSaveable selectedTab` 选索引，杜绝每 Tab 独立状态。
- [预置端点归一化问题] → 三个预置端点均已 `/chat/completions` 结尾，`ensureChatCompletionsUrl` 原样透传；若将来新增 base-url 形态预置，需先过归一化再保存（见设计文档踩坑）。
- [字符串遗漏某语言文件] → 验收用 `rg` 核对 8 个新 key 存在于 5 个 `strings.xml`；`localeFilters` 已有 5 语言，无需改。