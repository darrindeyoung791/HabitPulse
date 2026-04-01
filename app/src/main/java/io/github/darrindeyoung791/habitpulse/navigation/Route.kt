package io.github.darrindeyoung791.habitpulse.navigation

import java.util.UUID

sealed class Route(val route: String) {
    object Home : Route("home")
    object CreateHabit : Route("create_habit")
    object EditHabit : Route("edit_habit/{habitId}") {
        fun createRoute(habitId: UUID): String = "edit_habit/$habitId"
    }
    object Settings : Route("settings")
    object MultiSelectSort : Route("multi_select_sort")
}
