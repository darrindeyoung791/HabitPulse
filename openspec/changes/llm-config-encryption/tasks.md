## 1. 依赖与安全基础设施

- [ ] 1.1 `app/build.gradle.kts` 添加 `androidx.biometric:biometric` 依赖
- [ ] 1.2 新增 `data/security/KeystoreManager.kt`：双密钥别名常量（`llm_config_runtime` / `llm_config_reveal`），`KeyGenParameterSpec` 生成/加载 AES-256-GCM 密钥；展示密钥带 `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL)`
- [ ] 1.3 新增 `data/security/AesGcmCipher.kt`：`encrypt(key, plaintext)` → `Base64(IV + ciphertext)`，`decrypt(key, encoded)` 还原；每次新随机 12 字节 IV；解密失败抛出可恢复异常
- [ ] 1.4 新增 `data/security/ApiKeyCrypto.kt`：`encryptConfig(apiKey)` 产出运行时密文 + 展示密文（Pair），`decryptRuntime(cipher)`，`decryptReveal(cipher)`（CryptoObject 门控），`hasAuthenticationMethod()` 检测强生物识别/设备凭据

## 2. 数据模型与存储格式

- [ ] 2.1 `AIConfig.kt` 新增 `displayCipher: String` 与 `keyVersion: Int = 0` 字段，均加 `@SerializedName`；`apiKey` 语义改为运行时密文
- [ ] 2.2 `UserPreferences.encodeConfigs/decodeConfigs` 兼容旧格式（缺 `displayCipher`/`keyVersion` 时默认值，不抛错）

## 3. 迁移与读写接入

- [ ] 3.1 `UserPreferences` 新增 `encryptAndPersistConfigs()` 迁移工具：遍历明文 `apiKey`（`keyVersion == 0`）加密并回写，幂等
- [ ] 3.2 `HabitPulseApplication.onCreate`（复用 `migrateLegacyAiConfig()` 位置）调用冷启动迁移
- [ ] 3.3 `getActiveAIConfig()` 改为：读取后若发现明文则惰性迁移，再 `decryptRuntime` 解密 `apiKey` 返回明文版配置
- [ ] 3.4 `aiConfigsFlow` 保持返回密文版列表（不解密，`apiKey`/`displayCipher` 维持密文）
- [ ] 3.5 `activeConfigFlow` 的判空逻辑改为基于密文（`apiKey.isBlank()` 判定「已配置」），不依赖明文
- [ ] 3.6 `addAIConfig/updateAIConfig` 保存时对 `apiKey` 加密生成运行时密文 + 展示密文再持久化
- [ ] 3.7 `deleteAIConfig` 保持不变（整条删除）

## 4. 查看门控 UI

- [ ] 4.1 `NewSettingsAIEditScreen` 新增 BiometricPrompt 封装：`canAuthenticate` 检测、`CryptoObject` 解密展示密文、成功回调填入明文、失败/取消保持隐藏
- [ ] 4.2 API key 输入框改造：常态显示密文占位 + `PasswordVisualTransformation`；「查看」按钮点击 → BiometricPrompt → 解密展示
- [ ] 4.3 自动隐藏：离开编辑页 / key 失焦 / 超时后恢复密文态；每次查看重新验证
- [ ] 4.4 无生物识别且无设备凭据设备：查看按钮禁用/隐藏，运行时加密仍可用
- [ ] 4.5 解密失败（密钥失效）时引导用户重新录入 key，不崩溃

## 5. 运行时全链路适配

- [ ] 5.1 确认 `AICreateHabitViewModel`（`getActiveAIConfig()` 返回明文）→ `toLLMConfig()` → `LLMClient` Bearer 头链路可用
- [ ] 5.2 `AICreateHabitScreen` 判空逻辑改用密文判定，正常走通
- [ ] 5.3 `AiConnectionTester` 测试连接用解密后明文 key
- [ ] 5.4 编译验证 `.\gradlew.bat :app:compileDebugKotlin`

## 6. 测试与回归

- [ ] 6.1 单元测试：AES-GCM 加解密往返、IV 每次不同、Base64 编解码
- [ ] 6.2 单元测试：`keyVersion == 0` 明文迁移幂等、已加密配置不被二次加密
- [ ] 6.3 单元测试：`activeConfigFlow`/`getActiveAIConfig` 判空与解密逻辑（用 fake 加密层）
- [ ] 6.4 `.\gradlew.bat :app:testDebugUnitTest` 全绿
- [ ] 6.5 真机回归：加密后新建/编辑/测试连接/AI 创建习惯、查看需指纹、迁移旧数据
