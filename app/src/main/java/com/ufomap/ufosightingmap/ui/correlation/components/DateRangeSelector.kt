package com.ufomap.ufosightingmap.ui.correlation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A date range selector component for correlation analysis.
 * Allows selecting a start and end date, or choosing from predefined
 * date ranges like "Last Month", "Last Year", etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    modifier: Modifier = Modifier,
    onRangeSelected: (startDate: Long, endDate: Long) -> Unit = { _, _ -> },
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    label: String = "Date Range"
) {
    // Date formatting
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    // State for showing the date picker dialog
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Date range picker state
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate,
        initialSelectedEndDateMillis = initialEndDate,
        initialDisplayMode = DisplayMode.Input
    )

    // Track current selection
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }

    // Formatted date strings
    val startDateText = startDate?.let { dateFormatter.format(Date(it)) } ?: "Select Start Date"
    val endDateText = endDate?.let { dateFormatter.format(Date(it)) } ?: "Select End Date"

    // Predefined ranges
    var showPredefinedDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Display the selected range
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date display
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Start Date",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = startDateText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "End Date",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = endDateText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row {
                    // Predefined ranges dropdown
                    Box {
                        TextButton(onClick = { showPredefinedDropdown = true }) {
                            Text("Presets")
                        }

                        DropdownMenu(
                            expanded = showPredefinedDropdown,
                            onDismissRequest = { showPredefinedDropdown = false }
                        ) {
                            PredefinedDateRanges.values().forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range.label) },
                                    onClick = {
                                        val (start, end) = calculateDateRange(range)
                                        startDate = start
                                        endDate = end
                                        onRangeSelected(start, end)
                                        showPredefinedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Custom date picker button
                    TextButton(onClick = { showDateRangePicker = true }) {
                        Text("Custom")
                    }
                }
            }
        }

        // Date range picker dialog
        if (showDateRangePicker) {
            DateRangePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                onConfirm = {
                    val newStartDate = dateRangePickerState.selectedStartDateMillis
                    val newEndDate = dateRangePickerState.selectedEndDateMillis

                    if (newStartDate != null && newEndDate != null) {
                        startDate = newStartDate
                        endDate = newEndDate
                        onRangeSelected(newStartDate, newEndDate)
                    }

                    showDateRangePicker = false
                },
                dateRangePickerState = dateRangePickerState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    dateRangePickerState: DateRangePickerState
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Predefined date ranges
 */
enum class PredefinedDateRanges(val label: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    YEAR_TO_DATE("Year to Date"),
    ALL_TIME("All Time")
}

/**
 * Calculate start and end dates based on predefined ranges
 */
private fun calculateDateRange(range: PredefinedDateRanges): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    val endDate = calendar.timeInMillis

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val startDate = when (range) {
        PredefinedDateRanges.LAST_7_DAYS -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis
        }
        PredefinedDateRanges.LAST_30_DAYS -> {
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            calendar.timeInMillis
        }
        PredefinedDateRanges.LAST_90_DAYS -> {
            calendar.add(Calendar.DAY_OF_YEAR, -90)
            calendar.timeInMillis
        }
        PredefinedDateRanges.LAST_6_MONTHS -> {
            calendar.add(Calendar.MONTH, -6)
            calendar.timeInMillis
        }
        PredefinedDateRanges.LAST_YEAR -> {
            calendar.add(Calendar.YEAR, -1)
            calendar.timeInMillis
        }
        PredefinedDateRanges.YEAR_TO_DATE -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.timeInMillis
        }
        PredefinedDateRanges.ALL_TIME -> {
            // Set to arbitrary far past (e.g., Jan 1, 1970)
            0L
        }
    }

    return Pair(startDate, endDate)
}

/**
 * A simpler date selector that only allows selecting a single date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    modifier: Modifier = Modifier,
    onDateSelected: (Long) -> Unit = {},
    initialDate: Long? = null,
    label: String = "Date"
) {
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val dateText = selectedDate?.let { dateFormatter.format(Date(it)) } ?: "Select Date"

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Date display field
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            }
        )

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { date ->
                                selectedDate = date
                                onDateSelected(date)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}