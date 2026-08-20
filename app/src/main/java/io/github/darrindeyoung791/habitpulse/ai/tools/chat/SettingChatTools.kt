package io.github.darrindeyoung791.habitpulse.ai.tools.chat

import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * 可控制设置白名单。AI 只能改这些开关，其余（AI 配置/时段/模板/调试/震动参数）只能引导用户手动改。
 */
enum class ControllableSetting(
    val key: String,
    val labelRes: Int,
    val default: Boolean
) {
    REMINDER_ENABLED("reminder_enabled", R.string.ai_setting_reminder_enabled, true),
    DND_ENABLED("dnd_enabled", R.string.ai_setting_dnd_enabled, true),
    PERSISTENT_NOTIFICATION("persistent_notification", R.string.ai_setting_persistent_notification, false),
    HAPTIC_FEEDBACK_ENABLED("haptic_feedback_enabled", R.string.ai_setting_haptic_feedback_enabled, true),
    SHOW_SPLASH_AD("show_splash_ad", R.string.ai_setting_show_splash_ad, false),
    FORCE_TABLET_LANDSCAPE("force_tablet_landscape", R.string.ai_setting_force_tablet_landscape, false);

    companion object {
        fun byKey(key: String): ControllableSetting? = values().firstOrNull { it.key == key }
    }
}

/** 全量可控制设置状态 → SettingStatusCard。 */
object GetSettingsStatusChatTool : ChatTool {
    override val name = "get_settings_status"

    override fun spec() = functionSpec(
        name = name,
        description = "返回当前所有可控制设置的开/关状态。在用户询问设置状态或修改设置前，建议先调用一次。"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val prefs = arguments["__prefs"] as? UserPreferences
            ?: return ChatToolResult.Error("内部错误：缺少偏好设置存储")

        val pairs = mutableListOf<SettingPair>()
        for (setting in ControllableSetting.values()) {
            val value = when (setting) {
                ControllableSetting.REMINDER_ENABLED -> prefs.reminderEnabledFlow.first()
                ControllableSetting.DND_ENABLED -> prefs.dndEnabledFlow.first()
                ControllableSetting.PERSISTENT_NOTIFICATION -> prefs.persistentNotificationFlow.first()
                ControllableSetting.HAPTIC_FEEDBACK_ENABLED -> prefs.hapticsEnabledFlow.first()
                ControllableSetting.SHOW_SPLASH_AD -> prefs.showSplashAdFlow.first()
                ControllableSetting.FORCE_TABLET_LANDSCAPE -> prefs.forceTabletLandscapeFlow.first()
            }
            pairs.add(SettingPair(setting.key, setting.labelRes, value))
        }
        return ChatToolResult.Success(SettingsStatusData(pairs))
    }
}

/** 修改一项白名单开关 → SettingChangeCard（可撤销）。 */
object UpdateSettingChatTool : ChatTool {
    override val name = "update_setting"

    override fun spec() = functionSpec(
        name = name,
        description = "修改一项开关设置（仅限白名单 key）。返回变更确认卡，用户可一键撤销。" +
            "修改前建议先调用 get_settings_status 查看当前状态。不能改 AI 配置/模型/时段等，请引导用户。",
        properties = mapOf(
            "key" to mapOf("type" to "string", "enum" to ControllableSetting.values().map { it.key }),
            "value" to mapOf("type" to "boolean")
        ),
        required = listOf("key", "value")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val prefs = arguments["__prefs"] as? UserPreferences
            ?: return ChatToolResult.Error("内部错误：缺少偏好设置存储")
        val key = arguments["key"]?.toString().orEmpty()
        val value = arguments["value"] as? Boolean
            ?: return ChatToolResult.Error("value 必须是布尔值")

        val setting = ControllableSetting.byKey(key)
            ?: return ChatToolResult.Error("不支持修改的设置项，可用 get_settings_status 查看可控制项")

        val oldValue = when (setting) {
            ControllableSetting.REMINDER_ENABLED -> prefs.reminderEnabledFlow.first()
            ControllableSetting.DND_ENABLED -> prefs.dndEnabledFlow.first()
            ControllableSetting.PERSISTENT_NOTIFICATION -> prefs.persistentNotificationFlow.first()
            ControllableSetting.HAPTIC_FEEDBACK_ENABLED -> prefs.hapticsEnabledFlow.first()
            ControllableSetting.SHOW_SPLASH_AD -> prefs.showSplashAdFlow.first()
            ControllableSetting.FORCE_TABLET_LANDSCAPE -> prefs.forceTabletLandscapeFlow.first()
        }
        if (oldValue == value) {
            return ChatToolResult.Success(SettingChangeData(key, setting.labelRes, oldValue, value))
        }

        when (setting) {
            ControllableSetting.REMINDER_ENABLED -> prefs.setReminderEnabled(value)
            ControllableSetting.DND_ENABLED -> prefs.setDndEnabled(value)
            ControllableSetting.PERSISTENT_NOTIFICATION -> prefs.setPersistentNotification(value)
            ControllableSetting.HAPTIC_FEEDBACK_ENABLED -> prefs.setHapticsEnabled(value)
            ControllableSetting.SHOW_SPLASH_AD -> prefs.setShowSplashAd(value)
            ControllableSetting.FORCE_TABLET_LANDSCAPE -> prefs.setForceTabletLandscape(value)
        }
        return ChatToolResult.Success(SettingChangeData(key, setting.labelRes, oldValue, value))
    }
}

/** 打开指定设置子页 → SettingsNavCard。 */
object OpenSettingsPageChatTool : ChatTool {
    override val name = "open_settings_page"

    private val validPages = setOf("notifications", "general", "about", "ai", "home")

    override fun spec() = functionSpec(
        name = name,
        description = "当用户询问某设置在哪个页面、或 AI 无法直接修改时调用。返回一张导航卡片，点击跳转到对应设置子页。" +
            "只能使用枚举 page，禁止打开任意 Activity。",
        properties = mapOf(
            "page" to mapOf("type" to "string", "enum" to validPages.toList())
        ),
        required = listOf("page")
    )

    override suspend fun execute(arguments: Map<String, Any?>): ChatToolResult {
        val page = arguments["page"]?.toString().orEmpty()
        if (page !in validPages) {
            return ChatToolResult.Error("无效的设置页面（page），可选值：${validPages.joinToString()}")
        }
        return ChatToolResult.Success(SettingsNavData(page, page))
    }
}