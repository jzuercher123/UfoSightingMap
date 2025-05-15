package com.ufomap.ufosightingmap.ui.correlation.components


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A reusable chart component for displaying correlation analysis results.
 * Supports scatter plots, bar charts, and line charts for visualizing
 * relationships between UFO sightings and various factors.
 */
@Composable
fun CorrelationResultsChart(
    modifier: Modifier = Modifier,
    data: List<ChartDataPoint>,
    chartType: ChartType = ChartType.SCATTER,
    xAxisLabel: String = "X",
    yAxisLabel: String = "Y",
    chartColor: Color = Color(0xFF6200EE),
    showGridLines: Boolean = true,
    animateChanges: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .aspectRatio(1.5f)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val chartPadding = 40f

        val chartWidth = canvasWidth - (chartPadding * 2)
        val chartHeight = canvasHeight - (chartPadding * 2)

        if (data.isEmpty()) {
            // Draw "No Data Available" text
            return@Canvas
        }

        // Find data range
        val xValues = data.map { it.x }
        val yValues = data.map { it.y }

        val minX = xValues.minOrNull() ?: 0.0
        val maxX = xValues.maxOrNull() ?: 1.0
        val minY = yValues.minOrNull() ?: 0.0
        val maxY = yValues.maxOrNull() ?: 1.0

        // Draw chart background
        drawRect(
            color = Color.LightGray.copy(alpha = 0.2f),
            topLeft = Offset(chartPadding, chartPadding),
            size = Size(chartWidth, chartHeight)
        )

        // Draw grid lines if enabled
        if (showGridLines) {
            drawGridLines(chartPadding, chartWidth, chartHeight, 5)
        }

        // Draw axes
        drawLine(
            color = Color.Black,
            start = Offset(chartPadding, chartPadding + chartHeight),
            end = Offset(chartPadding + chartWidth, chartPadding + chartHeight),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.Black,
            start = Offset(chartPadding, chartPadding + chartHeight),
            end = Offset(chartPadding, chartPadding),
            strokeWidth = 2f
        )

        // Draw data points based on chart type
        when (chartType) {
            ChartType.SCATTER -> drawScatterPlot(
                data, chartPadding, chartWidth, chartHeight, minX, maxX, minY, maxY, chartColor
            )
            ChartType.BAR -> drawBarChart(
                data, chartPadding, chartWidth, chartHeight, minX, maxX, minY, maxY, chartColor
            )
            ChartType.LINE -> drawLineChart(
                data, chartPadding, chartWidth, chartHeight, minX, maxX, minY, maxY, chartColor
            )
        }

        // Draw axis labels
        // X-axis
        drawText(
            textMeasurer = textMeasurer,
            text = xAxisLabel,
            style = TextStyle(fontSize = 12.sp, color = Color.Black),
            topLeft = Offset(
                chartPadding + chartWidth / 2 - 20f,
                chartPadding + chartHeight + 20f
            )
        )

        // Y-axis
        drawText(
            textMeasurer = textMeasurer,
            text = yAxisLabel,
            style = TextStyle(fontSize = 12.sp, color = Color.Black),
            topLeft = Offset(chartPadding - 30f, chartPadding + chartHeight / 2)
        )
    }
}

private fun DrawScope.drawGridLines(
    chartPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    gridCount: Int
) {
    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    // Draw horizontal grid lines
    val yStep = chartHeight / gridCount
    for (i in 0..gridCount) {
        val y = chartPadding + i * yStep
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(chartPadding, y),
            end = Offset(chartPadding + chartWidth, y),
            strokeWidth = 1f,
            pathEffect = dashPathEffect
        )
    }

    // Draw vertical grid lines
    val xStep = chartWidth / gridCount
    for (i in 0..gridCount) {
        val x = chartPadding + i * xStep
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(x, chartPadding),
            end = Offset(x, chartPadding + chartHeight),
            strokeWidth = 1f,
            pathEffect = dashPathEffect
        )
    }
}

