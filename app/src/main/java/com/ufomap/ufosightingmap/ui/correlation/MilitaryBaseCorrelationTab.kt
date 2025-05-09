package com.ufomap.ufosightingmap.ui.correlation

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.data.correlation.dao.DistanceDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Tab for displaying military base correlation analysis.
 * Shows statistics and visualizations about the relationship
 * between UFO sightings and military installations.
 */
@Composable
fun MilitaryBaseCorrelationTab(viewModel: CorrelationViewModel) {
    // State
    val militaryBases by viewModel.militaryBases.collectAsState(initial = emptyList())
    val sightingsWithBaseDistance by viewModel.sightingsWithBaseDistance.collectAsState(initial = emptyList())
    val distanceDistribution by viewModel.militaryBaseDistanceDistribution.collectAsState(initial = emptyList())
    val correlationPercentage by viewModel.militaryBaseCorrelationPercentage.collectAsState(initial = 0f)
    val currentRadius by viewModel.currentBaseRadiusKm.collectAsState(initial = 50.0)

    // Local state for radius adjustment
    var sliderPosition by remember { mutableStateOf(currentRadius) }

    // Formatter for percentages
    val percentFormatter = remember { NumberFormat.getPercentInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Key statistic summary
        KeyStatisticsCard(
            militaryBaseCount = militaryBases.size,
            correlationPercentage = correlationPercentage,
            radiusKm = currentRadius,
            percentFormatter = percentFormatter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Radius adjustment
        RadiusControl(
            currentRadius = sliderPosition,
            onRadiusChange = {
                sliderPosition = it
            },
            onRadiusChangeFinished = {
                viewModel.setMilitaryBaseRadius(sliderPosition)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add fetch data button
        FetchDataButton(
            onClick = { viewModel.fetchMilitaryBaseData(true) },
            isLoading = viewModel.isLoading.collectAsState().value,
            baseCount = militaryBases.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // New button with proper loading state handling
        Button(
            onClick = { viewModel.loadFromJsonAsset() },
            enabled = !viewModel.isLoading.collectAsState().value,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            // Show circular progress indicator while loading
            if (viewModel.isLoading.collectAsState().value) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(if (viewModel.isLoading.collectAsState().value) "Loading..." else "Load from Local Asset")
        }

        // Distance distribution chart
        if (distanceDistribution.isNotEmpty()) {
            DistanceDistributionChart(
                distanceDistribution = distanceDistribution
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sightings near military bases
        if (sightingsWithBaseDistance.isNotEmpty()) {
            NearestBasesSection(
                sightings = sightingsWithBaseDistance.take(10)
            )
        }

        // Research citation
        ResearchSourceSection()

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
    }
}

/**
 * Card showing key statistics about military base correlation
 */
@Composable
private fun KeyStatisticsCard(
    militaryBaseCount: Int,
    correlationPercentage: Float,
    radiusKm: Double,
    percentFormatter: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Military Base Correlation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Military Base Count
                StatisticItem(
                    title = "Military Bases",
                    value = militaryBaseCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                // Correlation Percentage
                StatisticItem(
                    title = "Correlation",
                    value = percentFormatter.format(correlationPercentage / 100),
                    subtitle = "within ${radiusKm.roundToInt()}km",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis of the relationship between UFO sightings and proximity to military installations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatisticItem(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Control for adjusting the radius parameter
 */
@Composable
private fun RadiusControl(
    currentRadius: Double,
    onRadiusChange: (Double) -> Unit,
    onRadiusChangeFinished: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Proximity Radius",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adjust the radius to see how many UFO sightings occur within a specific distance of military installations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current radius display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${currentRadius.roundToInt()} km",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider for radius adjustment
            Slider(
                value = currentRadius.toFloat(),
                onValueChange = { onRadiusChange(it.toDouble()) },
                onValueChangeFinished = onRadiusChangeFinished,
                valueRange = 5f..200f,
                steps = 38, // 5 km increments in 5-200 range
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Labels for range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "5 km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "200 km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FetchDataButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    baseCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Fetch Real Military Base Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Load real military base data from OpenStreetMap using the Overpass API.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (baseCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Currently loaded: $baseCount military installations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(if (isLoading) "Fetching..." else "Fetch Military Base Data")
            }
        }
    }
}

/**
 * Chart showing the distribution of sightings by distance to military bases
 */
@Composable
private fun DistanceDistributionChart(
    distanceDistribution: List<DistanceDistribution>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sighting Distribution by Distance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate max count for scaling
            val maxCount = distanceDistribution.maxOfOrNull { it.sighting_count } ?: 0

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                if (maxCount > 0) {
                    BarChart(
                        data = distanceDistribution,
                        maxValue = maxCount,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "No distribution data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column {
                distanceDistribution.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${item.distance_band}: ${item.sighting_count} sightings",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple bar chart component
 */
@Composable
private fun BarChart(
    data: List<DistanceDistribution>,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    // Extract theme colors BEFORE entering the Canvas
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / data.size - 10f

            // Draw bars
            data.forEachIndexed { index, item ->
                val barHeight = (item.sighting_count.toFloat() / maxValue) * canvasHeight

                // Bar position
                val startX = index * (barWidth + 10f) + 10f
                val startY = canvasHeight - barHeight

                // Draw bar - use the extracted color variable
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(startX, startY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )

                // Draw outline - use the extracted color variable
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(startX, startY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    style = Stroke(width = 1f)
                )
            }

            // Draw baseline - use the extracted color variable
            drawLine(
                color = outlineColor,
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * Section displaying sightings nearest to military bases
 */
@Composable
private fun NearestBasesSection(
    sightings: List<SightingWithBaseDistance>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Closest Sightings to Military Bases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The following UFO sightings occurred in close proximity to military installations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )

                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Nearest Base",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )

                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Divider()

            // Sightings list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sightings.forEach { sighting ->
                    SightingNearBaseItem(sighting)
                    Divider()
                }
            }
        }
    }
}

/**
 * Individual sighting item in the list
 */
@Composable
private fun SightingNearBaseItem(sighting: SightingWithBaseDistance) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Location
            Text(
                text = buildString {
                    append(sighting.city ?: "Unknown")
                    if (!sighting.state.isNullOrBlank()) {
                        append(", ")
                        append(sighting.state)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.5f)
            )

            // Date
            Text(
                text = sighting.dateTime?.substringBefore("T") ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Nearest Base
            Text(
                text = sighting.nearest_base_name ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.5f)
            )

            // Distance
            Text(
                text = sighting.distance_to_nearest_base?.let {
                    String.format("%.1f km", it)
                } ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/**
 * Section with research citations and methods
 */
@Composable
private fun ResearchSourceSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Research Basis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Research shows a strong relationship between UFO sightings and proximity to military installations, " +
                        "with studies finding that 61% of all UFO sightings occur within 24 miles of a military installation, " +
                        "and 82% within 42 miles.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Method: Haversine formula used to calculate great-circle distances between sighting coordinates and military base locations.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Data sources: US Government Military Installation Database, National UFO Reporting Center (NUFORC).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}