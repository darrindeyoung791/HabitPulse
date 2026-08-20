package io.github.darrindeyoung791.habitpulse.data.security

import io.github.darrindeyoung791.habitpulse.data.model.AIConfig

/**
 * 存量明文 apiKey 迁移与运行时解密的纯逻辑，与 Android Keystore 解耦，便于 JVM 单测。
 *
 * 加解密通过函数参数注入（生产代码传 [ApiKeyCrypto]，测试传 fake）。
 */
object ApiKeyMigration {

    /**
     * 明文判定：`keyVersion <= 0` 且 `apiKey` 非空。
     * 旧 JSON 缺少 `keyVersion` 字段时 Gson 默认 0，因此也能命中明文。
     */
    fun isPlaintext(config: AIConfig): Boolean =
        config.keyVersion <= 0 && config.apiKey.isNotBlank()

    /**
     * 若 [config] 为明文则用 [encrypt] 加密（运行时密文 + 展示密文）并回写 `keyVersion = 1`；
     * 否则原样返回。幂等：已加密（keyVersion >= 1）或空白 key 不重复加密。
     * 加密抛异常时保留原配置（迁移失败不阻塞启动）。
     */
    fun encryptIfPlaintext(config: AIConfig, encrypt: (String) -> Pair<String, String>): AIConfig {
        if (!isPlaintext(config)) return config
        return try {
            val (runtimeCipher, displayCipher) = encrypt(config.apiKey)
            config.copy(apiKey = runtimeCipher, displayCipher = displayCipher, keyVersion = 1)
        } catch (e: Exception) {
            config
        }
    }

    /**
     * 解密运行时密文；失败返回空串（密钥失效 / 设备重置等场景，调用方引导用户重新录入）。
     */
    fun decryptRuntimeSafely(decrypt: (String) -> String, runtimeCipher: String): String =
        try {
            decrypt(runtimeCipher)
        } catch (e: Exception) {
            ""
        }
}
