package io.github.darrindeyoung791.habitpulse.data.security

import io.github.darrindeyoung791.habitpulse.data.model.AIConfig
import org.junit.Assert.*
import org.junit.Test

class ApiKeyMigrationTest {

    private fun config(
        apiKey: String = "plain-key",
        displayCipher: String = "",
        keyVersion: Int = 0
    ): AIConfig = AIConfig(
        id = "id-1",
        name = "test",
        apiEndpoint = "https://example.com",
        apiKey = apiKey,
        displayCipher = displayCipher,
        keyVersion = keyVersion,
        modelName = "model"
    )

    // fake 加密层：明文 -> "cipher(runtime):cipher(display)"
    private val fakeEncrypt: (String) -> Pair<String, String> = { plain ->
        "runtime[$plain]" to "display[$plain]"
    }

    @Test
    fun `isPlaintext true for keyVersion 0 with non-blank key`() {
        assertTrue(ApiKeyMigration.isPlaintext(config(keyVersion = 0, apiKey = "abc")))
    }

    @Test
    fun `isPlaintext false for blank key even with keyVersion 0`() {
        assertFalse(ApiKeyMigration.isPlaintext(config(keyVersion = 0, apiKey = "")))
    }

    @Test
    fun `isPlaintext false for keyVersion 1`() {
        assertFalse(ApiKeyMigration.isPlaintext(config(keyVersion = 1, apiKey = "ciphertext")))
    }

    @Test
    fun `legacy json without keyVersion defaulted to 0 is treated as plaintext`() {
        // 旧格式 Gson 解析后 keyVersion 默认 0 → 命中明文迁移
        val legacy = config(keyVersion = 0, apiKey = "legacy-plain")
        assertTrue(ApiKeyMigration.isPlaintext(legacy))
    }

    @Test
    fun `plaintext config is encrypted and keyVersion bumped to 1`() {
        val migrated = ApiKeyMigration.encryptIfPlaintext(config(), fakeEncrypt)
        assertEquals("runtime[plain-key]", migrated.apiKey)
        assertEquals("display[plain-key]", migrated.displayCipher)
        assertEquals(1, migrated.keyVersion)
    }

    @Test
    fun `already encrypted config is not double-encrypted`() {
        val encrypted = config(apiKey = "runtime[cipher]", displayCipher = "display[cipher]", keyVersion = 1)
        val result = ApiKeyMigration.encryptIfPlaintext(encrypted, fakeEncrypt)
        assertEquals(encrypted.apiKey, result.apiKey)
        assertEquals(encrypted.displayCipher, result.displayCipher)
        assertEquals(1, result.keyVersion)
    }

    @Test
    fun `blank key config is left unchanged`() {
        val blank = config(apiKey = "", keyVersion = 0)
        val result = ApiKeyMigration.encryptIfPlaintext(blank, fakeEncrypt)
        assertEquals(blank, result)
    }

    @Test
    fun `migration is idempotent when run twice`() {
        val once = ApiKeyMigration.encryptIfPlaintext(config(), fakeEncrypt)
        val twice = ApiKeyMigration.encryptIfPlaintext(once, fakeEncrypt)
        assertEquals(once.apiKey, twice.apiKey)
        assertEquals(once.displayCipher, twice.displayCipher)
        assertEquals(1, twice.keyVersion)
    }

    @Test
    fun `encryption failure keeps original config and does not throw`() {
        val failingEncrypt: (String) -> Pair<String, String> = { throw RuntimeException("keystore down") }
        val original = config()
        val result = ApiKeyMigration.encryptIfPlaintext(original, failingEncrypt)
        assertEquals(original, result)
    }

    @Test
    fun `decryptRuntimeSafely returns plaintext on success`() {
        val result = ApiKeyMigration.decryptRuntimeSafely({ plain -> "decrypted:$plain" }, "runtime[x]")
        assertEquals("decrypted:runtime[x]", result)
    }

    @Test
    fun `decryptRuntimeSafely returns empty string on failure`() {
        val failingDecrypt: (String) -> String = { throw RuntimeException("key invalidated") }
        assertEquals("", ApiKeyMigration.decryptRuntimeSafely(failingDecrypt, "runtime[x]"))
    }
}
