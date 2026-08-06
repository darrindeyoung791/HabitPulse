package io.github.darrindeyoung791.habitpulse.data.security

import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM 加解密工具。
 *
 * - 密文格式：`Base64(IV + ciphertext)`，IV 为每次加密新生成的 12 字节随机数。
 * - GCM 提供认证加密，密文被篡改时解密抛出 [AesGcmCipherException]。
 * - 纯 JCA 实现（无 Android 依赖），便于 JVM 单元测试。
 */
object AesGcmCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    /**
     * 加密 [plaintext] 为 `Base64(IV + ciphertext)`。
     */
    fun encrypt(key: SecretKey, plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * 解密 [encoded]（`Base64(IV + ciphertext)`）还原明文。
     *
     * 失败（密钥失效 / 密文损坏 / 认证失败）抛出 [AesGcmCipherException]，
     * 调用方捕获后可引导用户重新录入。
     */
    fun decrypt(key: SecretKey, encoded: String): String {
        try {
            val data = Base64.getDecoder().decode(encoded)
            require(data.size > IV_LENGTH) { "ciphertext too short" }
            val iv = data.copyOfRange(0, IV_LENGTH)
            val cipherText = data.copyOfRange(IV_LENGTH, data.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw AesGcmCipherException("AES-GCM decrypt failed", e)
        } catch (e: IllegalArgumentException) {
            throw AesGcmCipherException("AES-GCM decrypt failed: invalid input", e)
        }
    }
}

/**
 * 可恢复的加解密失败异常（如 Keystore 密钥被清除 / 生物识别策略变更导致密钥失效）。
 */
class AesGcmCipherException(message: String, cause: Throwable? = null) : Exception(message, cause)
