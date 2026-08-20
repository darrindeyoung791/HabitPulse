package io.github.darrindeyoung791.habitpulse.data.model

import org.junit.Assert.*
import org.junit.Test

class AIPresetTest {

    @Test
    fun `predefines three preset providers`() {
        assertEquals(3, AIPresets.ALL.size)
        assertEquals(
            listOf("deepseek", "xiaomi_mimo", "glm"),
            AIPresets.ALL.map { it.id }
        )
    }

    @Test
    fun `deepseek preset values`() {
        val p = AIPresets.ALL.first { it.id == "deepseek" }
        assertEquals("deepseek-v4-flash", p.modelName)
        assertEquals("https://api.deepseek.com/chat/completions", p.apiEndpoint)
        assertFalse(p.isFree)
    }

    @Test
    fun `xiaomi mimo preset values`() {
        val p = AIPresets.ALL.first { it.id == "xiaomi_mimo" }
        assertEquals("mimo-v2.5", p.modelName)
        assertEquals("https://api.xiaomimimo.com/v1/chat/completions", p.apiEndpoint)
        assertFalse(p.isFree)
    }

    @Test
    fun `zhipu glm preset values and is free`() {
        val p = AIPresets.ALL.first { it.id == "glm" }
        assertEquals("glm-4.7-flash", p.modelName)
        assertEquals("https://open.bigmodel.cn/api/paas/v4/chat/completions", p.apiEndpoint)
        assertTrue(p.isFree)
    }

    @Test
    fun `only glm preset is free`() {
        assertEquals(1, AIPresets.ALL.count { it.isFree })
    }

    @Test
    fun `preset defaults to streaming on and thinking off`() {
        AIPresets.ALL.forEach {
            assertTrue(it.defaultStreaming)
            assertFalse(it.defaultThinking)
        }
    }

    @Test
    fun `all preset endpoints end with chat completions for passthrough`() {
        AIPresets.ALL.forEach {
            assertTrue(
                "endpoint must end with /chat/completions: ${it.apiEndpoint}",
                it.apiEndpoint.endsWith("/chat/completions")
            )
        }
    }

    @Test
    fun `all presets have non blank models endpoints and name resources`() {
        AIPresets.ALL.forEach {
            assertTrue(it.modelName.isNotBlank())
            assertTrue(it.apiEndpoint.isNotBlank())
            assertTrue(it.nameRes != 0)
        }
    }
}