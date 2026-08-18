package io.github.darrindeyoung791.habitpulse.data.preferences

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.AIConfig
import io.github.darrindeyoung791.habitpulse.data.security.ApiKeyCrypto
import io.github.darrindeyoung791.habitpulse.data.security.ApiKeyMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 用户偏好设置存储
 *
 * 使用 DataStore 存储用户设置，支持异步读取和写入
 */

// DataStore 实例（扩展属性）
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 用户偏好设置键
 */
object PreferencesKeys {
    /**
     * 是否开启开屏广告以支持 HabitPulse
     */
    val SHOW_SPLASH_AD = booleanPreferencesKey("show_splash_ad")

    /**
     * 是否强制使用平板横屏模式
     */
    val FORCE_TABLET_LANDSCAPE = booleanPreferencesKey("force_tablet_landscape")

    /**
     * 是否开启应用内全部震动反馈
     * 默认值为 true（开启）
     */
    val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")

    /**
     * 是否跟随系统字体大小
     * 默认值为 true（跟随系统）
     */
    val FONT_SCALE_FOLLOW_SYSTEM = booleanPreferencesKey("font_scale_follow_system")

    /**
     * 自定义字体大小缩放值（1.0 为标准大小），仅在 FONT_SCALE_FOLLOW_SYSTEM 为 false 时生效
     */
    val FONT_SCALE = floatPreferencesKey("font_scale")

    /**
     * 是否开启持久通知以保持后台运行
     */
    val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")

    /**
     * LLM 配置列表（JSON 数组，元素为 AIConfig）
     */
    val LLM_AI_CONFIGS = stringPreferencesKey("llm_ai_configs")

    /**
     * 当前使用的 LLM 配置 id
     */
    val LLM_ACTIVE_CONFIG_ID = stringPreferencesKey("llm_active_config_id")

    // ---- 旧版单配置键（仅供一次性迁移读取，迁移完成后物理删除，不再被业务使用） ----
    @Deprecated("已迁移到 LLM_AI_CONFIGS，仅用于 migrateLegacyAiConfig 一次性读取")
    val LEGACY_LLM_API_ENDPOINT = stringPreferencesKey("llm_api_endpoint")

    @Deprecated("已迁移到 LLM_AI_CONFIGS，仅用于 migrateLegacyAiConfig 一次性读取")
    val LEGACY_LLM_API_KEY = stringPreferencesKey("llm_api_key")

    @Deprecated("已迁移到 LLM_AI_CONFIGS，仅用于 migrateLegacyAiConfig 一次性读取")
    val LEGACY_LLM_MODEL_NAME = stringPreferencesKey("llm_model_name")

    @Deprecated("已迁移到 AIConfig.streamingEnabled，仅用于 migrateLegacyAiConfig 一次性读取")
    val LEGACY_LLM_STREAMING_RESPONSE = booleanPreferencesKey("llm_streaming_response")

    /**
     * 是否开启习惯提醒通知（每30分钟）
     */
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")

    /**
     * 是否开启免打扰模式
     */
    val DND_ENABLED = booleanPreferencesKey("dnd_enabled")

    /**
     * 免打扰开始时间（HH:mm 格式）
     */
    val DND_START_TIME = stringPreferencesKey("dnd_start_time")

    /**
     * 免打扰结束时间（HH:mm 格式）
     */
    val DND_END_TIME = stringPreferencesKey("dnd_end_time")

    /**
     * 下次提醒闹钟时间（epoch millis），用于子页面显示
     */
    val NEXT_ALARM_TIME = longPreferencesKey("next_alarm_time")

    /**
     * 上次发送提醒的日期（yyyy-MM-dd），用于每日计数
     */
    val REMINDER_SENT_DATE = stringPreferencesKey("reminder_sent_date")

    /**
     * 今日已发送提醒次数
     */
    val REMINDER_SENT_COUNT = longPreferencesKey("reminder_sent_count")

