package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display - system bar colors handled by HabitPulseTheme
        enableEdgeToEdge()

        setContent {
            HabitPulseTheme {
                val navController = rememberNavController()
                val userPreferences = remember { UserPreferences.getInstance(applicationContext) }
                
                // 收集开屏广告设置状态
                val showSplashAd by userPreferences.showSplashAdFlow.collectAsStateWithLifecycle(initialValue = false)
                
                // 控制 splash screen 保持显示的状态
                var keepSplashScreen by remember { mutableStateOf(true) }
                
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
                            keepSplashScreen = false
                            showAdScreen = true
                        }
                        // 关闭广告：keepSplashScreen 保持 true，等待 homeDataLoaded
                    }
                }
                
                // 如果广告结束，进入主页
                val showMainContent = adFinished || (!showSplashAd || !showAdScreen)

                Crossfade(
                    targetState = showMainContent,
                    animationSpec = tween(durationMillis = 200),
                    label = "screenCrossfade"
                ) { showMain ->
                    if (showMain) {
                        HabitPulseNavGraph(
                            navController = navController,
                            onHomeDataLoaded = { homeDataLoaded = true }
                        )

                        // 处理系统返回键，确保在主页时按返回键可以退出应用
                        HandleSystemBackPress(navController = navController, activity = this)
                    } else if (showSplashAd && showAdScreen) {
                        // 显示广告页面
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
        // Preview content can be added here if needed
    }
}
