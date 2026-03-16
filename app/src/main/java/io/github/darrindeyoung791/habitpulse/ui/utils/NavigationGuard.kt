package io.github.darrindeyoung791.habitpulse.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

/**
 * 导航保护器
 * 防止多次点击导致返回到不存在的页面
 */
@Stable
class NavigationGuard(
    private val navController: NavHostController,
    private val debounceTime: Long = 500L
) {
    var canNavigate by mutableStateOf(true)
        private set

    private var isProcessing by mutableStateOf(false)

    /**
     * 检查当前是否在主页
     */
    val isAtHome: Boolean
        get() {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            return currentRoute == "home" || currentRoute == null
        }

    /**
     * 安全地执行返回操作，确保不会返回到主页以上
     * @return 是否成功执行了返回操作
     */
    suspend fun safePopBackStack(): Boolean {
        if (!canNavigate || isProcessing) {
            return false
        }

        // 检查是否已经在主页
        if (isAtHome) {
            return false
        }

        isProcessing = true
        canNavigate = false

        try {
            val result = navController.popBackStack()
            delay(debounceTime)
            return result
        } finally {
            isProcessing = false
            canNavigate = true
        }
    }

    /**
     * 安全地执行导航操作
     * @param route 目标路由
     * @return 是否成功执行了导航操作
     */
    suspend fun safeNavigate(route: String): Boolean {
        if (!canNavigate || isProcessing) {
            return false
        }

        isProcessing = true
        canNavigate = false

        try {
            navController.navigate(route) {
                launchSingleTop = true
            }
            delay(debounceTime)
            return true
        } finally {
            isProcessing = false
            canNavigate = true
        }
    }
}

/**
 * 创建一个导航保护器
 * @param navController 导航控制器
 * @param debounceTime 防抖时间（毫秒），默认 500ms
 */
@Composable
fun rememberNavigationGuard(
    navController: NavHostController,
    debounceTime: Long = 500L
): NavigationGuard {
    return remember(navController) { NavigationGuard(navController, debounceTime) }
}
