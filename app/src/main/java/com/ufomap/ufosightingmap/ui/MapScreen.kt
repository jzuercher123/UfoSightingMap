package com.ufomap.ufosightingmap.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ufomap.ufosightingmap.R
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Constants for map initialization
private const val INITIAL_ZOOM_LEVEL = 4.5
private val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795) // Center of USA

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSightingClick: (Int) -> Unit,
    onReportSighting: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect states from ViewModel
    val sightings: List<Sighting> by viewModel.sightings.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    // Changed loading state to use mutableState
    var isLoading by remember { mutableStateOf(true) }
    var mapView: MapView? by remember { mutableStateOf(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    val userSubmittedMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
            ?: defaultMarkerIcon
    }

    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Test direct JSON reading
    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("sightings.json").bufferedReader().use { it.readText() }
            Log.d("MapScreen", "JSON file read success: ${jsonString.take(100)}...")
        } catch (e: Exception) {
            Log.e("MapScreen", "Error reading JSON: ${e.message}", e)
        }
    }

    // Loading state management
    LaunchedEffect(sightings) {
        Log.d("MapScreen", "LaunchedEffect triggered. Sightings count: ${sightings.size}")
        if (sightings.isNotEmpty()) {
            isLoading = false
        } else {
            // Set a timeout in case database is truly empty
            delay(5000)
            isLoading = false
        }
    }

    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    locationOverlay?.disableMyLocation()
                    mapView?.onDetach()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                locationOverlay?.disableMyLocation()
                mapView?.onDetach()
            } else {
                mapView?.onPause()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Top app bar with filter action
                TopAppBar(
                    title = { Text("UFO Sighting Map") },
                    actions = {
                        // Filter button
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }

                        // Badge showing filtered count if filters applied
                        if (filterState.hasActiveFilters()) {
                            Badge(
                                modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                            ) {
                                Text(sightings.size.toString())
                            }
                        }
                    }
                )

                // Add search bar
                SearchBar(
                    initialQuery = filterState.searchText ?: "",
                    onQueryChanged = { viewModel.updateSearchQuery(it) }
                )

                // Show active filters as chips if any are applied
                ActiveFilterChips(
                    filterState = filterState,
                    onClearFilter = { viewModel.clearFilter(it) },
                    onClearAll = { viewModel.clearFilters() }
                )
            }
        },
        floatingActionButton = {
            // Use a Box to contain and align the two FABs
            Box(
                modifier = Modifier.fillMaxWidth() // Take available width
            ) {
                // My Location button (Aligned to BottomEnd within the FAB container)
                FloatingActionButton(
                    onClick = {
                        locationOverlay?.let { overlay ->
                            if (overlay.myLocation != null) {
                                mapView?.controller?.animateTo(overlay.myLocation)
                                mapView?.controller?.setZoom(15.0)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Location not available. Make sure location is enabled.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Align within the container Box
                        .padding(16.dp), // Padding from the edges of the container
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = "My Location"
                    )
                }

                // Report Sighting button (Aligned to BottomStart within the FAB container)
                FloatingActionButton(
                    onClick = onReportSighting,
                    modifier = Modifier
                        .align(Alignment.BottomStart) // Align within the container Box
                        .padding(16.dp), // Padding from the edges of the container
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Report Sighting"
                    )
                }
            }
        }
    ) { paddingValues ->
        // The main content area (Map and Loading Indicator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold correctly
        ) {
            // Map view
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        Log.d("MapScreen", "Creating MapView")
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(true)
                        controller.setZoom(INITIAL_ZOOM_LEVEL)
                        controller.setCenter(USA_CENTER_GEOPOINT)
                        mapView = this

                        val locProvider = GpsMyLocationProvider(ctx)
                        val overlay = MyLocationNewOverlay(locProvider, this)
                        try {
                            overlay.enableMyLocation()
                        } catch (se: SecurityException) {
                            Log.w("MapScreen", "Location permission not granted for MyLocationOverlay")
                        }
                        this.overlays.add(overlay)
                        locationOverlay = overlay

                        // Add a test marker to see if markers work at all
                        val testMarker = Marker(this).apply {
                            position = GeoPoint(40.0, -90.0)
                            title = "Test Marker"
                            snippet = "This is a test marker"
                            icon = defaultMarkerIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        this.overlays.add(testMarker)
                        Log.d("MapScreen", "Added test marker at lat=40.0, lng=-90.0")

                        Log.d("MapScreen", "MapView created and initialized.")
                    }
                },
                update = { view ->
                    Log.d("MapScreen", "Map update called with ${sightings.size} sightings")

                    // Log every sighting's coordinates
                    sightings.forEachIndexed { index, sighting ->
                        Log.d("MapScreen", "Sighting $index: lat=${sighting.latitude}, lng=${sighting.longitude}")
                    }

                    // Log map center and zoom
                    Log.d("MapScreen", "Map center: ${view.mapCenter}, zoom: ${view.zoomLevelDouble}")

                    val markersToRemove = view.overlays.filterIsInstance<Marker>()
                        .filter { it.relatedObject is Sighting }
                    view.overlays.removeAll(markersToRemove)

                    if (sightings.isNotEmpty()) {
                        Log.d("MapScreen", "Creating ${sightings.size} markers")
                        try {
                            sightings.forEach { sighting ->
                                val markerIcon = if (sighting.isUserSubmitted) {
                                    userSubmittedMarkerIcon ?: defaultMarkerIcon
                                } else {
                                    defaultMarkerIcon
                                }

                                val marker = Marker(view).apply {
                                    id = sighting.id.toString()
                                    position = GeoPoint(sighting.latitude, sighting.longitude)
                                    title = sighting.city ?: "Sighting ${sighting.id}"
                                    snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
                                    icon = markerIcon
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    relatedObject = sighting
                                    infoWindow = SightingInfoWindow(view, onSightingClick)
                                    setOnMarkerClickListener { m, mv ->
                                        InfoWindow.closeAllInfoWindowsOn(mv)
                                        m.showInfoWindow()
                                        true
                                    }
                                }
                                view.overlays.add(marker)
                                Log.d("MapScreen", "Added marker for sighting ID ${sighting.id} at ${sighting.latitude},${sighting.longitude}")
                            }
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error creating markers", e)
                        }
                    } else {
                        Log.d("MapScreen", "No sightings to display markers for.")
                    }
                    view.invalidate()
                }
            )

            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1f)
                )
            }

            // Show empty state message when no sightings and not loading
            if (!isLoading && sightings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sightings found. Try adjusting filters or reload the app.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Show filter bottom sheet if needed
    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { shape, state, country ->
                viewModel.updateFilters(shape = shape, state = state, country = country)
            },
            onClearFilters = { viewModel.clearFilters() },
            sheetState = bottomSheetState
        )
    }
}