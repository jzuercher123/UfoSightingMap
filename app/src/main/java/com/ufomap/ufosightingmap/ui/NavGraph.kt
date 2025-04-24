package com.ufomap.ufosightingmap.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import com.ufomap.ufosightingmap.viewmodel.SightingDetailViewModel

/**
 * Defines the navigation routes in the app
 */
sealed class Screen(val route: String) {
    /**
     * Main map screen showing all sightings
     */
    object Map : Screen("map")

    /**
     * Detail screen for a specific sighting
     */
    object Detail : Screen("detail/{sightingId}") {
        fun createRoute(sightingId: Int) = "detail/$sightingId"
    }
}

/**
 * Sets up the navigation graph for the app
 */
@Composable
fun UFOSightingsNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route
    ) {
        // Map Screen
        composable(Screen.Map.route) {
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                viewModel = viewModel,
                onSightingClick = { sightingId ->
                    navController.navigate(Screen.Detail.createRoute(sightingId))
                }
            )
        }

        // Detail Screen
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("sightingId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val sightingId = backStackEntry.arguments?.getInt("sightingId") ?: 0
            val viewModel: SightingDetailViewModel = viewModel()

            SightingDetailScreen(
                sightingId = sightingId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}