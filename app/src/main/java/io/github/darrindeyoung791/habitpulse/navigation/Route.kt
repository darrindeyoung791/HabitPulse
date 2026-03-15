package io.github.darrindeyoung791.habitpulse.navigation

sealed class Route(val route: String) {
    object Home : Route("home")
    object CreateHabit : Route("create_habit")
    object Settings : Route("settings")
}
