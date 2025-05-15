package com.ufomap.ufosightingmap.ui.correlation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A distance slider component for correlation analysis.
 * Allows selecting a distance radius for proximity analysis.
 */
@Composable
fun DistanceSlider(
    modifier: Modifier = Modifier,
    initialDistance: Float = 50f,
    minDistance: Float = 5f,
    maxDistance: Float = 200f,
    steps: Int = 39, // (200-5)/5 = 39 steps for 5km increments
    onDistanceChange: (Float) -> Unit = {},
    onDistanceChangeFinished: () -> Unit = {},
    title: String = "Distance Radius",
    description: String? = "Adjust the radius to analyze proximity correlations",
    unit: String = "km"
) {
    var distance by remember { mutableStateOf(initialDistance) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            description?.let {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current distance display
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${distance.roundToInt()} $unit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = distance,
                onValueChange = {
                    distance = it
                    onDistanceChange(it)
                },
                onValueChangeFinished = onDistanceChangeFinished,
                valueRange = minDistance..maxDistance,
                steps = steps,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${minDistance.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${maxDistance.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * A specialized version of the distance slider for military base proximity analysis
 */
@Composable
fun MilitaryBaseProximitySlider(
    modifier: Modifier = Modifier,
    initialDistance: Float = 50f,
    onDistanceChange: (Float) -> Unit = {},
    onDistanceChangeFinished: () -> Unit = {}
) {
    DistanceSlider(
        modifier = modifier,
        initialDistance = initialDistance,
        minDistance = 5f,
        maxDistance = 200f,
        steps = 39, // 5km increments from 5 to 200
        onDistanceChange = onDistanceChange,
        onDistanceChangeFinished = onDistanceChangeFinished,
        title = "Military Base Proximity",
        description = "Adjust the radius to analyze the correlation between UFO sightings and proximity to military installations"
    )
}

/**
 * A specialized version of the distance slider for population density analysis
 */
@Composable
fun PopulationDensityDistanceSlider(
    modifier: Modifier = Modifier,
    initialDistance: Float = 25f,
    onDistanceChange: (Float) -> Unit = {},
    onDistanceChangeFinished: () -> Unit = {}
) {
    DistanceSlider(
        modifier = modifier,
        initialDistance = initialDistance,
        minDistance = 5f,
        maxDistance = 100f,
        steps = 19, // 5km increments from 5 to 100
        onDistanceChange = onDistanceChange,
        onDistanceChangeFinished = onDistanceChangeFinished,
        title = "Population Density Radius",
        description = "Adjust the radius to analyze the correlation between UFO sightings and population density"
    )
}

/**
 * A specialized version of the distance slider for weather event analysis
 */
@Composable
fun WeatherEventDistanceSlider(
    modifier: Modifier = Modifier,
    initialDistance: Float = 30f,
    onDistanceChange: (Float) -> Unit = {},
    onDistanceChangeFinished: () -> Unit = {}
) {
    DistanceSlider(
        modifier = modifier,
        initialDistance = initialDistance,
        minDistance = 5f,
        maxDistance = 100f,
        steps = 19, // 5km increments from 5 to 100
        onDistanceChange = onDistanceChange,
        onDistanceChangeFinished = onDistanceChangeFinished,
        title = "Weather Event Radius",
        description = "Adjust the radius to analyze the correlation between UFO sightings and weather events"
    )
}

/**
 * A size slider component for adjusting visualization parameters
 */
@Composable
fun SizeSlider(
    modifier: Modifier = Modifier,
    initialSize: Float = 10f,
    minSize: Float = 1f,
    maxSize: Float = 50f,
    steps: Int = 49,
    onSizeChange: (Float) -> Unit = {},
    onSizeChangeFinished: () -> Unit = {},
    title: String = "Size",
    description: String? = "Adjust the visualization size",
    unit: String = "px"
) {
    var size by remember { mutableStateOf(initialSize) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            description?.let {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current size display
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${size.roundToInt()} $unit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = size,
                onValueChange = {
                    size = it
                    onSizeChange(it)
                },
                onValueChangeFinished = onSizeChangeFinished,
                valueRange = minSize..maxSize,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Labels for range
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${minSize.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${maxSize.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}