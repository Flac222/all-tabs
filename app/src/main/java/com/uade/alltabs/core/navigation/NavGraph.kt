package com.uade.alltabs.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uade.alltabs.presentation.auth.LoginScreen
import com.uade.alltabs.presentation.home.HomeScreen
import com.uade.alltabs.presentation.mytabs.MyTabsScreen
import com.uade.alltabs.presentation.search.SearchScreen

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
            // Placeholder for AI Jam
        }

        composable(route = Screen.Profile.route) {
            // Placeholder for Profile
        }
    }
}
