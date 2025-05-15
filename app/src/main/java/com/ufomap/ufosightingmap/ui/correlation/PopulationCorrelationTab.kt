package com.ufomap.ufosightingmap.ui.correlation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDensityDistribution
import com.ufomap.ufosightingmap.viewmodel.CorrelationViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun PopulationCorrelationTab(viewModel: CorrelationViewModel) {
    // States from ViewModel
    val populationDensityDistribution by viewModel.population
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Formatter for percentages
    val percentFormatter = remember { NumberFormat.getPercentInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Key statistics card
        KeyStatisticsCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Population density distribution chart
        if (populationDensityDistribution.isNotEmpty()) {
            PopulationDensityCard(distribution = populationDensityDistribution)
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "No population density data available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Urban vs Rural distribution
        UrbanRuralDistributionCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Research basis section
        ResearchSourceSection()

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
    }
}

@Composable
private fun KeyStatisticsCard() {
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
                // Population Density Statistic
                StatisticItem(
                    title = "Correlation Type",
                    value = "Sub-linear",
                    subtitle = "with population density",
                    modifier = Modifier.weight(1f)
                )

                // Sightings Per Capita
                StatisticItem(
                    title = "Sightings Per Capita",
                    value = "Higher",
                    subtitle = "in rural areas",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis of the relationship between UFO sightings and population density, examining urban vs. rural distribution patterns.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

@Composable
private fun PopulationDensityCard(distribution: List<PopulationDensityDistribution>) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Distribution of UFO sightings across different population density categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate total for percentages
            val totalSightings = distribution.sumOf { it.sighting_count }

            // Bar chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                PopulationDensityBarChart(
                    distribution = distribution,
                    totalCount = totalSightings,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Distribution details
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(distribution) { item ->
                    DensityItem(
                        category = item.density_category,
                        count = item.sighting_count,
                        total = totalSightings
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun PopulationDensityBarChart(
    distribution: List<PopulationDensityDistribution>,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / distribution.size - 10f

            // Draw bars
            distribution.forEachIndexed { index, item ->
                val percentage = if (totalCount > 0) item.sighting_count.toFloat() / totalCount else 0f
                val barHeight = percentage * canvasHeight

                // Bar position
                val startX = index * (barWidth + 10f) + 10f
                val startY = canvasHeight - barHeight

                // Choose color based on category
                val color = when {
                    item.density_category.contains("Rural", ignoreCase = true) -> primaryColor
                    item.density_category.contains("Suburban", ignoreCase = true) -> secondaryColor
                    else -> tertiaryColor
                }

                // Draw bar
                drawRect(
                    color = color,
                    topLeft = Offset(startX, startY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )

                // Draw outline
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(startX, startY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    style = Stroke(width = 1f)
                )
            }

            // Draw baseline
            drawLine(
                color = outlineColor,
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun DensityItem(
    category: String,
    count: Int,
    total: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Density category
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )

        // Count
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )

        // Percentage
        val percentage = if (total > 0) (count.toFloat() / total) * 100 else 0f
        Text(
            text = String.format("%.1f%%", percentage),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
private fun UrbanRuralDistributionCard() {
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
                text = "Urban vs Rural Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "UFO sightings per capita are higher in rural areas despite lower population density.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Example comparisons
            ComparisonItem(
                category = "Rural Areas",
                perCapitaRate = "1 per 6,000 people",
                color = MaterialTheme.colorScheme.primary
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonItem(
                category = "Suburban Areas",
                perCapitaRate = "1 per 12,000 people",
                color = MaterialTheme.colorScheme.secondary
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonItem(
                category = "Urban Areas",
                perCapitaRate = "1 per 20,000 people",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ComparisonItem(
    category: String,
    perCapitaRate: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = perCapitaRate,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

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