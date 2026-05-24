package com.uade.alltabs.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Home : Screen("home_screen")
    object MyTabs : Screen("my_tabs_screen")
    object Search : Screen("search_screen")
    object AiJam : Screen("ai_jam_screen")
    object Profile : Screen("profile_screen")

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}
