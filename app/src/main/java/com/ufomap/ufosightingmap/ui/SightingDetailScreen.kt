package com.ufomap.ufosightingmap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.viewmodel.SightingDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightingDetailScreen(
    sightingId: Int,
    viewModel: SightingDetailViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect the sighting data as state (initially null while loading)
    val sighting by viewModel.getSighting(sightingId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sighting Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sighting == null) {
                // Show loading indicator while data is being fetched
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Display the detailed content once data is available
                SightingDetailContent(sighting!!)
            }
        }
    }
}

@Composable
private fun SightingDetailContent(sighting: Sighting) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Location header
        Text(
            text = sighting.city ?: "Unknown Location",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "${sighting.state ?: ""}, ${sighting.country ?: ""}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Primary info card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Date", sighting.dateTime ?: "Unknown")
                DetailRow("Shape", sighting.shape ?: "Unknown")
                DetailRow("Duration", sighting.duration ?: "Unknown")
                DetailRow("Posted", sighting.posted ?: "Unknown")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sighting Description",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = sighting.summary ?: "No description available",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Latitude: ${sighting.latitude}")
                Text("Longitude: ${sighting.longitude}")

                // Future enhancement: Add a small map preview here
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        Text(text = value)
    }
}