package io.github.darrindeyoung791.habitpulse.utils

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.core.app.LocaleManagerCompat
import io.github.darrindeyoung791.habitpulse.R
import java.util.Locale

/**
 * 应用内语言管理（Android 13+ per-app language preferences）
 *
 * 依赖 AGP 生成的 localeConfig（`generateLocaleConfig = true`）与 Manifest 中
 * 自动注入的 `android:localeConfig`，因此 Android 13+ 的系统「应用语言」设置与
 * 应用内语言选择天然保持同步。
 *
 * 切换语言后由系统自动重建相关 Activity；选择语言后语言子页面立即返回上一页，
 * 上一页在一次刷新后以新语言显示。
 */
object AppLocaleManager {

    private const val TAG = "AppLocaleManager"

    /**
     * 语言选择页可选的 BCP-47 语言标签，null 表示「跟随系统」。
     * 顺序即列表展示顺序。
     */
    val supportedTags: List<String?> = listOf(
        null,
        "zh-CN",
        "zh-TW",
        "zh-HK",
        "en-US",
        "en-GB"
    )

    /**
     * 当前设备是否支持 Android 13+ 的 per-app language 功能。
     */
    fun isPerAppLanguageSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * 当前应用语言对应的 Locale，未设置（跟随系统）时返回 null。
     */
    fun getCurrentAppLocale(context: Context): Locale? {
        if (!isPerAppLanguageSupported()) return null
        val locales = LocaleManagerCompat.getApplicationLocales(context)
        return if (locales.isEmpty) null else locales[0]
    }

    /**
     * 切换应用语言。tag 为 null 表示恢复为跟随系统。
     * 由系统持久化选择并自动重建相关 Activity。
     *
     * `LocaleManager.setApplicationLocales` 在 API 34+ 为公开 API，
     * API 33 仍是 @SystemApi，需要通过反射调用。
     */
    fun setAppLanguage(context: Context, tag: String?) {
        if (!isPerAppLanguageSupported()) return
        val appContext = context.applicationContext
        val localeList = if (tag == null) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
        val localeManager = appContext.getSystemService(LocaleManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            localeManager.setApplicationLocales(localeList)
        } else {
            try {
                val method = LocaleManager::class.java.getMethod(
                    "setApplicationLocales",
                    LocaleList::class.java
                )
                method.invoke(localeManager, localeList)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set application locales", e)
            }
        }
    }

    /**
     * 语言选项标签对应的字符串资源 id（自名显示，不随当前语言翻译）。
     * 未知标签返回默认语言标签资源。
     */
    fun labelRes(tag: String): Int = when (tag) {
        "zh-CN" -> R.string.language_zh_cn
        "zh-TW" -> R.string.language_zh_tw
        "zh-HK" -> R.string.language_zh_hk
        "en-US" -> R.string.language_en_us
        "en-GB" -> R.string.language_en_gb
        else -> R.string.language_zh_cn
    }

    /**
     * 设备真正的系统语言，不受应用内 per-app 语言覆盖影响。
     *
     * 注意：不能使用 [Locale.getDefault] —— Android 13+ 设置 per-app 语言后，
     * 进程内 `Locale.getDefault` 会被改写为应用语言，导致「跟随系统」显示错误。
     * 这里通过 `LocaleManagerCompat.getSystemLocales` 读取系统级 locale。
     */
    fun systemLocale(context: Context): Locale {
        val sys = LocaleManagerCompat.getSystemLocales(context)
        return if (!sys.isEmpty) sys[0] ?: Locale.getDefault() else Locale.getDefault()
    }

    /**
     * 系统语言是否为应用支持的语言（中文或英文）。
     * 当系统语言既不是中文也不是英文时，应停用「跟随系统」选项，
     * 因为跟随系统会让应用回退到默认语言，界面无法按预期显示。
     */
    fun isSystemLanguageSupported(context: Context): Boolean {
        val language = systemLocale(context).language
        return language.equals("zh", ignoreCase = true) ||
            language.equals("en", ignoreCase = true)
    }

    /**
     * 设备系统语言若匹配内置语言标签，返回其固定标签资源 id（自名显示，不随当前语言翻译）；
     * 未匹配（如系统为不支持的语言）返回 null。
     */
    fun systemLocaleLabelRes(context: Context): Int? {
        val sys = systemLocale(context)
        val tag = supportedTags.filterNotNull().firstOrNull { candidate ->
            val locale = Locale.forLanguageTag(candidate)
            locale.language.equals(sys.language, ignoreCase = true) &&
                locale.country.equals(sys.country, ignoreCase = true)
        } ?: return null
        return labelRes(tag)
    }

    /**
     * 设备系统语言的自名显示（未匹配内置标签时的兜底）。
     */
    fun systemLocaleSelfName(context: Context): String {
        return systemLocale(context).getDisplayName(systemLocale(context))
    }
}
