package com.ufomap.ufosightingmap.ui

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ufomap.ufosightingmap.R
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
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

    var isLoading by remember { mutableStateOf(true) }
    var mapView: MapView? by remember { mutableStateOf(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

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

    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    val userSubmittedMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
            ?: defaultMarkerIcon
    }

    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

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
        }
    ) { paddingValues ->
        // The main content area (Map and Loading Indicator)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map view
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
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
                        Log.d("MapScreen", "MapView created and initialized.")
                    }
                },
                update = { view ->
                    Log.d("MapScreen", "Updating MapView with ${sightings.size} sightings.")

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

            // Report Sighting button (moved outside of Scaffold's floatingActionButton)
            FloatingActionButton(
                onClick = onReportSighting,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .zIndex(2f), // Ensure it appears above the map
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Report Sighting"
                )
            }

            // My Location button (moved outside of Scaffold's floatingActionButton)
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
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .zIndex(2f), // Ensure it appears above the map
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                    contentDescription = "My Location"
                )
            }

            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(3f) // Keep this on top of everything
                )
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