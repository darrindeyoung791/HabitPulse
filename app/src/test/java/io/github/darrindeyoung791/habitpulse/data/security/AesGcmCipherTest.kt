package io.github.darrindeyoung791.habitpulse.data.security

import org.junit.Assert.*
import org.junit.Test
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AesGcmCipherTest {

    private fun newKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `encrypt then decrypt restores original plaintext`() {
        val key = newKey()
        val original = "sk-this-is-a-test-api-key-1234567890"
        val encoded = AesGcmCipher.encrypt(key, original)
        assertEquals(original, AesGcmCipher.decrypt(key, encoded))
    }

    @Test
    fun `empty string round trips`() {
        val key = newKey()
        val encoded = AesGcmCipher.encrypt(key, "")
        assertEquals("", AesGcmCipher.decrypt(key, encoded))
    }

    @Test
    fun `unicode plaintext round trips`() {
        val key = newKey()
        val original = "密钥测试-abc-密钥"
        assertEquals(original, AesGcmCipher.decrypt(key, AesGcmCipher.encrypt(key, original)))
    }

    @Test
    fun `output is base64 of iv plus ciphertext`() {
        val key = newKey()
        val encoded = AesGcmCipher.encrypt(key, "hello")
        val decoded = Base64.getDecoder().decode(encoded)
        // 12-byte IV + GCM ciphertext (with 16-byte tag)
        assertTrue("decoded size=${decoded.size}", decoded.size > 12)
        // First 12 bytes are the IV
        assertEquals(12, decoded.copyOfRange(0, 12).size)
    }

    @Test
    fun `fresh iv per encryption produces different ciphertext`() {
        val key = newKey()
        val encoded1 = AesGcmCipher.encrypt(key, "same-plaintext")
        val encoded2 = AesGcmCipher.encrypt(key, "same-plaintext")
        assertNotEquals(encoded1, encoded2)
        // Both still decrypt to the same value
        assertEquals("same-plaintext", AesGcmCipher.decrypt(key, encoded1))
        assertEquals("same-plaintext", AesGcmCipher.decrypt(key, encoded2))
    }

    @Test
    fun `decrypt with wrong key throws recoverable exception`() {
        val encoded = AesGcmCipher.encrypt(newKey(), "secret")
        assertThrows(AesGcmCipherException::class.java) {
            AesGcmCipher.decrypt(newKey(), encoded)
        }
    }

    @Test
    fun `tampered ciphertext throws recoverable exception`() {
        val key = newKey()
        val encoded = AesGcmCipher.encrypt(key, "secret-data")
        val decoded = Base64.getDecoder().decode(encoded)
        decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 0xFF).toByte()
        val tampered = Base64.getEncoder().encodeToString(decoded)
        assertThrows(AesGcmCipherException::class.java) {
            AesGcmCipher.decrypt(key, tampered)
        }
    }

    @Test
    fun `decrypt too short input throws recoverable exception`() {
        val key = newKey()
        assertThrows(AesGcmCipherException::class.java) {
            AesGcmCipher.decrypt(key, Base64.getEncoder().encodeToString(ByteArray(4)))
        }
    }

    @Test
    fun `decrypt invalid base64 throws recoverable exception`() {
        val key = newKey()
        assertThrows(AesGcmCipherException::class.java) {
            AesGcmCipher.decrypt(key, "!!!not-base64!!!")
        }
    }
}
