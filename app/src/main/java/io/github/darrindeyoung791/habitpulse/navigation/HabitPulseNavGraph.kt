package io.github.darrindeyoung791.habitpulse.navigation

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.SettingsActivity
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.ui.screens.EditMode
import io.github.darrindeyoung791.habitpulse.ui.screens.HabitCreationScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.HomeScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.MultiSelectSortScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.WebViewScreen
import java.util.UUID

/**
 * 获取设备圆角半径（Android 12+）
 * 在 Android 12+ 上使用系统提供的圆角值，旧版本使用默认值 16dp
 */
@Composable
fun getDeviceCornerRadius(): Dp {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        try {
            val display = context.display
            if (display != null) {
                // 使用反射获取 cornerRadius，因为它是隐藏 API
                val method = display.javaClass.getMethod("getCornerRadius")
                val radius = method.invoke(display) as? android.util.Size
                if (radius != null) {
                    maxOf(radius.width, radius.height).toFloat().dp
                } else {
                    16.dp
                }
            } else {
                16.dp
            }
        } catch (e: Exception) {
            16.dp
        }
    } else {
        16.dp
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HabitPulseNavGraph(
    navController: NavHostController,
    onHomeDataLoaded: () -> Unit = {}
) {
    val cornerRadius = getDeviceCornerRadius()
    val context = LocalContext.current

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 0 })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { 0 })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { 0 })
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 0 })
            }
        ) {
        composable(
            route = Route.Home.route,
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            }
        ) { backStackEntry ->
            val animatedContentScope = this
            val sharedTransitionScope = this@SharedTransitionLayout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                HomeScreen(
                    onCreateHabit = {
                        navController.navigate(Route.CreateHabit.route) {
                            launchSingleTop = true
                            popUpTo(Route.Home.route) {
                                inclusive = false
                            }
                        }
                    },
                    onNavigateToSettings = {
                        val intent = android.content.Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    onEditHabit = { habit ->
                        navController.navigate(Route.EditHabit.createRoute(habit.id)) {
                            launchSingleTop = true
                            popUpTo(Route.Home.route) {
                                inclusive = false
                            }
                        }
                    },
                    onNavigateToMultiSelect = { habitId ->
                        val viewModel = (context.applicationContext as HabitPulseApplication).habitViewModel
                        viewModel.enterMultiSelectMode(habitId)
                        navController.navigate(Route.MultiSelectSort.route) {
                            launchSingleTop = true
                            popUpTo(Route.Home.route) {
                                inclusive = false
                            }
                        }
                    },
                    application = context.applicationContext as HabitPulseApplication,
                    onHomeDataLoaded = onHomeDataLoaded,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope
                )
            }
        }
        composable(
            route = Route.CreateHabit.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                HabitCreationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    editMode = EditMode.CREATE,
                    navController = navController,
                    application = context.applicationContext as HabitPulseApplication
                )
            }
        }
        composable(
            route = Route.EditHabit.route,
            arguments = listOf(
                navArgument("habitId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        ) { backStackEntry ->
            val habitId = UUID.fromString(backStackEntry.arguments?.getString("habitId"))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                HabitCreationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    editMode = EditMode.EDIT,
                    habitId = habitId,
                    navController = navController,
                    application = context.applicationContext as HabitPulseApplication
                )
            }
        }
        composable(
            route = Route.MultiSelectSort.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        ) { backStackEntry ->
            val viewModel = (context.applicationContext as HabitPulseApplication).habitViewModel
            val animatedContentScope = this
            val sharedTransitionScope = this@SharedTransitionLayout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                MultiSelectSortScreen(
                    onNavigateBack = {
                        // Exit multi-select mode - called as fallback if navigation guard fails
                        viewModel.exitMultiSelectMode()
                    },
                    viewModel = viewModel,
                    application = context.applicationContext as HabitPulseApplication,
                    navController = navController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope
                )
            }
        }
        composable(
            route = Route.Help.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = RouteConfig.HELP_URL
                }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: RouteConfig.HELP_URL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                WebViewScreen(
                    initialUrl = url,
                    onClose = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    }
}
