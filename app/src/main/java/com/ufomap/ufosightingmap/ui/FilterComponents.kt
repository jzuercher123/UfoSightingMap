package com.ufomap.ufosightingmap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ufomap.ufosightingmap.data.FilterState
import androidx.compose.foundation.horizontalScroll

/**
 * A bottom sheet for filtering UFO sightings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    onDismiss: () -> Unit,
    onApplyFilters: (
        shape: String?,
        state: String?,
        country: String?
    ) -> Unit,
    onClearFilters: () -> Unit,
    sheetState: SheetState
) {
    // Local state for the filter values
    var selectedShape by remember { mutableStateOf(filterState.shape) }
    var selectedState by remember { mutableStateOf(filterState.state) }
    var selectedCountry by remember { mutableStateOf(filterState.country) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Filter Sightings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shape filter dropdown
            ShapeFilterDropdown(
                selectedShape = selectedShape,
                onShapeSelected = { selectedShape = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State filter
            StateFilterDropdown(
                selectedState = selectedState,
                onStateSelected = { selectedState = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Country filter
            CountryFilterDropdown(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onClearFilters()
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear filters"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onApplyFilters(selectedShape, selectedState, selectedCountry)
                        onDismiss()
                    }
                ) {
                    Text("Apply Filters")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeFilterDropdown(
    selectedShape: String?,
    onShapeSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // UFO shape types based on the sample data
    val shapes = listOf(
        "Light", "Triangle", "Circle", "Formation", "Disk",
        "Sphere", "Fireball", "Cigar", "Unknown", "Other",
        "Oval", "Chevron", "Teardrop", "Diamond", "Changing"
    )

    Column {
        Text(
            "UFO Shape",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedShape ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Select shape") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Add "All" option at the top
                DropdownMenuItem(
                    text = { Text("All Shapes") },
                    onClick = {
                        onShapeSelected(null)
                        expanded = false
                    }
                )

                // Add shape options
                shapes.forEach { shape ->
                    DropdownMenuItem(
                        text = { Text(shape) },
                        onClick = {
                            onShapeSelected(shape)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateFilterDropdown(
    selectedState: String?,
    onStateSelected: (String?) -> Unit
) {
    // Common US states based on sample data
    var expanded by remember { mutableStateOf(false) }
    val states = listOf("IL", "CA", "TX", "NY", "FL", "WA", "CO", "AZ", "MA", "NV", "NM", "GA", "MO", "OR", "MI", "TN", "IN")

    Column {
        Text(
            "State/Province",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedState ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Select state") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Add "All" option
                DropdownMenuItem(
                    text = { Text("All States") },
                    onClick = {
                        onStateSelected(null)
                        expanded = false
                    }
                )

                // Add state options
                states.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            onStateSelected(state)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryFilterDropdown(
    selectedCountry: String?,
    onCountrySelected: (String?) -> Unit
) {
    // Based on sample data, most sightings are in USA but we can add other countries
    var expanded by remember { mutableStateOf(false) }
    val countries = listOf("USA", "Canada", "Mexico", "United Kingdom", "Australia")

    Column {
        Text(
            "Country",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCountry ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Select country") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Add "All" option
                DropdownMenuItem(
                    text = { Text("All Countries") },
                    onClick = {
                        onCountrySelected(null)
                        expanded = false
                    }
                )

                // Add country options
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country) },
                        onClick = {
                            onCountrySelected(country)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Component to display active filter chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFilterChips(
    filterState: FilterState,
    onClearFilter: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (!filterState.hasActiveFilters()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Create a scrollable row of filter chips
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add chip for each active filter
            filterState.shape?.let {
                FilterChip(
                    selected = true,
                    onClick = { /* No action on click, just clear */ },
                    label = { Text("Shape: $it") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filter",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = { onClearFilter("shape") }
                )
            }

            filterState.state?.let {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("state") },
                    label = { Text("State: $it") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filter"
                        )
                    }
                )
            }

            filterState.country?.let {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("country") },
                    label = { Text("Country: $it") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filter"
                        )
                    }
                )
            }

            filterState.searchText?.let {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("searchText") },
                    label = { Text("Search: $it") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filter"
                        )
                    }
                )
            }

            // Show clear all chip if multiple filters are active
            if (listOfNotNull(
                    filterState.shape,
                    filterState.state,
                    filterState.country,
                    filterState.searchText
                ).size > 1
            ) {
                FilterChip(
                    selected = true,
                    onClick = { onClearAll() },
                    label = { Text("Clear All") }
                )
            }
        }
    }
}