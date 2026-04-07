package io.github.darrindeyoung791.habitpulse

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.navigation.HabitPulseNavGraph
import io.github.darrindeyoung791.habitpulse.ui.screens.AdScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.HomeScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Set custom fade-out animation for splash screen exit
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOutAnimator = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                "alpha",
                1f,
                0f
            )
            fadeOutAnimator.duration = 200
            fadeOutAnimator.interpolator = AccelerateDecelerateInterpolator()
            fadeOutAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    splashScreenViewProvider.remove()
                }
            })
            fadeOutAnimator.start()
        }

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
                var adFinished by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // 等待读取设置
                    userPreferences.showSplashAdFlow.first().also { shouldShowAd ->
                        if (shouldShowAd) {
                            // 开启广告：splash screen 不延长，显示广告页面
                            showAdScreen = true
                        }
                    }
                }

                // 如果广告结束，进入主内容
                val showMainContent = adFinished || (!showSplashAd || !showAdScreen)

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
                        HabitPulseNavGraph(
                            navController = navController,
                            onHomeDataLoaded = { homeDataLoaded = true }
                        )

                        // 处理系统返回键，确保在主页时按返回键可以退出应用
                        HandleSystemBackPress(navController = navController, activity = activity)
                    }
                }

                // 显示广告页面（不参与淡入淡出动画）
                if (showSplashAd && showAdScreen && !adFinished) {
                    AdScreen(
                        onAdFinished = {
                            adFinished = true
                            showAdScreen = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 处理系统返回键
 * - 当在子页面时，返回上一级（带动画）
 * - 当在主页时，允许退出应用（带预测性返回动画）
 */
@Composable
fun HandleSystemBackPress(
    navController: NavHostController,
    activity: ComponentActivity
) {
    // 监听当前是否在主页（没有上一级）
    val isAtHome by remember {
        derivedStateOf {
            navController.previousBackStackEntry == null
        }
    }

    // 使用可变的 callback 引用，以便动态更新 enabled 状态
    val callback = remember {
        object : OnBackPressedCallback(!isAtHome) {
            override fun handleOnBackPressed() {
                // 如果在子页面，返回上一级（会触发 Navigation Compose 的预测性返回动画）
                navController.popBackStack()
            }
        }
    }

    // 注册回调
    DisposableEffect(navController, activity) {
        activity.onBackPressedDispatcher.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }

    // 根据是否在主页动态启用/禁用回调
    // 在主页时禁用回调，让系统处理退出（显示预测性返回动画）
    // 在子页面时启用回调，处理导航返回
    LaunchedEffect(isAtHome) {
        callback.isEnabled = !isAtHome
    }
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
