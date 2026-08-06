# AI 提供商预置组合（开箱即用）设计规范

> 本文档描述「添加 AI 配置」页的预置方案设计：在页面内部用 **Tab 切换**「预置方案 / 自定义」，
> 而非二级子页面。改动交互或协议前请先同步更新本文档。
> 关联文档：[ai-unified-chat-interface.md](ai-unified-chat-interface.md)（统一 AI 对话页）。

## 概览

| 项 | 值 |
|----|-----|
| 目标文件 | `ui/screens/settings/NewSettingsAIEditScreen.kt`（仅「添加」模式加 Tab；编辑模式不变） |
| 预置数据 | 新增 `data/model/AIPreset.kt`（纯数据 + 常量表，无 Android 依赖便于单测） |
| 交互结构 | **页面内 Tab**：`预置方案`（3 个开箱即用组合 + 密钥录入）/ `自定义`（现有完整表单） |
| 二级页面 | ❌ 不新增「预置二级页」；原有完整表单即为「自定义」Tab 内容 |
| 密钥加密 | 沿用现有 `ApiKeyCrypto`：新建时输入明文 → `addAIConfig` 加密落库 |
| 保留控件 | 预置 Tab 也保留：流式输出 / 深度思考开关、测试连接 |
| 提示文案 | 新增「配置信息可能随时间变化」提示，两个 Tab 均展示 |

### 与旧方案对比

- 旧方案（已否决）：预置选择页 → 密钥录入二级页 / 自定义二级页，两级跳转，割裂。
- 新方案：单页 Tab，选完预置就地填好，密钥、开关、测试连接都在同一屏，零跳转。

---

## 1. 预置组合定义

| 预置 id | 名称 | 模型 | 端点 | 免费 | 默认流式 | 默认深度思考 |
|---------|------|------|------|------|----------|--------------|
| `deepseek` | DeepSeek | `deepseek-v4-flash` | `https://api.deepseek.com/chat/completions` | 否 | true | false |
| `xiaomi_mimo` | 小米 MiMo | `mimo-v2.5` | `https://api.xiaomimimo.com/v1/chat/completions` | 否 | true | false |
| `glm` | 智谱 GLM | `glm-4.7-flash` | `https://open.bigmodel.cn/api/paas/v4/chat/completions` | 是 | true | false |

```kotlin
data class AIPreset(
    val id: String,
    val name: String,          // 显示名，字符串资源（见 §7），非模型名
    val modelName: String,     // 技术值，不可翻译
    val apiEndpoint: String,   // 技术值，不可翻译
    val isFree: Boolean = false,
    val defaultStreaming: Boolean = true,
    val defaultThinking: Boolean = false
)

object AIPresets {
    val ALL = listOf(
        AIPreset("deepseek", nameRes = R.string.ai_preset_deepseek,
            modelName = "deepseek-v4-flash",
            apiEndpoint = "https://api.deepseek.com/chat/completions"),
        AIPreset("xiaomi_mimo", nameRes = R.string.ai_preset_mimo,
            modelName = "mimo-v2.5",
            apiEndpoint = "https://api.xiaomimimo.com/v1/chat/completions"),
        AIPreset("glm", nameRes = R.string.ai_preset_glm,
            modelName = "glm-4.7-flash",
            apiEndpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            isFree = true)
    )
}
```

要点：

1. **名称用 `nameRes`**：`DeepSeek` / `小米 MiMo` / `智谱 GLM` 是展示名，走本地化；模型名、端点是技术值写死不翻译。
2. **端点均以 `/chat/completions` 结尾**：`LLMConfig.ensureChatCompletionsUrl` 已兼容该形态，原样透传。
3. **GLM 4.7 flash 免费**：仅 `glm` 预置 `isFree = true`，列表项带「免费」角标。
4. 保存时以预置显示名为配置名（`name`），后续可在编辑页改名。

---

## 2. 页面结构（Tab 方案）

### 2.1 总览

