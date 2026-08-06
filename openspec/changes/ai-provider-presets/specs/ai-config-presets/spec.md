## ADDED Requirements

### Requirement: 添加 AI 配置页的预置/自定义 Tab 切换
系统在「添加 AI 配置」页（新建模式）顶部提供 `预置方案` 与 `自定义` 两个 Tab，用户可在页内切换，
不跳转二级页面。两个 Tab 共享同一份表单状态，切换不丢失已填内容。

#### Scenario: 新建模式显示 Tab
- **WHEN** 用户从 AI 配置列表页点击「添加 AI 配置」进入新建页
- **THEN** 页面顶部显示 `预置方案` 与 `自定义` 两个 Tab，默认选中 `预置方案`

#### Scenario: 页内切换保留输入
- **WHEN** 用户在 `预置方案` Tab 已输入 API 密钥后切换到 `自定义` Tab
- **THEN** 表单仍显示同一份已填内容，`自定义` Tab 回到该 Tab 时不丢失密钥输入

#### Scenario: 编辑模式不显示 Tab
- **WHEN** 用户编辑一条已有 AI 配置（传入 configId）
- **THEN** 页面不显示 Tab，保持现有单一表单

### Requirement: 预置组合定义
系统提供 3 个开箱即用的预置组合，含显示名、模型名、端点、免费标记与默认开关。

#### Scenario: 三个预置组合可见
- **WHEN** 用户打开 `预置方案` Tab
- **THEN** 显示 DeepSeek（deepseek-v4-flash，https://api.deepseek.com/chat/completions）、
  小米 MiMo（mimo-v2.5，https://api.xiaomimimo.com/v1/chat/completions）、
  智谱 GLM（glm-4.7-flash，https://open.bigmodel.cn/api/paas/v4/chat/completions）三个预置卡片

#### Scenario: GLM 预置带免费角标
- **WHEN** 用户查看 `预置方案` Tab 中的智谱 GLM 卡片
- **THEN** 卡片上显示「免费」角标，其他两个预置不显示

#### Scenario: 预置端点可直接透传
- **WHEN** 用户选中任一预置并保存
- **THEN** 保存的端点即为预置定义中的完整 `/chat/completions` 端点，且 `ensureChatCompletionsUrl` 原样透传

### Requirement: 选中预置就地预填
用户选中一个预置卡片时，系统就地预填名称、端点、模型与开关默认值，不跳转页面。

#### Scenario: 选中预置预填字段
- **WHEN** 用户在 `预置方案` Tab 点击 DeepSeek 卡片
- **THEN** 名称填入 DeepSeek、端点填入 https://api.deepseek.com/chat/completions、
  模型填入 deepseek-v4-flash，流式开关开启、深度思考开关关闭

#### Scenario: 切换预置保留密钥输入
- **WHEN** 用户已输入 API 密钥后点击另一个预置卡片
- **THEN** 名称/端点/模型/开关覆盖为新预置值，已输入的 API 密钥保留不清空

### Requirement: 预置 Tab 保留密钥输入、开关与测试连接
`预置方案` Tab 必须保留 API 密钥输入框、流式输出开关、深度思考开关与测试连接入口，而非仅「模型+密钥」极简表单。

#### Scenario: 预置 Tab 可输入密钥并测试连接
- **WHEN** 用户在 `预置方案` Tab 选中预置、输入 API 密钥
- **THEN** 密钥输入框可编辑，测试连接按钮可用（名称/端点/模型已由预置填好且密钥非空），点击后发起连接测试

#### Scenario: 预置 Tab 可调整流式与深度思考
- **WHEN** 用户在 `预置方案` Tab 切换流式输出或深度思考开关
- **THEN** 开关状态即时生效并随保存写入新配置

### Requirement: 配置信息可能变化的提示
「添加 AI 配置」页的 `预置方案` 与 `自定义` 两个 Tab 均展示一条提示，说明 AI 服务商的配置信息可能随时间调整。

#### Scenario: 两个 Tab 均显示提示
- **WHEN** 用户切换到 `预置方案` Tab 或 `自定义` Tab 滚动到底部
- **THEN** 该 Tab 显示「配置信息可能随时间调整，如遇连接失败请核对或重新选择」类提示文案

### Requirement: 保存为预置配置
用户填写完成点击保存后，系统按现有逻辑新建配置、加密密钥并设为活跃。

#### Scenario: 保存预置配置
- **WHEN** 用户在 `预置方案` Tab 选中预置、输入密钥并点击保存
- **THEN** 系统创建一条名称/端点/模型来自预置、密钥经 `ApiKeyCrypto` 加密的新配置，设为活跃并返回列表页

#### Scenario: 必填不满足时不可保存
- **WHEN** 用户在 `预置方案` Tab 未选中任何预置或未输入 API 密钥
- **THEN** 保存按钮禁用（canSave 判定沿用名称/端点/模型非空且密钥非空）

### Requirement: 本地化
预置显示名、Tab 名、提示文案与免费角标走字符串资源，模型名与端点为技术值不翻译。

#### Scenario: 字符串资源齐全
- **WHEN** 系统构建使用任意受支持语言（zh 默认 / zh-HK / zh-TW / en-US / en-GB）
- **THEN** 预置显示名、Tab 名、提示文案、免费角标均显示对应语言文案，模型名与端点保持原样
