package com.ufomap.ufosightingmap.ui.correlation

import androidx.compose.runtime.remember
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ufomap.ufosightingmap.data.correlation.dao.EventTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.MeteorShowerCorrelation
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithAstronomicalEvents
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab for displaying astronomical event correlation analysis.
 * Shows statistics and visualizations about the relationship
 * between UFO sightings and astronomical events.
 */
@Composable
fun AstronomicalCorrelationTab(viewModel: CorrelationViewModel) {
    // State
    val astronomicalEvents by viewModel.astronomicalEvents.collectAsState()
    val sightingsWithEvents by viewModel.sightingsWithAstronomicalEvents.collectAsState()
    val eventTypeDistribution by viewModel.eventTypeDistribution.collectAsState()
    val meteorShowerCorrelation by viewModel.meteorShowerCorrelation.collectAsState()
    val correlationPercentage by viewModel.astronomicalEventCorrelationPercentage.collectAsState()

    // Formatter for percentages
    val percentFormatter = remember { NumberFormat.getPercentInstance() }

    // Date formatter
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Key statistic summary
        KeyStatisticsCard(
            eventCount = astronomicalEvents.size,
            correlationPercentage = correlationPercentage,
            percentFormatter = percentFormatter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Event type distribution
        if (eventTypeDistribution.isNotEmpty()) {
            EventTypeDistributionCard(
                distribution = eventTypeDistribution
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Meteor shower correlation
        if (meteorShowerCorrelation.isNotEmpty()) {
            MeteorShowerCorrelationCard(
                correlations = meteorShowerCorrelation.take(5), // Top 5
                percentFormatter = percentFormatter
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sightings during astronomical events
        if (sightingsWithEvents.isNotEmpty()) {
            SightingsDuringEventsCard(
                sightings = sightingsWithEvents.take(10) // Top 10
            )
        }

        // Research citation
        ResearchSourceSection()

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
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
 * Card showing key statistics about astronomical event correlation
 */
@Composable
private fun KeyStatisticsCard(
    eventCount: Int,
    correlationPercentage: Float,
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
                text = "Astronomical Event Correlation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Event Count
                StatisticItem(
                    title = "Astronomical Events",
                    value = eventCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                // Correlation Percentage
                StatisticItem(
                    title = "Sightings During Events",
                    value = percentFormatter.format(correlationPercentage / 100),
                    subtitle = "of all sightings",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis of the relationship between UFO sightings and astronomical events such as meteor showers, planetary conjunctions, and satellite passes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card showing distribution of sightings by astronomical event type
 */
@Composable
private fun EventTypeDistributionCard(
    distribution: List<EventTypeDistribution>
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
                text = "Sightings by Event Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate total for percentages
            val totalSightings = distribution.sumOf { it.sighting_count }

            // Event type list
            Column {
                distribution.forEach { item ->
                    EventTypeItem(
                        eventType = item.event_type,
                        count = item.sighting_count,
                        percentage = if (totalSightings > 0) item.sighting_count.toFloat() / totalSightings else 0f
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
 * Individual event type item with progress bar
 */
@Composable
private fun EventTypeItem(
    eventType: AstronomicalEvent.EventType,
    count: Int,
    percentage: Float
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Event type name
            Text(
                text = formatEventType(eventType),
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
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Card showing correlation between meteor showers and "fireball" sightings
 */
@Composable
private fun MeteorShowerCorrelationCard(
    correlations: List<MeteorShowerCorrelation>,
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
                text = "Meteor Shower Correlation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Correlation between meteor shower peak dates and UFO sightings, particularly 'fireball' shaped objects.",
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
                    text = "Meteor Shower",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.8f)
                )

                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )

                Text(
                    text = "Fireballs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )

                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.6f)
                )
            }

            Divider()

            // Meteor shower list
            Column {
                correlations.forEach { correlation ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Meteor shower name
                        Text(
                            text = correlation.event_name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.8f)
                        )

                        // Total sightings
                        Text(
                            text = correlation.sighting_count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )

                        // Fireball count
                        Text(
                            text = correlation.fireball_count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )

                        // Percentage
                        Text(
                            text = percentFormatter.format(correlation.getFireballPercentage() / 100),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Divider()
                }
            }
        }
    }
}

/**
 * Card showing sightings that occurred during astronomical events
 */
@Composable
private fun SightingsDuringEventsCard(
    sightings: List<SightingWithAstronomicalEvents>
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
                text = "Sightings During Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "UFO sightings that coincided with astronomical events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sightings list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(sightings) { sighting ->
                    SightingWithEventItem(sighting)
                    Divider()
                }
            }
        }
    }
}

/**
 * Individual sighting with concurrent event item
 */
@Composable
private fun SightingWithEventItem(sighting: SightingWithAstronomicalEvents) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Sighting information
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )

            // Location and date
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = buildString {
                        append(sighting.city ?: "Unknown")
                        if (!sighting.state.isNullOrBlank()) {
                            append(", ")
                            append(sighting.state)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = sighting.dateTime?.substringBefore("T") ?: "Unknown date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Shape
            if (!sighting.shape.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = sighting.shape,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Concurrent events
        Text(
            text = "During: ${sighting.concurrent_event_names ?: "Unknown events"}",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                text = "Many studies indicate that astronomical phenomena are often misidentified as UFOs. " +
                        "Analysis shows strong correlations between meteor shower peaks and 'fireball' UFO reports, " +
                        "as well as between satellite passes and 'formation' sightings.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Method: Date correlation between astronomical event periods and UFO sighting reports.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Data sources: International Meteor Organization, NASA/JPL, Space-Track.org, National UFO Reporting Center (NUFORC).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Format event type enum to a readable string
 */
private fun formatEventType(type: AstronomicalEvent.EventType): String {
    return when (type) {
        AstronomicalEvent.EventType.METEOR_SHOWER -> "Meteor Shower"
        AstronomicalEvent.EventType.PLANETARY_CONJUNCTION -> "Planetary Conjunction"
        AstronomicalEvent.EventType.BRIGHT_PLANET -> "Bright Planet"
        AstronomicalEvent.EventType.SATELLITE_PASS -> "Satellite Pass"
        AstronomicalEvent.EventType.LUNAR_PHASE -> "Lunar Phase"
        AstronomicalEvent.EventType.STARLINK_TRAIN -> "Starlink Train"
        AstronomicalEvent.EventType.ASTEROID_PASS -> "Asteroid Pass"
        AstronomicalEvent.EventType.COMET -> "Comet"
        AstronomicalEvent.EventType.ATMOSPHERIC_PHENOMENON -> "Atmospheric Phenomenon"
        AstronomicalEvent.EventType.OTHER -> "Other Event"
    }
}