private fun DrawScope.drawScatterPlot(
    data: List<ChartDataPoint>,
    chartPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    chartColor: Color
) {
    val xRange = maxX - minX
    val yRange = maxY - minY

    data.forEach { point ->
        // Scale data point to chart dimensions
        val x = chartPadding + (((point.x - minX) / xRange) * chartWidth).toFloat()
        val y = chartPadding + chartHeight - (((point.y - minY) / yRange) * chartHeight).toFloat()

        // Draw point
        drawCircle(
            color = point.color ?: chartColor,
            radius = point.size,
            center = Offset(x, y)
        )
    }

    // Draw trend line if there's enough data
    if (data.size > 2) {
        // Simple linear regression
        val sumX = data.sumOf { it.x }
        val sumY = data.sumOf { it.y }
        val sumXY = data.sumOf { it.x * it.y }
        val sumXX = data.sumOf { it.x * it.x }
        val n = data.size

        val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        // Draw trend line
        val startX = minX
        val startY = slope * startX + intercept
        val endX = maxX
        val endY = slope * endX + intercept

        // Scale to chart coordinates
        val x1 = chartPadding + (((startX - minX) / xRange) * chartWidth).toFloat()
        val y1 = chartPadding + chartHeight - (((startY - minY) / yRange) * chartHeight).toFloat()
        val x2 = chartPadding + (((endX - minX) / xRange) * chartWidth).toFloat()
        val y2 = chartPadding + chartHeight - (((endY - minY) / yRange) * chartHeight).toFloat()

        drawLine(
            color = Color.Red,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawBarChart(
    data: List<ChartDataPoint>,
    chartPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    chartColor: Color
) {
    val xRange = maxX - minX
    val yRange = maxY - minY

    // Sort data by x value for bar chart
    val sortedData = data.sortedBy { it.x }

    // Calculate bar width based on number of data points
    val barWidth = (chartWidth / data.size) * 0.8f
    val spacing = (chartWidth / data.size) * 0.2f

    sortedData.forEachIndexed { index, point ->
        // Position bars evenly along x-axis
        val x = chartPadding + index * (barWidth + spacing)

        // Calculate bar height from data point
        val barHeight = (((point.y - minY) / yRange) * chartHeight).toFloat()

        // Draw bar
        drawRect(
            color = point.color ?: chartColor,
            topLeft = Offset(x, chartPadding + chartHeight - barHeight),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawLineChart(
    data: List<ChartDataPoint>,
    chartPadding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    chartColor: Color
) {
    val xRange = maxX - minX
    val yRange = maxY - minY

    // Sort data by x value for line chart
    val sortedData = data.sortedBy { it.x }

    // Draw lines between points
    for (i in 0 until sortedData.size - 1) {
        val currentPoint = sortedData[i]
        val nextPoint = sortedData[i + 1]

        // Scale points to chart dimensions
        val x1 = chartPadding + (((currentPoint.x - minX) / xRange) * chartWidth).toFloat()
        val y1 = chartPadding + chartHeight - (((currentPoint.y - minY) / yRange) * chartHeight).toFloat()
        val x2 = chartPadding + (((nextPoint.x - minX) / xRange) * chartWidth).toFloat()
        val y2 = chartPadding + chartHeight - (((nextPoint.y - minY) / yRange) * chartHeight).toFloat()

        // Draw line
        drawLine(
            color = chartColor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2f
        )
    }

    // Draw points on the line
    sortedData.forEach { point ->
        // Scale data point to chart dimensions
        val x = chartPadding + (((point.x - minX) / xRange) * chartWidth).toFloat()
        val y = chartPadding + chartHeight - (((point.y - minY) / yRange) * chartHeight).toFloat()

        // Draw point
        drawCircle(
            color = point.color ?: chartColor,
            radius = point.size,
            center = Offset(x, y)
        )
    }
}

/**
 * Data point for the correlation chart
 *
 * @param x X-coordinate value
 * @param y Y-coordinate value
 * @param label Optional label for the point
 * @param color Optional custom color for this specific point
 * @param size Size of the point in scatter plot
 */
data class ChartDataPoint(
    val x: Double,
    val y: Double,
    val label: String? = null,
    val color: Color? = null,
    val size: Float = 6f
)

/**
 * Types of charts supported by the correlation results visualization
 */
enum class ChartType {
    SCATTER,
    BAR,
    LINE
}