```
┌────────────────────────────────────────────────┐
│ TopAppBar: 添加 AI 配置（与编辑模式共用标题）   │
├────────────────────────────────────────────────┤
│ TabRow（仅 add 模式，configId == null 时显示）  │
│   [预置方案]              [自定义]              │
├────────────────────────────────────────────────┤
│ Tab 预置方案                                    │
│  · 提示文案：选择预置方案，自动填入提供商/模型  │
│  · 3 张预置卡片（radio 选中态 + 免费角标）      │
│  · 已选预置摘要（名称/模型/端点，只读）         │
│  · API 密钥输入（新建必填，保存时加密）         │
│  · 流式输出 / 深度思考 开关                     │
│  · 测试连接                                     │
│  · 提示：配置信息可能随时间变化                 │
├────────────────────────────────────────────────┤
│ Tab 自定义（即现有完整表单）                    │
│  · 名称 / 端点 / API 密钥 / 模型下拉            │
│  · 流式输出 / 深度思考 开关                     │
│  · 测试连接                                     │
│  · 提示：配置信息可能随时间变化                 │
├────────────────────────────────────────────────┤
│ 保存 FAB（两 Tab 共用，canSave 统一判定）       │
└────────────────────────────────────────────────┘
```

- **编辑模式（`configId != null`）不显示 Tab**：保持现有单表单不变，避免预置概念干扰编辑语义。

### 2.2 预置方案 Tab

1. **预置卡片**：复用 `SettingsSegmentedItem`（参照 `NewSettingsAIScreen` 的列表项），
   每项 `headline = 预置名称`、`supportingText = 模型 · 端点域名`；
   选中态用 radio/勾选图标 + `selected` 高亮；`glm` 项尾加「免费」角标
   （小 `Surface` + `tintIndex`，样式对齐 `custom-color-scheme.md` 的种子色取色）。
2. **选中行为**：点卡片 → 选中该项并**就地预填**下方字段，不跳转：
   - `nameInput` ← 预置名称
   - `endpointInput` ← 预置端点
   - `modelInput` ← 预置模型名
   - `streamingEnabled` / `thinkingEnabled` ← 预置默认值
   - 已手动输入的 API key 保留不清空；再点其他卡片只覆盖上述 5 项。
3. **预置摘要**：选中后展示一行只读摘要（名称 / 模型 / 端点），点击卡片本身即可切换，摘要无需编辑入口
   （需要改端点/模型的用户去「自定义」Tab）。
4. **密钥录入**：API key 输入框（`PasswordVisualTransformation`），新建时无加密态圆点，
   保存走 `addAIConfig` 自动加密（`ApiKeyCrypto.encryptConfig`）。
5. **开关与测试连接**：完整保留「流式输出」「深度思考」两个 `SettingsSegmentedSwitch`，
   以及「测试连接」`SettingsSegmentedItem`（`canSave && !isTesting` 控制启用）。
   测试连接用的 key 取当前输入明文（新建态无既有密文可解密）。

### 2.3 自定义 Tab

即现有 `NewSettingsAIEditScreen` 的 add 模式表单原样保留：
名称 / 端点 / API 密钥 / 模型下拉（GLM 系列预置项）/ 流式 / 深度思考 / 测试连接 / 保存 FAB。
与预置 Tab 共享同一套表单状态（`nameInput` 等），Tab 切换不丢已填内容。

### 2.4 表单状态归属

- `nameInput / endpointInput / modelInput / apiKeyInput / streamingEnabled / thinkingEnabled`
  均为**页面级状态**（两个 Tab 共享同一份），预置 Tab 的卡片选择只写入这些状态。
- `selectedTab` 用 `rememberSaveable` 保存，进程重建后停留在原 Tab。
- `canSave` 逻辑不变：`name/endpoint/model 非空 && (key 非空)`。

---

## 3. 提示文案

两个 Tab 底部各加一行提示（`bodySmall` + `onSurfaceVariant`，样式同 `ai_settings_notice_*`）：

> 提示：AI 服务商的模型名称、端点等配置信息可能随时间调整，如遇连接失败，请核对或重新选择预置方案。

- 新建/编辑页均显示。
- 可选增强：`NewSettingsAIScreen`（AI 配置列表页）页脚也加同一句提示，便于用户理解已有配置也可能过期。

---

## 4. 新增字符串（5 个语言文件）

