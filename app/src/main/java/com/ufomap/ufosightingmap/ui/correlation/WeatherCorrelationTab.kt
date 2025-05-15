package com.ufomap.ufosightingmap.ui.correlation

// Keep existing imports for Compose elements
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.roundToInt

/**
 * Tab for displaying weather correlation analysis.
 */
@Composable
fun WeatherCorrelationTab(viewModel: CorrelationViewModel) { // Pass the CorrelationViewModel.kt

    // Collect states from the ViewModel
    val weatherTypeDistributions by viewModel.weatherTypeDistribution.collectAsState()
    val unusualWeatherPercentage by viewModel.unusualWeatherPercentage.collectAsState()
    val unusualWeatherEvents by viewModel.unusualWeatherEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState() // Use loading state if needed

    // Get the context for potentially showing Toasts or messages
    val context = LocalContext.current

    // Main column for the tab content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make content scrollable
    ) {

        // Use the collected states in your Composables
        KeyStatisticsCard(
            unusualWeatherPercentage = unusualWeatherPercentage,
            mostCommonCondition = weatherTypeDistributions.maxByOrNull { it.event_count }?.weather_type
            // Pass other stats if available from ViewModel
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && weatherTypeDistributions.isEmpty()) {
            // Show loading indicator centered if data is loading and not yet available
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (weatherTypeDistributions.isNotEmpty()) {
            WeatherTypeDistributionCard(
                weatherTypeDistributions = weatherTypeDistributions
            )
        } else {
            // Show a message if data is empty and not loading
            Text(
                "Weather distribution data not available.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder for Weather-Shape Correlation - Requires specific backend logic/data
        // Consider adding a message indicating this section is under development or showing basic stats if available.
        // WeatherCorrelationCard() // Keep using placeholders or implement data source
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                "Weather-Shape Correlation section under development.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && unusualWeatherEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (unusualWeatherEvents.isNotEmpty()){
            WeatherEventsCard(events = unusualWeatherEvents)
        } else if (!isLoading) {
            Text(
                "No recent unusual weather events found.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ResearchSourceSection()

        Spacer(modifier = Modifier.height(80.dp)) // Padding at the bottom
    }
}

// --- Reusable Composables (Minor adjustments to accept actual data) ---

/** Card showing key statistics about weather correlation */
@Composable
private fun KeyStatisticsCard(
    unusualWeatherPercentage: Float,
    mostCommonCondition: WeatherEvent.WeatherType? // Pass calculated common condition
) {
    val percentFormatter = remember { NumberFormat.getPercentInstance() }
    percentFormatter.maximumFractionDigits = 1 // Format to one decimal place

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top // Align items to the top
            ) {
                StatisticItem(
                    title = "Unusual Weather",
                    value = percentFormatter.format(unusualWeatherPercentage / 100f), // Use the passed value
                    subtitle = "of sightings coincide with unusual weather",
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // Add spacing between items
                StatisticItem(
                    title = "Most Common",
                    value = mostCommonCondition?.let { formatWeatherType(it) } ?: "N/A",
                    subtitle = "weather condition during sightings",
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
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

/** Individual statistic item */
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center // Center align title
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center // Center align value
        )
        Spacer(modifier = Modifier.height(4.dp)) // Add space before subtitle
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


/** Card showing distribution of weather types during UFO sightings */
@Composable
private fun WeatherTypeDistributionCard(
    weatherTypeDistributions: List<WeatherTypeDistribution>
) {
    if (weatherTypeDistributions.isEmpty()) {
        return // Don't display card if data is empty
    }

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
                text = "Weather Type Distribution During Sightings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val totalCount = weatherTypeDistributions.sumOf { it.event_count }.toFloat()
            // Limit the number of items shown or make the list scrollable if needed
            val itemsToShow = weatherTypeDistributions.sortedByDescending { it.event_count }.take(8)

            Column {
                itemsToShow.forEach { item ->
                    val percentage = if (totalCount > 0) item.event_count / totalCount else 0f
                    WeatherTypeItem(
                        weatherType = item.weather_type,
                        count = item.event_count,
                        percentage = percentage
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                if (weatherTypeDistributions.size > itemsToShow.size) {
                    Text(
                        "... and ${weatherTypeDistributions.size - itemsToShow.size} other types",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/** Individual weather type item with progress bar */
@Composable
private fun WeatherTypeItem(
    weatherType: WeatherEvent.WeatherType,
    count: Int,
    percentage: Float
) {
    val percentFormatter = remember { NumberFormat.getPercentInstance() }
    percentFormatter.maximumFractionDigits = 1

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatWeatherType(weatherType),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f) // Allow text to take available space
            )
            Spacer(Modifier.width(8.dp)) // Add space
            Text(
                text = "$count (${percentFormatter.format(percentage)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End // Align count/percentage to the right
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator( // Use LinearProgressIndicator for consistency
            progress = { percentage }, // Use the overload accepting a lambda returning Float
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = getWeatherTypeColor(weatherType),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}


/** Card showing correlation between weather conditions and sighting shapes (Placeholder) */
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
                text = "Weather-Shape Correlation (Example)", // Indicate placeholder
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Correlation between specific weather conditions and reported UFO shapes. (Actual data pending implementation)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weather Condition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Common Shapes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("Correlation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
            }
            Divider()

            // Example Data (Replace with LazyColumn and real data when available)
            getPlaceholderWeatherShapeCorrelations().forEach { correlation ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(correlation.weatherCondition, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(correlation.commonShapes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                    Text(
                        correlation.correlationStrength,
                        style = MaterialTheme.typography.bodyMedium,
                        color = getCorrelationColor(correlation.correlationStrength), // Use helper for color
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                }
                Divider()
            }
        }
    }
}

/** Card showing recent unusual weather events */
@Composable
private fun WeatherEventsCard(events: List<WeatherEvent>) {
    if (events.isEmpty()) return // Don't show card if no events

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
                text = "Recent Unusual Weather Events",
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

            // Use LazyColumn if list can be long
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp) // Constrain height
            ) {
                items(events) { event ->
                    WeatherEventItem(event)
                    Divider()
                }
            }
        }
    }
}

/** Individual weather event item */
@Composable
private fun WeatherEventItem(event: WeatherEvent) {
    // Use a safer date formatter
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US) }
    val formattedDate = remember(event.date) { // Calculate only when event.date changes
        try {
            dateFormatter.format(Date(event.date))
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(event.city ?: event.state ?: "Unknown Location") // Show state if city is null
                        if (!event.city.isNullOrBlank() && !event.state.isNullOrBlank()) {
                            append(", ${event.state}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(getWeatherTypeColor(event.type))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatWeatherType(event.type),
                    style = MaterialTheme.typography.labelSmall, // Use smaller text for badge
                    color = Color.White // Ensure contrast
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Display basic details if available
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            event.temperature?.let {
                Text(text = "Temp: ${it.roundToInt()}°C", style = MaterialTheme.typography.bodySmall)
            }
            event.visibility?.let {
                Text(text = "Vis: ${String.format("%.1f km", it)}", style = MaterialTheme.typography.bodySmall)
            }
            if (event.hasInversionLayer) {
                Text("Inversion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (event.hasLightRefractionConditions && !event.hasInversionLayer) { // Show only if not already inversion
                Text("Refraction Conditions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}


/** Section with research citations and methods */
@Composable
private fun ResearchSourceSection() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Research Basis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Research has shown correlations between unusual weather conditions (particularly thunderstorms, temperature inversions, and fog) and reported UFO sightings. These conditions can create optical illusions and unusual light refraction.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Method: Spatiotemporal correlation between weather events and UFO sightings within 50km and 24 hours.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Data sources: National Weather Service, NOAA Storm Events Database, National UFO Reporting Center (NUFORC).", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- Helper functions ---

/** Format weather type enum to a readable string */
private fun formatWeatherType(type: WeatherEvent.WeatherType): String {
    // Replace underscores with spaces and capitalize words
    return type.name.replace('_', ' ').lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/** Get color for a weather type */
private fun getWeatherTypeColor(type: WeatherEvent.WeatherType): Color {
    // Using slightly different colors for potentially better contrast/differentiation
    return when (type) {
        WeatherEvent.WeatherType.THUNDERSTORM -> Color(0xFF7B1FA2) // Purple
        WeatherEvent.WeatherType.LIGHTNING -> Color(0xFFD81B60) // Pinkish
        WeatherEvent.WeatherType.FOG -> Color(0xFF78909C) // Blue Grey
        WeatherEvent.WeatherType.TEMPERATURE_INVERSION -> Color(0xFFF57C00) // Orange
        WeatherEvent.WeatherType.HEAVY_RAIN -> Color(0xFF1976D2) // Blue
        WeatherEvent.WeatherType.SNOW -> Color(0xFF81D4FA) // Light Blue
        WeatherEvent.WeatherType.HAIL -> Color(0xFF00ACC1) // Cyan
        WeatherEvent.WeatherType.TORNADO -> Color(0xFF5E35B1) // Deep Purple
        WeatherEvent.WeatherType.HURRICANE -> Color(0xFF0D47A1) // Dark Blue
        WeatherEvent.WeatherType.CLEAR_SKY -> Color(0xFF0288D1) // Light Blue
        WeatherEvent.WeatherType.AURORA -> Color(0xFF388E3C) // Green
        WeatherEvent.WeatherType.SPRITES -> Color(0xFF00C853) // Bright Green
        WeatherEvent.WeatherType.BALL_LIGHTNING -> Color(0xFFFF6F00) // Amber
        WeatherEvent.WeatherType.OTHER -> Color(0xFF616161) // Grey
    }
}

/** Get color for correlation strength (Example) */
private fun getCorrelationColor(strength: String): Color {
    return when (strength.lowercase()) {
        "strong" -> Color(0xFF2E7D32) // Dark Green
        "moderate" -> Color(0xFFFFA000) // Amber
        "weak" -> Color(0xFF1976D2) // Blue
        else -> Color(0xFF616161) // Grey
    }
}

// --- Placeholder Data (Keep for example/fallback if needed, or remove if fully data-driven) ---

/** Data class for placeholder weather-shape correlation */
private data class WeatherShapeCorrelation(
    val weatherCondition: String,
    val commonShapes: String,
    val correlationStrength: String
)

/** Generate placeholder weather-shape correlation data */
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