    /**
     * 通知监督人内容模板
     */
    val NOTIFICATION_TEMPLATE = stringPreferencesKey("notification_template")

    /**
     * 按压震动时长（毫秒），默认 25
     */
    val PRESS_VIBRATION_DURATION_MS = longPreferencesKey("press_vibration_duration_ms")

    /**
     * 按压震动强度（1-255），默认 128
     */
    val PRESS_VIBRATION_AMPLITUDE = intPreferencesKey("press_vibration_amplitude")

    /**
     * AI 工具调用失败的最大自动重试次数，默认 20
     */
    val AI_TOOL_RETRY_LIMIT = intPreferencesKey("ai_tool_retry_limit")

    /**
     * AI 单次最大输出 Token，默认 5000（全局影响所有模型）
     */
    val AI_MAX_OUTPUT_TOKENS = intPreferencesKey("ai_max_output_tokens")

    /**
     * AI 最大思考预算（reasoning tokens），默认 5000；0 表示不发送思考预算参数
     */
    val AI_MAX_THINKING_TOKENS = intPreferencesKey("ai_max_thinking_tokens")
}

/**
 * Check if TalkBack is enabled
 *
 * Note: We only check touch exploration mode which is specific to TalkBack,
 * not other accessibility services that users might have enabled.
 */
private fun isTalkBackEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    // Only check touch exploration mode (TalkBack specific)
    // Don't use isEnabled as it returns true for ANY accessibility service
    return accessibilityManager.isTouchExplorationEnabled
}

/**
 * 用户偏好设置管理器
 *
 * 提供单例访问入口，封装 DataStore 操作
 */
