package io.github.darrindeyoung791.habitpulse.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 引导偏好设置管理器
 *
 * 用于存储和读取用户引导相关设置
 */
class OnboardingPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 用户是否已完成初次使用引导（同意协议）
     */
    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, value).apply()

    /**
     * 用户是否处于受限模式（不同意协议但仍使用应用）
     */
    var isLimitedMode: Boolean
        get() = prefs.getBoolean(KEY_IS_LIMITED_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LIMITED_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "habitpulse_onboarding_prefs"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        private const val KEY_IS_LIMITED_MODE = "is_limited_mode"
    }
}
