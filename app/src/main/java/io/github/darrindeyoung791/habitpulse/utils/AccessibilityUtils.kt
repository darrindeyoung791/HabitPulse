package io.github.darrindeyoung791.habitpulse.utils

import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * Accessibility utility functions
 */
object AccessibilityUtils {

    /**
     * Check if TalkBack is enabled
     *
     * Note: We only check touch exploration mode which is specific to TalkBack,
     * not other accessibility services that users might have enabled.
     *
     * @param context the context to check accessibility status
     * @return true if TalkBack is enabled, false otherwise
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        // Only check touch exploration mode (TalkBack specific)
        // Don't use isEnabled as it returns true for ANY accessibility service
        return accessibilityManager.isTouchExplorationEnabled
    }
}
