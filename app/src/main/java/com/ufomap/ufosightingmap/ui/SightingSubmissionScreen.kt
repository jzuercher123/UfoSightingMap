package com.ufomap.ufosightingmap.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ufomap.ufosightingmap.viewmodel.SightingSubmissionViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightingSubmissionScreen(
    viewModel: SightingSubmissionViewModel,
    onNavigateBack: () -> Unit
) {
    val submissionState by viewModel.submissionState.collectAsState()

    // Form state
    var dateTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<Long?>(null) }

    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("USA") }
    var shape by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    // Location state
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var useCurrentLocation by remember { mutableStateOf(false) }

    // Context for location access
    val context = LocalContext.current

    // Validation state
    var dateTimeError by remember { mutableStateOf(false) }
    var cityError by remember { mutableStateOf(false) }
    var countryError by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }

    // Shape dropdown state
    var shapeExpanded by remember { mutableStateOf(false) }
    val shapeOptions = listOf(
        "Light", "Triangle", "Circle", "Formation", "Disk",
        "Sphere", "Fireball", "Cigar", "Unknown", "Other",
        "Oval", "Chevron", "Teardrop", "Diamond", "Changing"
    )

    // Location updates
    LaunchedEffect(useCurrentLocation) {
        if (useCurrentLocation) {
            // Get location from device - simplified here, add proper permission checks
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                }
            } catch (e: Exception) {
                // Handle location error
            }
        }
    }

    // Submission success effect
    LaunchedEffect(submissionState) {
        if (submissionState is SightingSubmissionViewModel.SubmissionState.Success) {
            Toast.makeText(context, "Sighting reported successfully!", Toast.LENGTH_LONG).show()
            delay(1500) // Short delay before navigating back
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report UFO Sighting") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Date/Time picker - simplified, implement date/time pickers
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text("Date and Time*") },
                    placeholder = { Text("YYYY-MM-DD HH:MM") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = dateTime.isEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // City field
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = city.isEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State field
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State/Province") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country field
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = country.isEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shape dropdown - implement dropdown with shapes
                OutlinedTextField(
                    value = shape,
                    onValueChange = { shape = it },
                    label = { Text("UFO Shape") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration field
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    placeholder = { Text("e.g. 5 minutes, 30 seconds") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description field
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Description*") },
                    placeholder = { Text("Describe what you saw") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    isError = summary.isEmpty(),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = useCurrentLocation,
                                onCheckedChange = { useCurrentLocation = it }
                            )
                            Text("Use my current location")
                        }

                        if (!useCurrentLocation) {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = latitude.toString(),
                                onValueChange = {
                                    latitude = it.toDoubleOrNull() ?: latitude
                                },
                                label = { Text("Latitude") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = longitude.toString(),
                                onValueChange = {
                                    longitude = it.toDoubleOrNull() ?: longitude
                                },
                                label = { Text("Longitude") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Latitude: $latitude")
                            Text("Longitude: $longitude")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        viewModel.submitSighting(
                            dateTime = dateTime,
                            city = city,
                            state = state.takeIf { it.isNotEmpty() },
                            country = country,
                            shape = shape.takeIf { it.isNotEmpty() },
                            duration = duration.takeIf { it.isNotEmpty() },
                            summary = summary,
                            latitude = latitude,
                            longitude = longitude
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = dateTime.isNotEmpty() && city.isNotEmpty() &&
                            country.isNotEmpty() && summary.isNotEmpty() &&
                            submissionState !is SightingSubmissionViewModel.SubmissionState.Submitting
                ) {
                    Text("Submit Sighting Report")
                }

                if (submissionState is SightingSubmissionViewModel.SubmissionState.Submitting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                if (submissionState is SightingSubmissionViewModel.SubmissionState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (submissionState as SightingSubmissionViewModel.SubmissionState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}