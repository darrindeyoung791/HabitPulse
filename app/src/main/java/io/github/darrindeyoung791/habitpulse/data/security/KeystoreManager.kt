package io.github.darrindeyoung791.habitpulse.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Android Keystore 双密钥管理。
 *
 * 两把 AES-256-GCM 密钥：
 * - [RUNTIME_KEY_ALIAS]：运行时密钥，无用户认证要求，发请求时静默解密。
 * - [REVEAL_KEY_ALIAS]：展示密钥，绑定强生物识别或设备凭据，查看明文时才解密。
 */
object KeystoreManager {

    const val RUNTIME_KEY_ALIAS = "llm_config_runtime"
    const val REVEAL_KEY_ALIAS = "llm_config_reveal"

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE

    /**
     * 生成或加载运行时密钥（无用户认证要求）。
     */
    fun getOrCreateRuntimeKey(): SecretKey =
        getOrCreateKey(RUNTIME_KEY_ALIAS, userAuthenticationRequired = false)

    /**
     * 生成或加载展示密钥（绑定强生物识别 + 设备凭据）。
     */
    fun getOrCreateRevealKey(): SecretKey =
        getOrCreateKey(REVEAL_KEY_ALIAS, userAuthenticationRequired = true)

    private fun getOrCreateKey(alias: String, userAuthenticationRequired: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(256)

        if (userAuthenticationRequired) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    0,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                builder.setUserAuthenticationRequired(true)
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
        }

        generator.init(builder.build())
        return generator.generateKey()
    }
}
