package io.github.darrindeyoung791.habitpulse.utils

import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * Accessibility utility functions
 */
object AccessibilityUtils {

    /**
     * Check if TalkBack (or any accessibility service) is enabled
     *
     * @param context the context to check accessibility status
     * @return true if TalkBack or any accessibility service is enabled, false otherwise
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled || accessibilityManager.isTouchExplorationEnabled
    }
}
