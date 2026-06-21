package com.uade.alltabs.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uade.alltabs.presentation.auth.LoginScreen
import com.uade.alltabs.presentation.home.HomeScreen
import com.uade.alltabs.presentation.mytabs.MyTabsScreen
import com.uade.alltabs.presentation.search.SearchScreen
import com.uade.alltabs.presentation.songdetail.SongDetailScreen
import com.uade.alltabs.presentation.createTab.CreateTabScreen
import com.uade.alltabs.presentation.ai.AiJamScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(route = Screen.MyTabs.route) {
            MyTabsScreen(navController = navController)
        }

        composable(route = Screen.Search.route) {
            SearchScreen(navController = navController)
        }

        composable(route = Screen.AiJam.route) {
            AiJamScreen(navController = navController)
        }

        composable(route = Screen.Profile.route) {
            // Placeholder for Profile
        }

        composable(
            route = Screen.SongDetail.routeWithArgs,
            arguments = listOf(
                androidx.navigation.navArgument("mbid") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { nullable = true },
                androidx.navigation.navArgument("artist") { nullable = true }
            )
        ) { backStackEntry ->
            val mbid = backStackEntry.arguments?.getString("mbid")
            val title = backStackEntry.arguments?.getString("title")
            val artist = backStackEntry.arguments?.getString("artist")
            SongDetailScreen(navController = navController, mbid = mbid, initialTitle = title, initialArtist = artist)
        }

        composable(
            route = Screen.CreateTab.routeWithArgs,
            arguments = listOf(
                androidx.navigation.navArgument("title") { nullable = true },
                androidx.navigation.navArgument("artist") { nullable = true },
                androidx.navigation.navArgument("mbid") { nullable = true }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title")
            val artist = backStackEntry.arguments?.getString("artist")
            val mbid = backStackEntry.arguments?.getString("mbid")
            CreateTabScreen(navController = navController, initialTitle = title, initialArtist = artist, mbid = mbid)
        }

        composable(
            route = Screen.TabDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("tabId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val tabId = backStackEntry.arguments?.getString("tabId")
            com.uade.alltabs.presentation.tabdetail.TabDetailScreen(navController = navController, tabId = tabId)
        }

        composable(
            route = Screen.TabEditor.route,
            arguments = listOf(
                androidx.navigation.navArgument("tabId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val tabId = backStackEntry.arguments?.getString("tabId")
            com.uade.alltabs.presentation.editor.TabEditorScreen(navController = navController, tabId = tabId)
        }
    }
}
