## 1. 字符串资源

- [x] 1.1 在 `app/src/main/res/values/strings.xml` 新增 8 条 key：`ai_config_tab_presets`、`ai_config_tab_custom`、`ai_config_presets_hint`、`ai_preset_deepseek`、`ai_preset_mimo`、`ai_preset_glm`、`ai_preset_free`、`ai_config_info_may_change`
- [x] 1.2 同步以上 8 条到 `app/src/main/res/values-en-rUS/strings.xml`
- [x] 1.3 同步以上 8 条到 `app/src/main/res/values-en-rGB/strings.xml`
- [x] 1.4 同步以上 8 条到 `app/src/main/res/values-zh-rHK/strings.xml`
- [x] 1.5 同步以上 8 条到 `app/src/main/res/values-zh-rTW/strings.xml`

## 2. AIPreset 数据类

- [x] 2.1 新建 `app/src/main/java/io/github/darrindeyoung791/habitpulse/data/model/AIPreset.kt`：`AIPreset(id, nameRes, modelName, apiEndpoint, isFree, defaultStreaming, defaultThinking)` 数据类 + `AIPresets.ALL` 常量表（DeepSeek / 小米 MiMo / 智谱 GLM 三个预置，端点均以 `/chat/completions` 结尾，glm 预置 `isFree=true`）
- [x] 2.2 新增 `AIPresetTest` 单元测试：验证 3 个预置的模型名/端点/免费标记，及所有端点均以 `/chat/completions` 结尾（保证 `ensureChatCompletionsUrl` 透传）

## 3. 页面改造（NewSettingsAIEditScreen.kt）

- [x] 3.1 抽出公共表单段 `@Composable`（名称/端点/模型下拉/密钥/开关/测试连接），以页面级状态为参数，预置与自定义 Tab 复用
- [x] 3.2 新建模式（`configId == null`）在表单上方加 `TabRow`（`预置方案` / `自定义`），`selectedTab` 用 `rememberSaveable`；编辑模式不渲染 Tab
- [x] 3.3 预置方案 Tab：预置卡片组（复用 `SettingsSegmentedItem`，radio 选中态 + GLM「免费」角标）
- [x] 3.4 实现选中预置就地预填：覆盖 `nameInput/endpointInput/modelInput/streamingEnabled/thinkingEnabled`，**保留 `apiKeyInput` 不清空**
- [x] 3.5 预置方案 Tab 展示已选预置摘要（名称/模型/端点，只读行）
- [x] 3.6 两个 Tab 均渲染「配置信息可能随时间调整」提示（`ai_config_info_may_change`）
- [x] 3.7 预置方案 Tab 保留 API 密钥输入、流式/深度思考开关、测试连接（`canSave && !isTesting` 逻辑不变，测试 key 取 `apiKeyInput`）
- [x] 3.8 确认保存逻辑不变：`canSave` 判定、`addAIConfig` 加密、设为活跃、编辑模式 `updateAIConfig` 均不受 Tab 影响

## 4. 验证

- [x] 4.1 用 grep 核对 8 个新字符串 key 在 5 个 `strings.xml` 中均存在
- [x] 4.2 运行 `.\gradlew.bat testDebugUnitTest`，`AIPresetTest` 及既有测试全绿
- [x] 4.3 运行 `.\gradlew.bat assembleDebug` 编译通过
- [ ] 4.4 手动验收：新建页 Tab 切换保留密钥输入；选中预置就地预填；GLM 显示免费角标；预置 Tab 测试连接可用；编辑已有配置不显示 Tab；两个 Tab 均显示提示文案