| key | 默认（中文） | en-US / en-GB | zh-HK / zh-TW |
|-----|--------------|---------------|---------------|
| `ai_config_tab_presets` | 预置方案 | Presets | 預設方案 |
| `ai_config_tab_custom` | 自定义 | Custom | 自訂 |
| `ai_config_presets_hint` | 选择预置方案，自动填入提供商、模型与端点 | Choose a preset to auto-fill provider, model and endpoint | 選擇預設方案，自動填入提供商、模型與端點 |
| `ai_preset_deepseek` | DeepSeek | DeepSeek | DeepSeek |
| `ai_preset_mimo` | 小米 MiMo | Xiaomi MiMo | 小米 MiMo |
| `ai_preset_glm` | 智谱 GLM | Zhipu GLM | 智譜 GLM |
| `ai_preset_free` | 免费 | Free | 免費 |
| `ai_config_info_may_change` | 提示：AI 服务商的模型名称、端点等配置信息可能随时间调整，如遇连接失败，请核对或重新选择预置方案。 | Note: model names, endpoints and other provider configuration may change over time. If a connection fails, review or reselect a preset. | 提示：AI 服務商的模型名稱、端點等配置資訊可能隨時間調整，如遇連線失敗，請核對或重新選擇預設方案。 |

> 模型名（`deepseek-v4-flash` 等）与端点属技术值，**不**入字符串资源，写死在 `AIPreset` 常量。

---

## 5. 实现变更点

| 文件 | 变更 |
|------|------|
| `data/model/AIPreset.kt`（新增） | `AIPreset` 数据类 + `AIPresets.ALL` 常量表（引用 `R.string.*`，故放 `data/model/` 可被 UI 引用；如需纯 JVM 单测再拆 `nameRes: Int` 之外的部分） |
| `ui/screens/settings/NewSettingsAIEditScreen.kt` | add 模式加 `TabRow`；抽公共表单段；预置卡片组；`selectedTab` 状态；「信息可能变化」提示 |
| `NewSettingsAIEditActivity.kt` | 无改动（add 仍由列表页进入该 Activity） |
| `NewSettingsAIActivity.kt` | 无改动（`onAddConfig` 仍指向 `NewSettingsAIEditActivity`，不传 `EXTRA_CONFIG_ID`） |
| `values/strings.xml` + `values-en-rUS` + `values-en-rGB` + `values-zh-rHK` + `values-zh-rTW` | 补 §4 全部 key |

> 不新增 Activity、不改 `AndroidManifest.xml`、不改导航。因为否决了二级界面。

---

## 6. 使用约定

1. **预置不写回全局**：选预置只是预填表单；保存后才成为一条新配置并设为活跃，逻辑与现有 `addAIConfig` 一致。
2. **默认流式开启、深度思考关闭**：三个预置默认一致；用户可在页内开关覆盖。
3. **Tab 不缓存两套表单**：共享同一份状态，切换不丢已填内容（避免「预置选到一半切自定义」丢 key）。
4. **免费角标仅视觉**：不参与校验、不参与费用计算，仅文案提示。
5. **测试连接使用当前输入值**：新建态 key 一定是明文输入，直接取 `apiKeyInput`，无需解密路径。

---

## 7. 常见问题（踩坑）

- **不要做成二级页面**：用户明确要求页内 Tab；二级跳转会割裂「选模型 → 输密钥」的连续心流。
- **预置 Tab 也要留开关与测试连接**：用户明确要求流式/深度思考/测试连接在预置页可用，
  不要只留「模型 + 密钥」的极简表单。
- **Tab 切换丢失输入**：若两 Tab 各自 `remember` 一份表单状态会互不可见；必须共享页面级状态。
- **端点归一化**：预置端点以 `/chat/completions` 结尾时 `ensureChatCompletionsUrl` 直接透传；
  若将来加 base URL 形态预置，需先过一遍归一化再保存，避免存成不含 `/chat/completions` 的端点在测试连接时报错。
- **技术值不翻译**：模型名/端点放代码常量，只把显示名/提示文案入字符串资源；否则 5 语言文件会散落可变的模型名。
- **`ai_config_info_may_change` 放两个 Tab**：只在「自定义」Tab 放会漏掉预置用户。
