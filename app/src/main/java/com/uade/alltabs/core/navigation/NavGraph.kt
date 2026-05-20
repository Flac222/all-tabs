package com.uade.alltabs.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            // Placeholder for Login Screen
        }
        
        composable(route = Screen.Home.route) {
            // Placeholder for Home Screen
        }
    }
}
