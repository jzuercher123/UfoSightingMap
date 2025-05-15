package com.ufomap.ufosightingmap.ui.correlation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tooltip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.ui.correlation.components.ChartType

/**
 * Toolbar for the correlation analysis screen.
 * Provides controls for changing chart type, refreshing data, exporting results,
 * and displaying information about the correlation analysis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrelationToolbar(
    modifier: Modifier = Modifier,
    title: String = "Correlation Analysis",
    onRefresh: () -> Unit = {},
    onExport: () -> Unit = {},
    onChartTypeChanged: (ChartType) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    chartType: ChartType = ChartType.SCATTER,
    hasActiveFilters: Boolean = false
) {
    var showChartTypeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Main toolbar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Right-side actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chart type selector
                    Box {
                        Tooltip(
                            text = { Text("Change Chart Type") }
                        ) {
                            IconButton(onClick = { showChartTypeMenu = true }) {
                                when (chartType) {
                                    ChartType.SCATTER -> Icon(Icons.Default.ScatterPlot, "Scatter Plot")
                                    ChartType.BAR -> Icon(Icons.Default.BarChart, "Bar Chart")
                                    ChartType.LINE -> Icon(Icons.Default.ShowChart, "Line Chart")
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showChartTypeMenu,
                            onDismissRequest = { showChartTypeMenu = false }
                        ) {
                            ChartTypeMenuItem(
                                title = "Scatter Plot",
                                icon = Icons.Default.ScatterPlot,
                                selected = chartType == ChartType.SCATTER,
                                onClick = {
                                    onChartTypeChanged(ChartType.SCATTER)
                                    showChartTypeMenu = false
                                }
                            )

                            ChartTypeMenuItem(
                                title = "Bar Chart",
                                icon = Icons.Default.BarChart,
                                selected = chartType == ChartType.BAR,
                                onClick = {
                                    onChartTypeChanged(ChartType.BAR)
                                    showChartTypeMenu = false
                                }
                            )

                            ChartTypeMenuItem(
                                title = "Line Chart",
                                icon = Icons.Default.ShowChart,
                                selected = chartType == ChartType.LINE,
                                onClick = {
                                    onChartTypeChanged(ChartType.LINE)
                                    showChartTypeMenu = false
                                }
                            )
                        }
                    }

                    // Refresh button
                    Tooltip(
                        text = { Text("Refresh Data") }
                    ) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }

                    // Export button
                    Tooltip(
                        text = { Text("Export Results") }
                    ) {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.Download, "Export")
                        }
                    }

                    // Info button
                    Tooltip(
                        text = { Text("Information") }
                    ) {
                        IconButton(onClick = onInfoClick) {
                            Icon(Icons.Default.Info, "Information")
                        }
                    }

                    // Settings button
                    Tooltip(
                        text = { Text("Settings") }
                    ) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
            }

            // Filter status bar - only show if there are active filters
            if (hasActiveFilters) {
                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters Applied",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onClearFilters,
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Filters",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTypeMenuItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(title) },
        leadingIcon = { Icon(icon, contentDescription = title) },
        onClick = onClick,
        colors = if (selected) {
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.primary,
                leadingIconColor = MaterialTheme.colorScheme.primary
            )
        } else {
            MenuDefaults.itemColors()
        }
    )
}

/**
 * Displays correlation analysis information in a card format
 */
@Composable
fun CorrelationInfoCard(
    correlationValue: Double,
    pValue: Double,
    sampleSize: Int,
    interpretation: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Correlation Analysis Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    title = "Correlation",
                    value = String.format("%.3f", correlationValue)
                )

                StatisticItem(
                    title = "P-Value",
                    value = String.format("%.4f", pValue)
                )

                StatisticItem(
                    title = "Sample Size",
                    value = sampleSize.toString()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Correlation strength indicator
            val (strengthText, strengthColor) = when {
                correlationValue.isNaN() -> "Invalid" to MaterialTheme.colorScheme.error
                Math.abs(correlationValue) >= 0.8 -> "Very Strong" to MaterialTheme.colorScheme.primary
                Math.abs(correlationValue) >= 0.6 -> "Strong" to MaterialTheme.colorScheme.primary
                Math.abs(correlationValue) >= 0.4 -> "Moderate" to MaterialTheme.colorScheme.secondary
                Math.abs(correlationValue) >= 0.2 -> "Weak" to MaterialTheme.colorScheme.tertiary
                else -> "Very Weak" to MaterialTheme.colorScheme.error
            }

            Text(
                text = "Correlation Strength: $strengthText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = strengthColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Statistical significance
            val significantText = if (pValue < 0.05) "Statistically Significant" else "Not Statistically Significant"
            Text(
                text = "Statistical Significance: $significantText (p-value: ${String.format("%.4f", pValue)})",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interpretation
            Text(
                text = "Interpretation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = interpretation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}