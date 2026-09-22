package io.github.darrindeyoung791.habitpulse.data.search

import android.content.Context
import io.github.darrindeyoung791.habitpulse.R

/**
 * 设置搜索结果条目的图标键。
 *
 * 索引数据保持纯 Kotlin（无 Compose / Android 依赖，便于 JVM 单元测试）；
 * 图标键在 UI 层（ui/screens/search）映射为 [androidx.compose.ui.graphics.vector.ImageVector]。
 */
enum class SettingsIcon {
    AI,
    LAN,
    NOTIFICATIONS,
    GENERAL,
    ABOUT,
    HELP,
    LANGUAGE,
    FONT_SCALE,
    TABLET,
    VIBRATION,
    CLEAN,
    ALARM,
    BEDTIME,
    VERSION,
    PERSON,
    DOCUMENT
}

/**
 * 设置搜索结果点击后的跳转目标。
 *
 * - 页面级目标（AI/Notifications/General/About/Language/FontScale/OpenSourceLicenses）
 *   直接启动对应 Activity；
 * - [Help] 打开帮助 WebView；
 * - [LanSync] 复用主页 EntryZone 的局域网同步入口回调（设置页该项本身是占位项）。
 */
sealed class SettingsTarget {
    object AI : SettingsTarget()
    object Notifications : SettingsTarget()
    object General : SettingsTarget()
    object About : SettingsTarget()
    object Help : SettingsTarget()
    object LanSync : SettingsTarget()
    object Language : SettingsTarget()
    object FontScale : SettingsTarget()
    object OpenSourceLicenses : SettingsTarget()
}

/**
 * 已解析本地化字符串的设置搜索索引条目。
 *
 * @param title 条目标题（本地化，按当前应用语言解析）
 * @param supportingText 条目副标题（本地化，可空）
 * @param icon 图标键（UI 层映射为具体图标）
 * @param target 点击跳转目标
 * @param keywords 额外匹配关键词（可空，用于标题/副标题未覆盖的叫法）
 */
data class SettingsSearchEntry(
    val title: String,
    val supportingText: String?,
    val icon: SettingsIcon,
    val target: SettingsTarget,
    val keywords: List<String> = emptyList()
)

/**
 * Omnibox 统一搜索的「系统设置」静态索引。
 *
 * 覆盖设置顶层分类 + 二级条目（Debug 条目与 AI 编辑页内部项不收录）。
 * [filterSettings] 为纯过滤函数，匹配逻辑与习惯/联系人保持一致：
 * 子串包含、忽略大小写；查询词为空时返回全部条目。
 */
object SettingsSearchIndex {

    /**
     * 按查询词过滤设置条目。匹配范围：标题 / 副标题 / 关键词。
     * 查询词为空（或纯空白）时返回全部条目。
     */
    fun filterSettings(
        entries: List<SettingsSearchEntry>,
        query: String
    ): List<SettingsSearchEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return entries
        return entries.filter { entry ->
            entry.title.contains(trimmed, ignoreCase = true) ||
                entry.supportingText?.contains(trimmed, ignoreCase = true) == true ||
                entry.keywords.any { it.contains(trimmed, ignoreCase = true) }
        }
    }

    /**
     * 运行时构建索引：解析本地化字符串并应用设备条件。
     *
     * @param context 用于解析当前语言的条目文案
     * @param showForceTabletLandscapeSwitch 「强制使用平板横屏模式」条目是否收录
     *   （与 SettingsGeneralScreen 的可见性判定一致：非平板横屏时才显示）
     */
    fun buildEntries(
        context: Context,
        showForceTabletLandscapeSwitch: Boolean
    ): List<SettingsSearchEntry> = buildList {
        // ============ 顶层分类 ============
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_category_ai),
                supportingText = context.getString(R.string.settings_category_ai_description),
                icon = SettingsIcon.AI,
                target = SettingsTarget.AI
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_category_lan),
                supportingText = null,
                icon = SettingsIcon.LAN,
                target = SettingsTarget.LanSync
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_notifications),
                supportingText = context.getString(R.string.settings_notifications_description),
                icon = SettingsIcon.NOTIFICATIONS,
                target = SettingsTarget.Notifications
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_category_general),
                supportingText = context.getString(R.string.settings_category_general_description),
                icon = SettingsIcon.GENERAL,
                target = SettingsTarget.General
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_about),
                supportingText = context.getString(R.string.settings_about_description),
                icon = SettingsIcon.ABOUT,
                target = SettingsTarget.About
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_help_button),
                supportingText = context.getString(R.string.settings_help_button_description),
                icon = SettingsIcon.HELP,
                target = SettingsTarget.Help
            )
        )

        // ============ 二级条目：通用 ============
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_language),
                supportingText = null,
                icon = SettingsIcon.LANGUAGE,
                target = SettingsTarget.Language
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_font_scale),
                supportingText = context.getString(R.string.settings_font_scale_description),
                icon = SettingsIcon.FONT_SCALE,
                target = SettingsTarget.FontScale
            )
        )
        if (showForceTabletLandscapeSwitch) {
            add(
                SettingsSearchEntry(
                    title = context.getString(R.string.settings_force_tablet_landscape),
                    supportingText = context.getString(R.string.settings_force_tablet_landscape_description),
                    icon = SettingsIcon.TABLET,
                    target = SettingsTarget.General
                )
            )
        }
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_haptic_feedback),
                supportingText = context.getString(R.string.settings_haptic_feedback_description),
                icon = SettingsIcon.VIBRATION,
                target = SettingsTarget.General
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_clear_webview_cache),
                supportingText = context.getString(R.string.settings_clear_webview_cache_description),
                icon = SettingsIcon.CLEAN,
                target = SettingsTarget.General
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_clear_webview_cookies),
                supportingText = context.getString(R.string.settings_clear_webview_cookies_description),
                icon = SettingsIcon.CLEAN,
                target = SettingsTarget.General
            )
        )

        // ============ 二级条目：通知提醒 ============
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_reminder),
                supportingText = null,
                icon = SettingsIcon.ALARM,
                target = SettingsTarget.Notifications
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_reminder_dnd),
                supportingText = null,
                icon = SettingsIcon.BEDTIME,
                target = SettingsTarget.Notifications
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_persistent_notification),
                supportingText = context.getString(R.string.settings_persistent_notification_description),
                icon = SettingsIcon.NOTIFICATIONS,
                target = SettingsTarget.Notifications
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.notification_template_settings_title),
                supportingText = null,
                icon = SettingsIcon.DOCUMENT,
                target = SettingsTarget.Notifications
            )
        )

        // ============ 二级条目：关于 ============
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_app_version_label),
                supportingText = null,
                icon = SettingsIcon.VERSION,
                target = SettingsTarget.About
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_developer),
                supportingText = null,
                icon = SettingsIcon.PERSON,
                target = SettingsTarget.About
            )
        )
        add(
            SettingsSearchEntry(
                title = context.getString(R.string.settings_open_source_licenses),
                supportingText = context.getString(R.string.settings_open_source_licenses_description),
                icon = SettingsIcon.DOCUMENT,
                target = SettingsTarget.OpenSourceLicenses
            )
        )
    }
}
