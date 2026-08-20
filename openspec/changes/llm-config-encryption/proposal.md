## Why

LLM 配置列表（`llm_ai_configs`）当前以 **明文 JSON** 存于 Preferences DataStore，其中 `apiKey` 未做任何加密。设备备份/迁移（`adb backup`、系统自动备份）会原样带出该文件，任何能读取应用私有目录的途径都能直接拿到明文密钥，存在泄露风险。用户需要密钥安全存放的同时，仍能在后台正常发起 LLM 请求，并且查看密钥明文时必须通过指纹/面容验证。

## What Changes

- **API key 加密存放**：运行时密钥（Android Keystore AES）加密 `apiKey` 后存入 DataStore，应用发请求时静默解密，无感使用。**BREAKING**：`llm_ai_configs` 存储格式从明文 JSON 升级为「加密密文 + 展示密文 + 元信息」结构。
- **查看需生物识别**：设置页查看 API key 明文需指纹/面容（或系统 PIN）验证，验证通过才解密展示；展示后自动隐藏。
- **存量明文迁移**：冷启动时检测到旧版明文 `apiKey` 自动加密迁移（一次性，成功后旧值不复存在）。
- **无生物识别设备降级**：仅运行时加密可用，查看功能回退为需确认的「显示明文」或按设备能力用系统凭据门控。
- **不改变**：配置列表的增删改查、多配置切换、测试连接、AI 创建习惯等现有功能交互。

## Capabilities

### New Capabilities
- `keystore-encryption`: 基于 Android Keystore 的双密钥加解密基础设施（运行时密钥 + 生物识别门控密钥），及 API key 的加密/解密/迁移
- `biometric-reveal`: 设置页查看 API key 明文的生物识别（指纹/面容/系统凭据）门控流程与 UI

### Modified Capabilities
- `llm-config`: 现有 `apiKey` 明文存储要求升级为「加密存放，查看需生物识别」——变更存储格式与 `AIConfig` 结构；`openspec/changes/ai-create-habit/specs/llm-config/spec.md` 中"encrypted at rest"场景此前未真正实现，本次补齐

## Impact

- **代码**：
  - `data/model/AIConfig.kt` — 结构变更（`apiKey` → 密文字段 + 展示密文 + 版本标记）
  - `data/preferences/UserPreferences.kt` — `llm_ai_configs` 的编码/解码、迁移逻辑、`getActiveAIConfig()`/`add/update/delete` 读写路径
  - `data/security/`（新增）— Keystore 密钥管理、AES-GCM 加解密工具、生物识别门控封装
  - `ui/screens/settings/NewSettingsAIEditScreen.kt` / `NewSettingsAIScreen.kt` — API key 字段加解密读写与查看按钮
  - `ai/llm/AiConnectionTester.kt`、`LLMClient.kt`、`LLMConfig.kt` — 运行时使用路径改为解密后取用
  - `HabitPulseApplication.kt` — 冷启动迁移钩子
- **依赖（新增）**：`androidx.biometric`（BiometricPrompt）
- **数据**：DataStore `user_preferences.preferences_pb` 中 `llm_ai_configs` 值结构变化（一次性迁移）
- **安全**：密钥存 Keystore（硬件保护），设备上仅存密文；备份泄露面从「明文可读」降为「密文不可读」
- **测试**：加解密往返、迁移、指纹门控逻辑单元测试
