package com.ufomap.ufosightingmap.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ufomap.ufosightingmap.ui.correlation.CorrelationScreen
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import com.ufomap.ufosightingmap.viewmodel.SightingDetailViewModel
import com.ufomap.ufosightingmap.viewmodel.SightingSubmissionViewModel
import kotlinx.coroutines.launch

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

    /**
     * Screen for submitting a new UFO sighting report
     */
    object SubmitSighting : Screen("submit")

    /**
     * Correlation analysis screen for data visualization
     */
    object CorrelationAnalysis : Screen("correlation")
}

/**
 * Sets up the navigation graph for the app
 */
@Composable
fun UFOSightingsNavGraph(navController: NavHostController) {
    // Create a SnackbarHostState to show information messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                },
                onReportSighting = {
                    navController.navigate(Screen.SubmitSighting.route)
                },
                onShowCorrelationAnalysis = {
                    navController.navigate(Screen.CorrelationAnalysis.route)
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

        // Sighting Submission Screen
        composable(Screen.SubmitSighting.route) {
            val viewModel: SightingSubmissionViewModel = viewModel()

            SightingSubmissionScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Correlation Analysis Screen
        composable(Screen.CorrelationAnalysis.route) {
            val viewModel: CorrelationViewModel = viewModel()

            CorrelationScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onShowInfo = { topic ->
                    // Show information about correlation analysis using the coroutineScope
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Information about $topic",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }
}