package com.ufomap.ufosightingmap.ui.correlation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel

/**
 * Main screen for the correlation analysis feature.
 * Displays tabs for different types of correlations and their analyses.
 *
 * @param viewModel The correlation view model
 * @param onNavigateBack Callback for navigating back to the main map
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrelationScreen(
    viewModel: CorrelationViewModel,
    onNavigateBack: () -> Unit,
    onShowInfo: (String) -> Unit
) {
    // Current UI state
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Tab selection state
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Tab titles
    val tabTitles = listOf(
        "Military Bases",
        "Astronomy",
        "Weather",
        "Population"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Correlation Analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Info button
                    IconButton(onClick = { onShowInfo("Correlation Analysis") }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Information"
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = { viewModel.reloadAllData() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs row
            TabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Loading indicator or error message
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> MilitaryBaseCorrelationTab(viewModel)
                    1 -> AstronomicalCorrelationTab(viewModel)
                    2 -> WeatherCorrelationTab(viewModel)
                    3 -> PopulationCorrelationTab(viewModel)
                }

                // Show loading overlay if loading
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * Linear progress indicator for loading state
 */
@Composable
fun LinearProgressIndicator(modifier: Modifier = Modifier) {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary
    )
}