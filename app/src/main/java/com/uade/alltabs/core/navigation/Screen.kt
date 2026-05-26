package com.uade.alltabs.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Home : Screen("home_screen")
    object MyTabs : Screen("my_tabs_screen")
    object Search : Screen("search_screen")
    object AiJam : Screen("ai_jam_screen")
    object Profile : Screen("profile_screen")
    object SongDetail : Screen("song_detail_screen")
    object CreateTab : Screen("create_tab_screen") {
        const val routeWithArgs = "create_tab_screen?title={title}&artist={artist}&mbid={mbid}"
        fun createRoute(title: String? = null, artist: String? = null, mbid: String? = null): String {
            return "create_tab_screen?" + 
                listOfNotNull(
                    title?.let { "title=$it" },
                    artist?.let { "artist=$it" },
                    mbid?.let { "mbid=$it" }
                ).joinToString("&")
        }
    }

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach {
                append("/$it")
            }
        }
    }
}
