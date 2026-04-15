package io.github.darrindeyoung791.habitpulse.navigation

import java.util.UUID

object RouteConfig {
    const val HELP_URL = "https://darrindeyoung791.github.io/HabitPulse/tutorial/help-and-feedback"
}

sealed class Route(val route: String) {
    object Home : Route("home")
    object CreateHabit : Route("create_habit")
    object EditHabit : Route("edit_habit/{habitId}") {
        fun createRoute(habitId: UUID): String = "edit_habit/$habitId"
    }
    object Settings : Route("settings")
    object MultiSelectSort : Route("multi_select_sort")
    object Help : Route("help?url={url}") {
        fun createRoute(url: String): String = "help?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
}
