package com.ufomap.ufosightingmap.ui.correlation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card showing key statistics about weather correlation
 */
@Composable
private fun KeyStatisticsCard(
    unusualWeatherPercentage: Float
) {
    // Formatter for percentages
    val percentFormatter = remember { NumberFormat.getPercentInstance() }

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
                text = "Weather Correlation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weather type statistic
                StatisticItem(
                    title = "Unusual Weather",
                    value = percentFormatter.format(unusualWeatherPercentage / 100),
                    subtitle = "of sightings coincide with unusual weather",
                    modifier = Modifier.weight(1f)
                )

                // Most common correlation
                StatisticItem(
                    title = "Most Common",
                    value = "Fog & Low Visibility",
                    subtitle = "weather condition during sightings",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis of the relationship between UFO sightings and weather conditions, including unusual atmospheric phenomena.",
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Card showing distribution of weather types during UFO sightings
 */
@Composable
private fun WeatherTypeDistributionCard(
    weatherTypeDistributions: List<WeatherTypeDistribution>
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
                text = "Weather Type Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate total for percentages
            val totalCount = weatherTypeDistributions.sumOf { it.event_count }

            // Weather type list with progress bars
            Column {
                weatherTypeDistributions.forEach { item ->
                    val percentage = if (totalCount > 0) item.event_count.toFloat() / totalCount else 0f

                    WeatherTypeItem(
                        weatherType = item.weather_type,
                        count = item.event_count,
                        percentage = percentage
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual weather type item with progress bar
 */
@Composable
private fun WeatherTypeItem(
    weatherType: WeatherEvent.WeatherType,
    count: Int,
    percentage: Float
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Weather type name
            Text(
                text = formatWeatherType(weatherType),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Count and percentage
            Text(
                text = "$count (${String.format("%.1f", percentage * 100)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((percentage * 100).coerceIn(0f, 100f).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(getWeatherTypeColor(weatherType))
            )
        }
    }
}

/**
 * Card showing correlation between weather conditions and sighting shapes
 */
@Composable
private fun WeatherCorrelationCard() {
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
                text = "Weather-Shape Correlation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Correlation between specific weather conditions and reported UFO shapes.",
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
                    text = "Weather Condition",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )

                Text(
                    text = "Common Shapes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )

                Text(
                    text = "Correlation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Divider()

            // Weather-shape correlation list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                items(getPlaceholderWeatherShapeCorrelations()) { correlation ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Weather condition
                        Text(
                            text = correlation.weatherCondition,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.2f)
                        )

                        // Common shapes
                        Text(
                            text = correlation.commonShapes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.5f)
                        )

                        // Correlation strength
                        Text(
                            text = correlation.correlationStrength,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            color = getCorrelationColor(correlation.correlationStrength),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Divider()
                }
            }
        }
    }
}

/**
 * Card showing weather events at sighting locations
 */
@Composable
private fun WeatherEventsCard(events: List<WeatherEvent>) {
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
                text = "Recent Weather Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recent unusual weather events that may correlate with UFO sightings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weather events list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(events) { event ->
                    WeatherEventItem(event)
                    Divider()
                }
            }
        }
    }
}

/**
 * Individual weather event item
 */
