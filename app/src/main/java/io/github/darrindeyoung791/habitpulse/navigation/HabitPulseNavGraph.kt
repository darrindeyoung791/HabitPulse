package io.github.darrindeyoung791.habitpulse.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.darrindeyoung791.habitpulse.SettingsActivity
import io.github.darrindeyoung791.habitpulse.ui.screens.HabitCreationScreen
import io.github.darrindeyoung791.habitpulse.ui.screens.HomeScreen

@Composable
fun HabitPulseNavGraph(navController: NavHostController) {
    val context = LocalContext.current

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
                )
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            HomeScreen(
                onCreateHabit = {
                    navController.navigate(Route.CreateHabit.route)
                },
                onNavigateToSettings = {
                    val intent = android.content.Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
        composable(
            route = Route.CreateHabit.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                )
            }
        ) {
            HabitCreationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
