## Context

当前 LLM 配置以明文 JSON 数组存于 Preferences DataStore（`llm_ai_configs` 键，`AIConfig` Gson 序列化），其中 `apiKey` 为明文。应用已具备多配置管理、切换、测试连接、AI 创建习惯等完整功能，所有读写在 `UserPreferences.kt` 中通过 `aiConfigsFlow` / `getActiveAIConfig()` / `addAIConfig()` / `updateAIConfig()` / `deleteAIConfig()` 收口。

- 运行时消费路径：`AICreateHabitViewModel`（`getActiveAIConfig()`）→ `LLMConfig.toLLMConfig()` → `LLMClient`（`Authorization: Bearer <apiKey>`）。
- UI 展示路径：`NewSettingsAIEditScreen` 的 API key 输入框（当前仅 `PasswordVisualTransformation` 视觉遮蔽，无加密）；`NewSettingsAIScreen` 列表页不显示 key 内容。
- 迁移前置：已存在 `migrateLegacyAiConfig()`（单配置→多配置）先例，新的明文→密文迁移可复用其冷启动钩子思路。

## Goals / Non-Goals

**Goals:**
- API key 在 DataStore 中以密文存放，运行时静默解密用于请求，用户体验无变化。
- 设置页查看 API key 明文需指纹/面容（强生物识别）或系统 PIN/图案凭据验证，验证通过才临时展示，自动隐藏。
- 存量明文 key 冷启动一次性自动加密迁移，无手动干预。
- 全链路由 `UserPreferences` 的既有读写入口收口，不影响多配置管理、测试连接、AI 创建习惯等现有功能。

**Non-Goals:**
- 不实现「应用本身完全拿不到 key」的强隔离（运行时必须解密，进程被攻破即暴露——接受此边界）。
- 不改动 `apiEndpoint`/`modelName` 等非敏感字段的存储方式。
- 不引入全盘加密或 TEE/StrongBox 强制（Keystore 硬件可用则用，否则软件回退）。
- 不做跨设备密钥迁移（密钥不迁移，需在新设备重新录入 key）。

## Decisions

### D1. 双密钥分离：运行时密钥 + 展示密钥

**方案**：Keystore 中建立两把 AES-256 密钥：

| 密钥 | 用途 | `userAuthenticationRequired` | 加密对象 |
|------|------|------------------------------|----------|
| `keyRuntime` | 请求时静默解密 | 否 | `apiKey` 运行时密文 |
| `keyReveal` | 查看时门控解密 | 是（`AUTH_BIOMETRIC_STRONG \| AUTH_DEVICE_CREDENTIAL`） | `apiKey` 展示密文 |

`AIConfig` 中 `apiKey` 字段存**运行时密文**，新增 `displayCipher`（Base64）字段存**展示密文**，新增 `keyVersion`（Int）标记加密版本。

**理由**：同一把密钥无法同时满足「静默可用」和「每次查看都验证」。双密钥让运行时路径零打扰，查看路径由 Keystore 硬件强制每次验证。这是密码管理器的标准做法。

**备选考虑**：
- 单密钥 + 仅 UI 弹生物识别：生物识别只是 UI 遮罩，`keyReveal` 实际不参与解密，安全性低 → 拒绝。
- 生物识别超时窗口（如 30s 内免验证）：后台请求可能刚好卡在验证期，体验不稳定 → 拒绝。
- 仅运行时加密、查看需输主密码：增加额外记忆负担，与现有无密码体系冲突 → 拒绝。

### D2. AES-GCM 加密方案

- 算法：`AES/GCM/NoPadding`，IV 12 字节随机，密钥 256 位。
- Keystore 生成：`KeyGenParameterSpec.Builder`，`setBlockModes(GCM)`、`setEncryptionPaddings(NONE)`、`setKeySize(256)`。
- 运行时密钥别名 `llm_config_runtime`，展示密钥别名 `llm_config_reveal`（展示密钥额外 `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL)`）。
- 密文格式：`Base64(IV + ciphertext)`，IV 不重用（每次加密新随机 IV）。

**理由**：GCM 提供认证加密（防篡改），NIST 推荐，Keystore 原生支持。IV 前置便于解密还原。

**备选**：CBC+HMAC → 需自管 IV/填充/完整性，易错；ChaCha20 非 Keystore 标准支持 → 不选。

### D3. 存储格式升级（BREAKING）

`llm_ai_configs` 的 JSON 数组元素新增字段，旧字段保留：

```json
{
  "id": "...",
  "name": "...",
  "apiEndpoint": "...",
  "apiKey": "<runtimeCipher>",
  "displayCipher": "<revealCipher>",
  "keyVersion": 1,
  "modelName": "...",
  "streamingEnabled": true,
  "thinkingEnabled": false
}
```

**兼容策略**：`decodeConfigs()` 检测 `apiKey` 是否含 `v1:` 前缀（或 `keyVersion` 缺省/为 0）判定是否为明文，是则触发惰性迁移。**不破坏**旧的 `@SerializedName` 映射（`displayCipher`/`keyVersion` 缺失字段在 Gson 中为默认值）。

### D4. 惰性 + 冷启动双重迁移

