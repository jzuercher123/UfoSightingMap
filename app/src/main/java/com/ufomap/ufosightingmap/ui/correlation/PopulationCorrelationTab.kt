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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * Tab for displaying population correlation analysis.
 * Shows statistics and visualizations about the relationship
 * between UFO sightings and population density.
 */
@Composable
fun PopulationCorrelationTab(viewModel: CorrelationViewModel) {
    // State
    val isLoading by viewModel.isLoading.collectAsState()

    // Placeholder data until actual implementation
    val populationDensityDistributions = remember {
        listOf(
            PopulationDensityDistribution("Rural (< 10 pop/km²)", 245),
            PopulationDensityDistribution("Suburban (10-100 pop/km²)", 612),
            PopulationDensityDistribution("Urban (100-1000 pop/km²)", 892),
            PopulationDensityDistribution("Metro (> 1000 pop/km²)", 423)
        )
    }

    // Formatter for percentages
    val percentFormatter = remember { NumberFormat.getPercentInstance() }
    percentFormatter.maximumFractionDigits = 1

    // Main column for the tab content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Key statistic summary
        KeyStatisticsCard(
            populationAveragePercentile = 76.5f, // Placeholder value
            ruralVsUrbanRatio = 0.32f, // Placeholder value
            percentFormatter = percentFormatter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Population density distribution
        PopulationDensityDistributionCard(
            distributions = populationDensityDistributions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Per capita visualization
        SightingsPerCapitaCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Demographics factors
        DemographicFactorsCard()

        // Research citation
        ResearchSourceSection()

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
    }
}

/**
 * Card showing key statistics about population correlation
 */
@Composable
private fun KeyStatisticsCard(
    populationAveragePercentile: Float,
    ruralVsUrbanRatio: Float,
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
                text = "Population Correlation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Population Percentile
                StatisticItem(
                    title = "Population Percentile",
                    value = "${populationAveragePercentile.roundToInt()}%",
                    subtitle = "average for sightings",
                    modifier = Modifier.weight(1f)
                )

                // Rural vs Urban
                StatisticItem(
                    title = "Rural vs Urban",
                    value = percentFormatter.format(ruralVsUrbanRatio),
                    subtitle = "sightings ratio",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis of the relationship between UFO sightings and population density, showing how sighting reports correlate with urban vs. rural areas.",
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
 * Card showing distribution of sightings by population density
 */
@Composable
private fun PopulationDensityDistributionCard(
    distributions: List<PopulationDensityDistribution>
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
                text = "Sightings by Population Density",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate total for percentages
            val totalSightings = distributions.sumOf { it.sighting_count }

            // Population density list
            Column {
                distributions.forEach { item ->
                    val percentage = if (totalSightings > 0)
                        item.sighting_count.toFloat() / totalSightings
                    else 0f

                    PopulationDensityItem(
                        densityCategory = item.density_category,
                        count = item.sighting_count,
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
 * Individual population density item with progress bar
 */
@Composable
private fun PopulationDensityItem(
    densityCategory: String,
    count: Int,
    percentage: Float
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Density category
            Text(
                text = densityCategory,
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
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

/**
 * Card showing per capita sighting rate visualization
 */
@Composable
private fun SightingsPerCapitaCard() {
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
                text = "Sightings Per Capita",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Visualization of UFO sighting rates normalized by population, showing areas with disproportionate reporting rates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Placeholder for actual implementation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Per Capita Visualization\n(Coming Soon)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Note: This visualization will show the relationship between population size and sighting frequency, highlighting areas with disproportionately high or low reporting rates.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

/**
 * Card showing demographic factors correlated with sightings
 */
@Composable
private fun DemographicFactorsCard() {
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
                text = "Demographic Factors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Analysis of demographic factors correlated with UFO sighting reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Example demographic factors (placeholder data)
            DemographicFactorRow("Median Age", "Higher sighting rates in areas with median age 30-45")
            DemographicFactorRow("Education Level", "Slight positive correlation with college education rates")
            DemographicFactorRow("Internet Access", "Strong positive correlation with broadband availability")
            DemographicFactorRow("Income Level", "No significant correlation with median income")
        }
    }
}

/**
 * Individual demographic factor row
 */
@Composable
private fun DemographicFactorRow(factor: String, finding: String) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = factor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = finding,
            style = MaterialTheme.typography.bodyMedium
        )

        Divider(
            modifier = Modifier.padding(top = 8.dp)
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
                text = "Studies have found a sub-linear relationship between population density and UFO sightings, indicating that while more people means more potential observers, the relationship is not directly proportional.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Method: Sightings normalized by population density to identify deviations from expected distribution.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Data sources: US Census Bureau, World Bank population data, National UFO Reporting Center (NUFORC).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}