@Composable
private fun WeatherEventItem(event: WeatherEvent) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Location and date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(event.city ?: "Unknown Location")
                        if (!event.state.isNullOrBlank()) {
                            append(", ")
                            append(event.state)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = dateFormatter.format(Date(event.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weather type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(getWeatherTypeColor(event.type))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatWeatherType(event.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Weather details
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (event.temperature != null) {
                Text(
                    text = "Temp: ${String.format("%.1f°C", event.temperature)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (event.visibility != null) {
                Text(
                    text = "Visibility: ${String.format("%.1f km", event.visibility)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (event.hasInversionLayer || event.hasLightRefractionConditions) {
                Text(
                    text = if (event.hasInversionLayer) "Inversion Layer" else "Light Refraction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
                text = "Research has shown correlations between unusual weather conditions (particularly thunderstorms, temperature inversions, and fog) and reported UFO sightings. These conditions can create optical illusions and unusual light refraction.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Method: Spatiotemporal correlation between weather events and UFO sightings within 50km and 24 hours.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Data sources: National Weather Service, NOAA Storm Events Database, National UFO Reporting Center (NUFORC).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Format weather type enum to a readable string
 */
private fun formatWeatherType(type: WeatherEvent.WeatherType): String {
    return when (type) {
        WeatherEvent.WeatherType.THUNDERSTORM -> "Thunderstorm"
        WeatherEvent.WeatherType.LIGHTNING -> "Lightning"
        WeatherEvent.WeatherType.FOG -> "Fog"
        WeatherEvent.WeatherType.TEMPERATURE_INVERSION -> "Temperature Inversion"
        WeatherEvent.WeatherType.HEAVY_RAIN -> "Heavy Rain"
        WeatherEvent.WeatherType.SNOW -> "Snow"
        WeatherEvent.WeatherType.HAIL -> "Hail"
        WeatherEvent.WeatherType.TORNADO -> "Tornado"
        WeatherEvent.WeatherType.HURRICANE -> "Hurricane"
        WeatherEvent.WeatherType.CLEAR_SKY -> "Clear Sky"
        WeatherEvent.WeatherType.AURORA -> "Aurora"
        WeatherEvent.WeatherType.SPRITES -> "Sprites"
        WeatherEvent.WeatherType.BALL_LIGHTNING -> "Ball Lightning"
        WeatherEvent.WeatherType.OTHER -> "Other"
    }
}

/**
 * Get color for a weather type
 */
private fun getWeatherTypeColor(type: WeatherEvent.WeatherType): Color {
    return when (type) {
        WeatherEvent.WeatherType.THUNDERSTORM -> Color(0xFF4527A0) // Deep purple
        WeatherEvent.WeatherType.LIGHTNING -> Color(0xFFD500F9) // Purple
        WeatherEvent.WeatherType.FOG -> Color(0xFFB0BEC5) // Blue grey
        WeatherEvent.WeatherType.TEMPERATURE_INVERSION -> Color(0xFFFF9800) // Orange
        WeatherEvent.WeatherType.HEAVY_RAIN -> Color(0xFF1976D2) // Blue
        WeatherEvent.WeatherType.SNOW -> Color(0xFF90CAF9) // Light blue
        WeatherEvent.WeatherType.HAIL -> Color(0xFF26C6DA) // Cyan
        WeatherEvent.WeatherType.TORNADO -> Color(0xFF7B1FA2) // Purple
        WeatherEvent.WeatherType.HURRICANE -> Color(0xFF0D47A1) // Dark blue
        WeatherEvent.WeatherType.CLEAR_SKY -> Color(0xFF03A9F4) // Light blue
        WeatherEvent.WeatherType.AURORA -> Color(0xFF4CAF50) // Green
        WeatherEvent.WeatherType.SPRITES -> Color(0xFF00E676) // Green
        WeatherEvent.WeatherType.BALL_LIGHTNING -> Color(0xFFFF3D00) // Orange
        WeatherEvent.WeatherType.OTHER -> Color(0xFF757575) // Grey
    }
}

/**
 * Get color for correlation strength
 */
private fun getCorrelationColor(strength: String): Color {
    return when (strength) {
        "Strong" -> Color(0xFF4CAF50) // Green
        "Moderate" -> Color(0xFFFFC107) // Amber
        "Weak" -> Color(0xFF2196F3) // Blue
        else -> Color(0xFF757575) // Grey
    }
}

/**
 * Generate placeholder weather type distribution data
 */
private fun getPlaceholderWeatherDistribution(): List<WeatherTypeDistribution> {
    return listOf(
        WeatherTypeDistribution(WeatherEvent.WeatherType.FOG, 142),
        WeatherTypeDistribution(WeatherEvent.WeatherType.THUNDERSTORM, 98),
        WeatherTypeDistribution(WeatherEvent.WeatherType.TEMPERATURE_INVERSION, 76),
        WeatherTypeDistribution(WeatherEvent.WeatherType.CLEAR_SKY, 55),
        WeatherTypeDistribution(WeatherEvent.WeatherType.AURORA, 29),
        WeatherTypeDistribution(WeatherEvent.WeatherType.SPRITES, 17),
        WeatherTypeDistribution(WeatherEvent.WeatherType.BALL_LIGHTNING, 12),
        WeatherTypeDistribution(WeatherEvent.WeatherType.OTHER, 45)
    )
}

/**
 * Data class for weather-shape correlation
 */
private data class WeatherShapeCorrelation(
    val weatherCondition: String,
    val commonShapes: String,
    val correlationStrength: String
)

/**
 * Generate placeholder weather-shape correlation data
 */
private fun getPlaceholderWeatherShapeCorrelations(): List<WeatherShapeCorrelation> {
    return listOf(
        WeatherShapeCorrelation("Fog / Low Visibility", "Orb, Light, Oval", "Strong"),
        WeatherShapeCorrelation("Thunderstorm", "Fireball, Triangle, Disk", "Strong"),
        WeatherShapeCorrelation("Temperature Inversion", "Cigar, Ellipse, Changing", "Moderate"),
        WeatherShapeCorrelation("Aurora Activity", "Light, Formation, Rectangle", "Moderate"),
        WeatherShapeCorrelation("Ball Lightning", "Sphere, Circle, Light", "Strong"),
        WeatherShapeCorrelation("Clear Night Sky", "Triangle, Formation, Other", "Weak"),
        WeatherShapeCorrelation("Sprites", "Flash, Light, Rectangle", "Moderate"),
        WeatherShapeCorrelation("Heavy Rain", "Light, Oval, Unknown", "Weak")
    )
}