class UserPreferences(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null

        private val gson = Gson()
        private val configListType = object : TypeToken<List<AIConfig>>() {}.type

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * 把旧版单配置键只读合成为一条「默认配置」；旧键无有效值时返回 null。
     * 供迁移完成前的读取兜底与一次性迁移复用。
     */
    private fun synthesizeLegacyConfig(preferences: Preferences): AIConfig? {
        val legacyEndpoint = preferences[PreferencesKeys.LEGACY_LLM_API_ENDPOINT]
        val legacyKey = preferences[PreferencesKeys.LEGACY_LLM_API_KEY]
        val legacyModel = preferences[PreferencesKeys.LEGACY_LLM_MODEL_NAME]
        val legacyStreaming = preferences[PreferencesKeys.LEGACY_LLM_STREAMING_RESPONSE]
        if (legacyEndpoint == null && legacyKey.isNullOrBlank() && legacyModel == null) return null
        return AIConfig(
            id = UUID.randomUUID().toString(),
            name = context.getString(R.string.ai_config_migrated_name),
            apiEndpoint = legacyEndpoint ?: "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            apiKey = legacyKey.orEmpty(),
            modelName = legacyModel ?: "glm-4-flash-250414",
            streamingEnabled = legacyStreaming ?: true
        )
    }

    private fun encodeConfigs(configs: List<AIConfig>): String = gson.toJson(configs)

    private fun decodeConfigs(json: String): List<AIConfig> {
        return try {
            (gson.fromJson<List<AIConfig>>(json, configListType) ?: emptyList())
                .map { it.normalizeForStorage() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 兼容旧格式：旧 JSON 缺少 `displayCipher`/`keyVersion` 时，Gson 会把 `displayCipher`
     * 置为 null、`keyVersion` 置为 0。这里归一化到数据类的非空默认值，保证后续读写不抛错。
     */
    private fun AIConfig.normalizeForStorage(): AIConfig {
        val rawDisplay: String? = this.displayCipher
        val displayCipher = rawDisplay ?: ""
        return if (displayCipher == this.displayCipher) this else this.copy(displayCipher = displayCipher)
    }

    /**
     * 是否显示开屏广告的 Flow
     * 默认值为 false（不显示）
     * 如果检测到 TalkBack 开启，则强制返回 false
     */
    val showSplashAdFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val showAd = preferences[PreferencesKeys.SHOW_SPLASH_AD] ?: false
        // If TalkBack is enabled, force disable splash ad
        if (isTalkBackEnabled(context)) {
            false
        } else {
            showAd
        }
    }

    /**
     * 设置是否显示开屏广告
     *
     * @param show true 为显示，false 为不显示
     */
    suspend fun setShowSplashAd(show: Boolean) {
        context.dataStore.edit { preferences ->
            // If TalkBack is enabled, force disable splash ad
            val actualValue = if (isTalkBackEnabled(context) && show) {
                false
            } else {
                show
            }
            preferences[PreferencesKeys.SHOW_SPLASH_AD] = actualValue
        }
    }

    /**
     * 是否强制使用平板横屏模式的 Flow
     * 默认值为 false（不强制）
     */
    val forceTabletLandscapeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FORCE_TABLET_LANDSCAPE] ?: false
    }

    /**
     * 设置是否强制使用平板横屏模式
     *
     * @param force true 为强制，false 为不强制
     */
    suspend fun setForceTabletLandscape(force: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FORCE_TABLET_LANDSCAPE] = force
        }
    }

    /**
     * 是否开启应用内全部震动反馈的 Flow
     * 默认值为 true（开启）
     */
    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true
    }

    /**
     * 设置是否开启应用内全部震动反馈
     *
     * @param enabled true 为开启，false 为关闭
     */
    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    /**
     * 是否跟随系统字体大小的 Flow
     * 默认值为 true（跟随系统）
     */
    val fontScaleFollowSystemFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FONT_SCALE_FOLLOW_SYSTEM] ?: true
    }

    /**
     * 设置是否跟随系统字体大小
     *
     * @param enabled true 为跟随系统，false 为使用自定义字体大小
     */
    suspend fun setFontScaleFollowSystem(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE_FOLLOW_SYSTEM] = enabled
        }
    }

    /**
     * 自定义字体大小缩放值的 Flow
     * 默认值为 1.0f（标准大小）
     */
    val fontScaleFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FONT_SCALE] ?: 1.0f
    }

    /**
     * 设置自定义字体大小缩放值
     *
     * @param value 1.0 为标准大小
     */
    suspend fun setFontScale(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] = value
        }
    }

    /**
     * 是否开启持久通知的 Flow
     * 默认值为 false（不开启）
     */
    val persistentNotificationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PERSISTENT_NOTIFICATION] ?: false
    }

    /**
     * 设置是否开启持久通知
     *
     * @param enabled true 为开启，false 为不开启
     */
    suspend fun setPersistentNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERSISTENT_NOTIFICATION] = enabled
        }
    }

    /**
     * 全部 LLM 配置列表的 Flow。
     *
     * 若 `llm_ai_configs` 键尚未写入但旧版单配置键存在（迁移完成前），
     * 则只读合成一条「默认配置」以保证 UI 与业务在迁移完成前也能读到正确的值。
     */
    val aiConfigsFlow: Flow<List<AIConfig>> = context.dataStore.data.map { preferences ->
        val stored = preferences[PreferencesKeys.LLM_AI_CONFIGS]
        if (stored != null) {
            decodeConfigs(stored)
        } else {
            synthesizeLegacyConfig(preferences)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * 当前使用的 LLM 配置的 Flow。
     * 优先返回 activeConfigId 指向的配置；若 id 失效则回退到第一条；列表为空返回 null。
     */
    val activeConfigFlow: Flow<AIConfig?> = context.dataStore.data.map { preferences ->
        val configs = preferences[PreferencesKeys.LLM_AI_CONFIGS]?.let { decodeConfigs(it) }
            ?: synthesizeLegacyConfig(preferences)?.let { listOf(it) }
            ?: emptyList()
        if (configs.isEmpty()) {
            null
        } else {
            val activeId = preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID]
            configs.firstOrNull { it.id == activeId } ?: configs.first()
        }
    }

    /**
     * 同步读取当前使用的 LLM 配置；无配置时返回 null。
     *
     * 返回的配置 `apiKey` 为**明文**（用运行时密钥解密），供发请求使用。
     * 若存储中仍是旧版明文（keyVersion == 0），会先惰性加密迁移再解密返回。
     */
    suspend fun getActiveAIConfig(): AIConfig? {
        var config = activeConfigFlow.first() ?: return null
        if (ApiKeyMigration.isPlaintext(config)) {
            encryptAndPersistConfigs()
            config = activeConfigFlow.first() ?: return null
        }
        val plainKey = ApiKeyMigration.decryptRuntimeSafely(
            decrypt = { ApiKeyCrypto.decryptRuntime(it) },
            runtimeCipher = config.apiKey
        )
        // 密钥失效/被清除：返回空 key 配置，调用方会提示用户重新录入
        return config.copy(apiKey = plainKey)
    }

    /**
     * 若 [config] 的 apiKey 仍是明文（keyVersion == 0），用运行时密钥 + 展示密钥加密并回写。
     * 幂等：已加密（keyVersion >= 1）或空白 key 直接原样返回。
     */
    private fun encryptIfPlaintext(config: AIConfig): AIConfig =
        ApiKeyMigration.encryptIfPlaintext(config) { plain ->
            ApiKeyCrypto.encryptConfig(plain)
        }

    /**
     * 一次性迁移：把 `llm_ai_configs` 中所有明文 apiKey 加密为密文并回写。
     * 幂等：已加密的配置保持不变；无配置或全部已加密时不做任何写操作。
     */
    suspend fun encryptAndPersistConfigs() {
        context.dataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.LLM_AI_CONFIGS] ?: return@edit
            val configs = decodeConfigs(json)
            val migrated = configs.map { encryptIfPlaintext(it) }
            if (migrated != configs) {
                preferences[PreferencesKeys.LLM_AI_CONFIGS] = encodeConfigs(migrated)
            }
        }
    }

    /**
     * 新增一条配置。若当前没有已选配置，则自动设为当前使用。
     * 保存时对明文 apiKey 加密（[encryptIfPlaintext]）。
     */
    suspend fun addAIConfig(config: AIConfig) {
        val stored = encryptIfPlaintext(config)
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LLM_AI_CONFIGS]?.let { decodeConfigs(it) } ?: emptyList()
            val updated = current + stored
            preferences[PreferencesKeys.LLM_AI_CONFIGS] = encodeConfigs(updated)
            if (preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] == null) {
                preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] = stored.id
            }
        }
    }

    /**
     * 更新一条已有配置（按 id 匹配）。保存时对明文 apiKey 加密（[encryptIfPlaintext]）。
     */
    suspend fun updateAIConfig(config: AIConfig) {
        val stored = encryptIfPlaintext(config)
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LLM_AI_CONFIGS]?.let { decodeConfigs(it) } ?: emptyList()
            val updated = current.map { if (it.id == stored.id) stored else it }
            preferences[PreferencesKeys.LLM_AI_CONFIGS] = encodeConfigs(updated)
        }
    }

    /**
     * 删除一条配置。若删除的是当前使用项，则自动提升列表第一条；列表清空则无当前配置。
     */
    suspend fun deleteAIConfig(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LLM_AI_CONFIGS]?.let { decodeConfigs(it) } ?: emptyList()
            val updated = current.filterNot { it.id == id }
            preferences[PreferencesKeys.LLM_AI_CONFIGS] = encodeConfigs(updated)
            if (preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] == id) {
                if (updated.isNotEmpty()) {
                    preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] = updated.first().id
                } else {
                    preferences.remove(PreferencesKeys.LLM_ACTIVE_CONFIG_ID)
                }
            }
        }
    }

    /**
     * 设置当前使用的配置。
     */
    suspend fun setActiveAIConfig(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] = id
        }
    }

    /**
     * 按 id 读取指定配置，返回带**明文** apiKey（运行时密钥解密）的副本，供发请求使用。
     * 会话内切换提供商用（不写回全局 active id）。找不到返回 null。
     */
    suspend fun getAIConfig(id: String): AIConfig? {
        var config = aiConfigsFlow.first().firstOrNull { it.id == id } ?: return null
        if (ApiKeyMigration.isPlaintext(config)) {
            encryptAndPersistConfigs()
            config = aiConfigsFlow.first().firstOrNull { it.id == id } ?: return null
        }
        val plainKey = ApiKeyMigration.decryptRuntimeSafely(
            decrypt = { ApiKeyCrypto.decryptRuntime(it) },
            runtimeCipher = config.apiKey
        )
        return config.copy(apiKey = plainKey)
    }

    /**
     * 一次性迁移：把旧版单配置键（llm_api_endpoint / llm_api_key / llm_model_name /
     * llm_streaming_response）迁移为一条「默认配置」并设为当前使用，随后物理删除旧键。
     * 幂等：`llm_ai_configs` 已存在时直接返回。
     */
    suspend fun migrateLegacyAiConfig() {
        context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.LLM_AI_CONFIGS] != null) return@edit
            val legacy = synthesizeLegacyConfig(preferences) ?: return@edit
            preferences[PreferencesKeys.LLM_AI_CONFIGS] = encodeConfigs(listOf(legacy))
            preferences[PreferencesKeys.LLM_ACTIVE_CONFIG_ID] = legacy.id
            preferences.remove(PreferencesKeys.LEGACY_LLM_API_ENDPOINT)
            preferences.remove(PreferencesKeys.LEGACY_LLM_API_KEY)
            preferences.remove(PreferencesKeys.LEGACY_LLM_MODEL_NAME)
            preferences.remove(PreferencesKeys.LEGACY_LLM_STREAMING_RESPONSE)
        }
    }

    /**
     * 是否开启习惯提醒的 Flow
     * 默认值为 true（开启）
     */
    val reminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMINDER_ENABLED] ?: true
    }

    /**
     * 设置是否开启习惯提醒
     */
    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_ENABLED] = enabled
        }
    }

    /**
     * 是否开启免打扰的 Flow
     * 默认值为 true（开启）
     */
    val dndEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DND_ENABLED] ?: true
    }

    /**
     * 设置是否开启免打扰
     */
    suspend fun setDndEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_ENABLED] = enabled
        }
    }

    /**
     * 免打扰开始时间的 Flow
     * 默认值为 "22:00"
     */
    val dndStartTimeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DND_START_TIME] ?: "22:00"
    }

    /**
     * 设置免打扰开始时间
     */
    suspend fun setDndStartTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_START_TIME] = time
        }
    }

    /**
     * 免打扰结束时间的 Flow
     * 默认值为 "07:00"
     */
    val dndEndTimeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DND_END_TIME] ?: "07:00"
    }

    /**
     * 设置免打扰结束时间
     */
    suspend fun setDndEndTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_END_TIME] = time
        }
    }

    /**
     * 下次提醒闹钟时间的 Flow（用于子页面显示）
     */
    val nextAlarmTimeFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NEXT_ALARM_TIME]
    }

    /**
     * 设置下次提醒闹钟时间
     */
    suspend fun setNextAlarmTime(millis: Long?) {
        context.dataStore.edit { preferences ->
            if (millis != null) {
                preferences[PreferencesKeys.NEXT_ALARM_TIME] = millis
            } else {
                preferences.remove(PreferencesKeys.NEXT_ALARM_TIME)
            }
        }
    }

    /**
     * 今日已发送提醒次数的 Flow
     */
    val reminderSentCountFlow: Flow<Pair<String, Long>> = context.dataStore.data.map { preferences ->
        val date = preferences[PreferencesKeys.REMINDER_SENT_DATE] ?: ""
        val count = preferences[PreferencesKeys.REMINDER_SENT_COUNT] ?: 0L
        Pair(date, count)
    }

    /**
     * 增加今日提醒发送计数（若日期不同则重置）
     */
    suspend fun incrementReminderSentCount(todayDate: String) {
        context.dataStore.edit { preferences ->
            val storedDate = preferences[PreferencesKeys.REMINDER_SENT_DATE]
            if (storedDate != todayDate) {
                preferences[PreferencesKeys.REMINDER_SENT_DATE] = todayDate
                preferences[PreferencesKeys.REMINDER_SENT_COUNT] = 1L
            } else {
                val current = preferences[PreferencesKeys.REMINDER_SENT_COUNT] ?: 0L
                preferences[PreferencesKeys.REMINDER_SENT_COUNT] = current + 1L
            }
        }
    }

    /**
     * 获取今日已发送提醒次数（同步读取）
     */
    suspend fun getTodayReminderSentCount(todayDate: String): Long {
        return context.dataStore.data.map { preferences ->
            val storedDate = preferences[PreferencesKeys.REMINDER_SENT_DATE]
            if (storedDate == todayDate) {
                preferences[PreferencesKeys.REMINDER_SENT_COUNT] ?: 0L
            } else {
                0L
            }
        }.first()
    }

    /**
     * 通知内容模板的 Flow
     * 返回存储的自定义模板，如果未设置则返回 null
     */
    val notificationTemplateFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_TEMPLATE]
    }

    /**
     * 设置通知内容模板
     *
     * @param template 模板文本，包含 {habit_name}、{checkin_time}、{habit_notes} 占位符
     */
    suspend fun setNotificationTemplate(template: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_TEMPLATE] = template
        }
    }

    /**
     * 重置通知内容模板为默认值（清除存储）
     */
    suspend fun resetNotificationTemplate() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.NOTIFICATION_TEMPLATE)
        }
    }

    /**
     * 按压震动时长的 Flow
     * 默认值为 25（毫秒）
     */
    val pressVibrationDurationMsFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRESS_VIBRATION_DURATION_MS] ?: 25L
    }

    /**
     * 设置按压震动时长
     */
    suspend fun setPressVibrationDurationMs(ms: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESS_VIBRATION_DURATION_MS] = ms
        }
    }

    /**
     * 按压震动强度的 Flow
     * 默认值为 128（中等，范围内 1-255）
     */
    val pressVibrationAmplitudeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRESS_VIBRATION_AMPLITUDE] ?: 128
    }

    /**
     * 设置按压震动强度
     */
    suspend fun setPressVibrationAmplitude(amplitude: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESS_VIBRATION_AMPLITUDE] = amplitude
        }
    }

    /**
     * 重置按压震动参数为默认值（清除存储）
     */
    suspend fun resetPressVibration() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PRESS_VIBRATION_DURATION_MS)
            preferences.remove(PreferencesKeys.PRESS_VIBRATION_AMPLITUDE)
        }
    }

    /**
     * AI 工具调用最大重试次数的 Flow
     * 默认值为 20
     */
    val aiToolRetryLimitFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_TOOL_RETRY_LIMIT] ?: 20
    }

    /**
     * 设置 AI 工具调用最大重试次数
     */
    suspend fun setAiToolRetryLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_TOOL_RETRY_LIMIT] = limit
        }
    }

    /**
     * AI 单次最大输出 Token 的 Flow
     * 默认值为 5000
     */
    val aiMaxOutputTokensFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_MAX_OUTPUT_TOKENS] ?: 5000
    }

    /**
     * 设置 AI 单次最大输出 Token
     */
    suspend fun setAiMaxOutputTokens(tokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MAX_OUTPUT_TOKENS] = tokens
        }
    }

    /**
     * AI 最大思考预算的 Flow
     * 默认值为 5000；0 表示不发送思考预算参数
     */
    val aiMaxThinkingTokensFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_MAX_THINKING_TOKENS] ?: 5000
    }

    /**
     * 设置 AI 最大思考预算
     */
    suspend fun setAiMaxThinkingTokens(tokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MAX_THINKING_TOKENS] = tokens
        }
    }
}
