package io.github.darrindeyoung791.habitpulse.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

/**
 * 验证：
 * - [AIConfig.hasApiKeyConfigured] 基于密文判空（不依赖明文）。
 * - 旧格式 JSON（缺少 displayCipher/keyVersion）经 Gson 解析 + normalizeForStorage 后兼容。
 */
class AIConfigCompatTest {

    private val gson = Gson()
    private val listType = object : TypeToken<List<AIConfig>>() {}.type

    private fun encryptLikeRuntimeCipher() = "AYIC+abc123def456"
    private fun displayCipher() = "DUMMY+xyz789"

    @Test
    fun `hasApiKeyConfigured true when runtime cipher present`() {
        val config = AIConfig(
            id = "id", name = "n", apiEndpoint = "e",
            apiKey = encryptLikeRuntimeCipher(), displayCipher = displayCipher(), keyVersion = 1,
            modelName = "m"
        )
        assertTrue(config.hasApiKeyConfigured())
    }

    @Test
    fun `hasApiKeyConfigured false when runtime cipher blank`() {
        val config = AIConfig(
            id = "id", name = "n", apiEndpoint = "e",
            apiKey = "", displayCipher = displayCipher(), keyVersion = 1,
            modelName = "m"
        )
        assertFalse(config.hasApiKeyConfigured())
    }

    @Test
    fun `legacy plaintext key counts as configured before migration`() {
        // keyVersion 默认 0、apiKey 为明文 —— 迁移前 UI 也应判定为已配置
        val config = AIConfig(
            id = "id", name = "n", apiEndpoint = "e", apiKey = "legacy-plain-key",
            modelName = "m"
        )
        assertTrue(config.hasApiKeyConfigured())
    }

    @Test
    fun `new format json round trips with displayCipher and keyVersion`() {
        val config = AIConfig(
            id = "id-1", name = "glm", apiEndpoint = "https://x",
            apiKey = encryptLikeRuntimeCipher(), displayCipher = displayCipher(), keyVersion = 1,
            modelName = "glm-4-flash", streamingEnabled = true, thinkingEnabled = false
        )
        val json = gson.toJson(config)
        val parsed = gson.fromJson<AIConfig>(json, AIConfig::class.java)
        assertEquals(encryptLikeRuntimeCipher(), parsed.apiKey)
        assertEquals(displayCipher(), parsed.displayCipher)
        assertEquals(1, parsed.keyVersion)
        assertEquals("glm", parsed.name)
    }

    @Test
    fun `old format json missing displayCipher and keyVersion is normalized`() {
        val oldJson = """
            [
              {
                "id": "legacy-1",
                "name": "默认配置",
                "apiEndpoint": "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                "apiKey": "sk-legacy-plaintext",
                "modelName": "glm-4-flash-250414",
                "streamingEnabled": true,
                "thinkingEnabled": false
              }
            ]
        """.trimIndent()
        val parsed = gson.fromJson<List<AIConfig>>(oldJson, listType)
        assertEquals(1, parsed.size)
        val config = parsed[0]

        // 缺字段时 Gson 运行时把 displayCipher 置 null（Kotlin 静态类型为非空，需 as String? 验证）
        @Suppress("USELESS_CAST")
        val rawDisplayCipher = config.displayCipher as String?
        assertNull("old format should lack displayCipher at runtime", rawDisplayCipher)
        assertEquals(0, config.keyVersion)
        assertEquals("sk-legacy-plaintext", config.apiKey)
        assertTrue(config.hasApiKeyConfigured())

        // 修复为默认值后重序列化仍保持新格式字段
        val normalized = config.copy(displayCipher = rawDisplayCipher ?: "")
        val json = gson.toJson(listOf(normalized))
        assertTrue(json.contains("\"displayCipher\""))
        assertTrue(json.contains("\"keyVersion\""))
    }
}
