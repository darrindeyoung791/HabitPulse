package io.github.darrindeyoung791.habitpulse.data.search

import org.junit.Assert.*
import org.junit.Test

class SettingsSearchIndexTest {

    private fun entry(
        title: String,
        supportingText: String? = null,
        keywords: List<String> = emptyList(),
        target: SettingsTarget = SettingsTarget.General
    ) = SettingsSearchEntry(
        title = title,
        supportingText = supportingText,
        icon = SettingsIcon.GENERAL,
        target = target,
        keywords = keywords
    )

    private val sample = listOf(
        entry("字体大小", supportingText = "自定义应用内字体大小", target = SettingsTarget.FontScale),
        entry("通知模板", target = SettingsTarget.Notifications),
        entry("帮助与反馈…", supportingText = "查看帮助文档或反馈问题", target = SettingsTarget.Help),
        entry("AI 配置", keywords = listOf("model", "llm"), target = SettingsTarget.AI)
    )

    @Test
    fun `blank query returns all entries`() {
        assertEquals(sample, SettingsSearchIndex.filterSettings(sample, ""))
        assertEquals(sample, SettingsSearchIndex.filterSettings(sample, "   "))
    }

    @Test
    fun `matches title ignoring case`() {
        val result = SettingsSearchIndex.filterSettings(sample, "ai")
        assertEquals(1, result.size)
        assertEquals(SettingsTarget.AI, result.single().target)
    }

    @Test
    fun `matches chinese title substring`() {
        val result = SettingsSearchIndex.filterSettings(sample, "字体")
        assertEquals(1, result.size)
        assertEquals("字体大小", result.single().title)
    }

    @Test
    fun `matches supporting text`() {
        val result = SettingsSearchIndex.filterSettings(sample, "帮助文档")
        assertEquals(1, result.size)
        assertEquals(SettingsTarget.Help, result.single().target)
    }

    @Test
    fun `matches keywords`() {
        val result = SettingsSearchIndex.filterSettings(sample, "LLM")
        assertEquals(1, result.size)
        assertEquals(SettingsTarget.AI, result.single().target)
    }

    @Test
    fun `query is trimmed before matching`() {
        val result = SettingsSearchIndex.filterSettings(sample, "  通知模板  ")
        assertEquals(1, result.size)
        assertEquals(SettingsTarget.Notifications, result.single().target)
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(SettingsSearchIndex.filterSettings(sample, "不存在的条目").isEmpty())
    }

    @Test
    fun `multiple entries can match one query`() {
        val entries = sample + entry("字体缩放", target = SettingsTarget.FontScale)
        val result = SettingsSearchIndex.filterSettings(entries, "字体")
        assertEquals(2, result.size)
    }

    @Test
    fun `matching does not mutate input order`() {
        val result = SettingsSearchIndex.filterSettings(sample, "通知")
        assertEquals(listOf("通知模板"), result.map { it.title })
    }
}
