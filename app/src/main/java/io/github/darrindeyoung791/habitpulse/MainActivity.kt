package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavHostController
import io.github.darrindeyoung791.habitpulse.navigation.HabitPulseNavGraph
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and keep it visible until content is loaded
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display - system bar colors handled by HabitPulseTheme
        enableEdgeToEdge()

        setContent {
            HabitPulseTheme {
                val navController = rememberNavController()
                
                // 控制 splash screen 保持显示的状态
                // 使用 rememberSaveable 确保状态在配置变更时保持
                var keepSplashScreen by remember { mutableStateOf(true) }
                
                // 设置 splash screen 保持条件
                splashScreen.setKeepOnScreenCondition { keepSplashScreen }
                
                // 将 keepSplashScreen 的状态传递给导航图
                HabitPulseNavGraph(
                    navController = navController,
                    onSplashScreenReady = { keepSplashScreen = false }
                )

                // 处理系统返回键，确保在主页时按返回键可以退出应用
                HandleSystemBackPress(navController = navController, activity = this)
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
