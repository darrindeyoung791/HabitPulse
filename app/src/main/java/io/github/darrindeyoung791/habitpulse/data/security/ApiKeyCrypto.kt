package io.github.darrindeyoung791.habitpulse.data.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key 加解密入口，串联 [KeystoreManager] 与 [AesGcmCipher]。
 *
 * - [encryptConfig]：用运行时密钥与展示密钥各加密一次，产出 (运行时密文, 展示密文)。
 * - [decryptRuntime]：用运行时密钥静默解密（发请求用）。
 * - [decryptReveal]：用展示密钥解密，需先通过 BiometricPrompt 的 CryptoObject 完成认证。
 * - [hasAuthenticationMethod]：检测设备是否具备强生物识别或设备凭据。
 */
object ApiKeyCrypto {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128

    /**
     * 用运行时密钥 + 展示密钥各加密一次。
     *
     * @return Pair(运行时密文, 展示密文)
     */
    fun encryptConfig(apiKey: String): Pair<String, String> {
        val runtimeCipher = AesGcmCipher.encrypt(KeystoreManager.getOrCreateRuntimeKey(), apiKey)
        val revealCipher = AesGcmCipher.encrypt(KeystoreManager.getOrCreateRevealKey(), apiKey)
        return runtimeCipher to revealCipher
    }

    /**
     * 用运行时密钥解密（无用户认证要求）。
     */
    fun decryptRuntime(runtimeCipher: String): String {
        return AesGcmCipher.decrypt(KeystoreManager.getOrCreateRuntimeKey(), runtimeCipher)
    }

    /**
     * 为「查看 API key」准备一个绑定展示密钥的解密 Cipher，交给 BiometricPrompt 作为 CryptoObject。
     *
     * 认证成功后，用 [finishRevealDecrypt] 完成解密。若密钥失效返回 null，调用方引导用户重新录入。
     */
    fun createRevealDecryptCipher(displayCipher: String): Cipher? {
        return try {
            val key = KeystoreManager.getOrCreateRevealKey()
            val data = java.util.Base64.getDecoder().decode(displayCipher)
            require(data.size > 12) { "ciphertext too short" }
            val iv = data.copyOfRange(0, 12)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 在 BiometricPrompt 认证成功后，用已授权的 Cipher 完成展示密文解密。
     */
    fun finishRevealDecrypt(cipher: Cipher, displayCipher: String): String {
        val data = java.util.Base64.getDecoder().decode(displayCipher)
        val cipherText = data.copyOfRange(12, data.size)
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    /**
     * 设备是否具备强生物识别或设备凭据（用于决定查看按钮可用性）。
     */
    fun hasAuthenticationMethod(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    /**
     * 构造 BiometricPrompt 的 PromptInfo：强生物识别 + 设备凭据兜底。
     *
     * 注意：允许 `DEVICE_CREDENTIAL` 时系统会自动提供「使用设备凭据」入口，
     * 此时**不能**设置 negative button（否则 `PromptInfo.Builder.build()` 抛
     * `IllegalArgumentException: Negative text must not be set if device credential
     * authentication is allowed`）。仅 API 29 及以下（不向 prompt 注册设备凭据）才设置。
     */
    fun createRevealPromptInfo(
        title: CharSequence,
        subtitle: CharSequence?,
        negativeButtonText: CharSequence
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ).build()
        } else {
            builder.setNegativeButtonText(negativeButtonText).build()
        }
    }

    /**
     * 工具方法：当前 Keystore 中的两把密钥别名（供调试展示）。
     */
    fun keyAliases(): List<String> = listOf(
        KeystoreManager.RUNTIME_KEY_ALIAS,
        KeystoreManager.REVEAL_KEY_ALIAS
    )

    /**
     * 供编译期确认 CryptoObject 支持的类型（当前使用 Cipher）。
     */
    @Suppress("unused")
    private fun supportedCryptoTypes(): List<Class<*>> = listOf(
        Cipher::class.java,
        Signature::class.java
    )
}
