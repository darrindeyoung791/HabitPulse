package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.HabitPulseNavGraph
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.AdScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.HomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.NotificationPermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display - system bar colors handled by HabitPulseTheme
        enableEdgeToEdge()

        // Ensure status bar/navigation bar icon appearance is set early
        // This prevents a transient incorrect icon color after splash -> main content
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }

        setContent {
            HabitPulseTheme {
                val navController = rememberNavController()
                val userPreferences = remember { UserPreferences.getInstance(applicationContext) }
                val activity = this  // Capture activity reference before composable scope

                // 收集开屏广告设置状态
                val showSplashAd by userPreferences.showSplashAdFlow.collectAsStateWithLifecycle(initialValue = false)
                // 收集持久通知设置状态
                val persistentNotification by userPreferences.persistentNotificationFlow.collectAsStateWithLifecycle(initialValue = false)

                // 主页数据是否加载完成
                var homeDataLoaded by remember { mutableStateOf(false) }

                // 设置 splash screen 保持条件
                splashScreen.setKeepOnScreenCondition {
                    // 开启广告时：不延长 splash screen
                    // 关闭广告时：延长到主页数据加载完成
                    if (!showSplashAd) {
                        !homeDataLoaded
                    } else {
                        false
                    }
                }

                // 根据设置决定是否显示广告页面
                var showAdScreen by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // 等待读取设置
                    userPreferences.showSplashAdFlow.first().also { shouldShowAd ->
                        if (shouldShowAd) {
                            // 开启广告：splash screen 不延长，显示广告页面
                            showAdScreen = true
                        }
                    }
                }

                // Manage foreground service based on user preference
                val context = LocalContext.current
                LaunchedEffect(persistentNotification) {
                    if (persistentNotification && NotificationHelper.hasNotificationPermission(context)) {
                        NotificationHelper.createNotificationChannel(context)
                        ForegroundNotificationService.toggleService(context, true)
                    } else {
                        ForegroundNotificationService.toggleService(context, false)
                    }
                }

                // 如果广告结束，进入主内容
                val showMainContent = !showSplashAd || !showAdScreen
                
                // 检查是否需要显示通知权限对话框
                // 条件：持久通知设置已开启 AND 通知权限未授予 AND 广告已结束（或没有广告）
                var showNotificationPermissionDialog by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                
                LaunchedEffect(showMainContent, persistentNotification) {
                    if (showMainContent && persistentNotification && !NotificationHelper.hasNotificationPermission(context)) {
                        // 延迟显示对话框，确保主内容已渲染
                        showNotificationPermissionDialog = true
                    }
                }
                
                // 显示通知权限对话框
                if (showNotificationPermissionDialog) {
                    NotificationPermissionDialog(
                        onDismiss = { showNotificationPermissionDialog = false },
                        onGoToSettings = {
                            NotificationPermissionHelper.openAppSettings(context)
                            showNotificationPermissionDialog = false
                        },
                        onDisableFeature = {
                            // 关闭持久通知设置
                            coroutineScope.launch {
                                userPreferences.setPersistentNotification(false)
                            }
                            showNotificationPermissionDialog = false
                        }
                    )
                }

                // 再次在主内容可见时确保系统栏图标外观被正确设置（覆盖启动/过渡期影响）
                val isSystemDark = isSystemInDarkTheme()
                LaunchedEffect(showMainContent, isSystemDark) {
                    if (showMainContent) {
                        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                            isAppearanceLightStatusBars = !isSystemDark
                            isAppearanceLightNavigationBars = !isSystemDark
                        }
                    }
                }

                // 初始淡入淡出动画状态 - 用于splash结束后的平滑过渡
                var contentFadeInStarted by remember { mutableStateOf(false) }

                LaunchedEffect(showMainContent) {
                    if (showMainContent && !contentFadeInStarted) {
                        contentFadeInStarted = true
                    }
                }

                if (showMainContent) {
                    // 显示主导航（带淡入动画）
                    AnimatedVisibility(
                        visible = contentFadeInStarted,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                        label = "mainContentFadeIn"
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            HabitPulseNavGraph(
                                navController = navController,
                                onHomeDataLoaded = { homeDataLoaded = true }
                            )
                        }

                        // 处理系统返回键，确保在主页时按返回键可以退出应用
                        HandleSystemBackPress(navController = navController, activity = activity)
                    }
                }

                // 显示广告页面（带淡入淡出过渡动画）
                AnimatedVisibility(
                    visible = showAdScreen,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                    label = "adScreenTransition"
                ) {
                    AdScreen(
                        onAdFinished = { showAdScreen = false }
                    )
                }
            }
        }
    }
}

/**
 * 处理系统返回键
 * - 在子页面时不拦截，由 Navigation Compose 处理预测性导航返回动画
 * - 在主页时通过 moveTaskToBack 保活，避免 finish 导致进程被杀
 * - 输入法可见时先收回键盘再处理
 */
@Composable
fun HandleSystemBackPress(
    navController: NavHostController,
    activity: ComponentActivity
) {
    val isAtHome by remember {
        derivedStateOf {
            navController.previousBackStackEntry == null
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val callback = remember {
        object : OnBackPressedCallback(isAtHome || imeVisible) {
            override fun handleOnBackPressed() {
                if (imeVisible) {
                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
                }
                if (isAtHome) {
                    activity.moveTaskToBack(true)
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    DisposableEffect(navController, activity) {
        activity.onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }

    LaunchedEffect(isAtHome, imeVisible) {
        callback.isEnabled = isAtHome || imeVisible
    }
}

/**
 * 通知权限对话框
 * 
 * 当用户开启了保持后台运行功能，但未授予通知权限时显示。
 * 提供两个选项：去授权 或 关闭后台运行功能。
 * 
 * @param onDismiss 对话框关闭回调
 * @param onGoToSettings 前往应用设置回调
 * @param onDisableFeature 关闭后台运行功能回调
 */
@Composable
private fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    onDisableFeature: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = stringResource(id = R.string.notification_permission_dialog_title)
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = stringResource(id = R.string.notification_permission_dialog_message)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onGoToSettings
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.notification_permission_dialog_go_to_settings)
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDisableFeature
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.notification_permission_dialog_disable)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HabitPulseTheme {
        // Preview HomeScreen directly to avoid ClassCastException in preview
        // Full navigation preview requires HabitPulseApplication context
        HomeScreen(
            onCreateHabit = {},
            onNavigateToSettings = {},
            onEditHabit = {},
            application = null,
            onHomeDataLoaded = {}
        )
    }
}