- **冷启动**（`HabitPulseApplication.onCreate`，复用现有 `migrateLegacyAiConfig()` 位置）：遍历 `llm_ai_configs`，对明文 `apiKey` 执行 `encryptConfig`（生成运行时密文 + 展示密文），回写。
- **惰性兜底**：`getActiveAIConfig()` / `aiConfigsFlow` 读取时若发现未迁移条目，就地加密迁移（防冷启动失败或新写入）。

**理由**：冷启动迁移简单可靠，惰性兜底防遗漏；两者共用同一 `encryptConfig`/`decryptRuntime` 工具，保证幂等。

### D5. 运行时解密入口

在 `UserPreferences` 提供：
- `getActiveAIConfig()`：解密 `apiKey`（用 `keyRuntime`）后返回**明文版** `AIConfig`（供 `toLLMConfig()`/`LLMClient` 使用）。
- `aiConfigsFlow`（UI 列表用）：返回**不解密**的配置列表（`apiKey` 保持密文，UI 不需要明文）。

**理由**：运行时与 UI 需求不同——请求要明文，列表不要明文（避免明文常驻内存/日志）。仅在真正发请求前解密。

**风险**：`activeConfigFlow`（AICreateHabitScreen 用于判断 key 是否为空）需要改判——用 `displayCipher`/`apiKey` 密文是否为空判断「已配置」，不依赖明文。

### D6. 查看门控流程（BiometricPrompt）

`NewSettingsAIEditScreen` API key 输入框改造：
- 常态：显示密文占位（如「已加密（已配置）」）或空，`PasswordVisualTransformation`。
- 点击「查看」→ `BiometricPrompt`（`androidx.biometric`，`ALLOWED_AUTHENTICATORS` = `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`）→ 成功回调中 `decryptReveal(config.displayCipher)` → 明文填入输入框并展示。
- 展示后自动隐藏：退出页面或失去焦点/超时后恢复密文态；`showApiKey` 现有 toggle 改为仅本会话内、展示前必过验证。
- **无生物识别设备**：`BiometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)` 返回 `NONE` 时，查看按钮隐藏或禁用（仅运行时加密仍可用）。

**理由**：BiometricPrompt + `CryptoObject` 在系统级安全通道内解密，密文不落地。`DEVICE_CREDENTIAL` 兜底指纹/面容不可用但设了 PIN 的设备。

### D7. 新依赖

- `androidx.biometric:biometric`（BiometricPrompt 与 CryptoObject）。

**理由**：唯一需要的第三方；Keystore 是平台 API 无需额外依赖。不引入已 deprecated 的 `androidx.security.crypto`。

## Risks / Trade-offs

- **[运行时明文必然短暂存在于内存]** → 接受此边界；解密范围最小化（仅 `getActiveAIConfig()` 发请求前），明文不写入日志、不进入 Flow 长期驻留。
- **[Keystore 密钥被用户清除凭据/重置设备删除]** → 解密抛异常，catch 后提示重新录入 key（走删除重建配置路径）。
- **[设备备份带走密文，但密钥不迁移]** → 备份中 key 不可读；用户在新设备需重新录入（现有配置仍显示但解密失败 → 引导重新填写）。
- **[展示密钥因生物识别策略变更失效（如新增指纹后旧 key 作废）]** → `decryptReveal` 失败时引导删除重建展示密文（重录 key 即可，运行时密文不受影响）。
- **[存量明文迁移失败（如 Keystore 初始化失败）]** → 惰性迁移兜底 + 失败时保留明文并告警，不阻塞启动。
- **[Gson 版本字段漂移]** → 用 `@SerializedName` 固定字段名（沿用现有约定），`keyVersion` 控制格式演进。

## Migration Plan

1. 新增 `data/security/`（Keystore 管理 + AES-GCM + 迁移工具），`build.gradle.kts` 加 `androidx.biometric`。
2. `AIConfig` 加 `displayCipher`/`keyVersion` 字段（`@SerializedName`），`encodeConfigs`/`decodeConfigs` 兼容旧格式。
3. `UserPreferences` 接入运行时解密（`getActiveAIConfig` 解密；`aiConfigsFlow` 保持密文；`activeConfigFlow` 判空改密文判断）+ 冷启动/惰性迁移。
4. `NewSettingsAIEditScreen` 接入展示门控（BiometricPrompt + 查看按钮 + 自动隐藏）。
5. `AICreateHabitScreen`/`AICreateHabitViewModel`/`AiConnectionTester` 用解密后的明文 key，验证全链路。
6. 单元测试（加解密往返、迁移幂等、GCM IV、判空逻辑）+ 回归。

**回滚**：仅代码回滚 + 卸载重装（密文数据不向后兼容明文格式；回滚版本需清理 DataStore）。加解密逻辑纯新增，回滚路径干净。

## Open Questions

- 是否需要在**列表页**（`NewSettingsAIScreen`）也提供「查看 key」入口，还是仅在编辑页？→ 倾向仅编辑页，减少生物识别弹窗频次。
- 明文展示的自动隐藏时机：离开页面 / 失去焦点 / 固定 N 秒？→ 倾向「离开编辑页即隐藏」+ 保留现有手动切换。
- 是否需要 `EncryptedFile` 级别的保护（key 内容较长时走文件而非 DataStore）？→ 当前 key 长度 ≤ 100，DataStore 足够，暂不需要。
