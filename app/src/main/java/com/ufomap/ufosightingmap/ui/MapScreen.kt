package com.ufomap.ufosightingmap.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ufomap.ufosightingmap.utils.MarkerClusterManager
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

// Constants for map initialization
private const val INITIAL_ZOOM_LEVEL = 3.5
private val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795) // Center of USA
private const val TAG = "MapScreen"
private const val MIN_BOUNDING_BOX_SPAN_DEGREES = 0.1
private const val LOADING_TIMEOUT_MS = 5000L
private const val MAX_ZOOM_LEVEL = 17.0

/**
 * Represents the current UI state of the map screen.
 * Using a single state object makes it easier to manage complex state changes.
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val showFilterSheet: Boolean = false,
    val filterApplied: Boolean = false,
    val sightings: List<Sighting> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Main map screen that displays UFO sightings on a map with filtering capabilities.
 *
 * @param viewModel ViewModel that provides data and handles business logic
 * @param onSightingClick Callback when a sighting is clicked for details
 * @param onReportSighting Callback when user wants to report a new sighting
 * @param onShowCorrelationAnalysis Callback when user wants to view correlation analysis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSightingClick: (Int) -> Unit,
    onReportSighting: () -> Unit,
    onShowCorrelationAnalysis: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State management
    var uiState by remember { mutableStateOf(MapUiState()) }
    val filterState by viewModel.filterState.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState()

    // Map-related state
    var mapView: MapView? by remember { mutableStateOf(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var markerClusterManager by remember { mutableStateOf<MarkerClusterManager?>(null) }

    // Setup marker icons
    val markerIcons: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_ufo_marker)
            ?: ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    // Collect sightings
    LaunchedEffect(Unit) {
        viewModel.sightings.collectLatest { sightings ->
            Timber.d("Received ${sightings.size} sightings")

            uiState = uiState.copy(
                sightings = sightings,
                isLoading = sightings.isEmpty() && uiState.isLoading,
                filterApplied = filterState.hasActiveFilters()
            )

            // Add timeout for loading state to avoid waiting forever
            if (uiState.isLoading) {
                delay(LOADING_TIMEOUT_MS)
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    // Collect errors
    LaunchedEffect(Unit) {
        viewModel.errorState.collectLatest { errorMessage ->
            if (errorMessage != null) {
                uiState = uiState.copy(errorMessage = errorMessage)
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Lifecycle handling for MapView
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
                        // Analysis button
                        IconButton(onClick = onShowCorrelationAnalysis) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Correlation Analysis"
                            )
                        }

                        // Add to MapScreen.kt toolbar actions
                        IconButton(
                            onClick = { viewModel.forceReloadData() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Data"
                            )
                        }

                        // Filter button
                        IconButton(onClick = {
                            uiState = uiState.copy(showFilterSheet = true)
                        }) {
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
                                Text(uiState.sightings.size.toString())
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
                if (filterState.hasActiveFilters()) {
                    ActiveFilterChips(
                        filterState = filterState,
                        onClearFilter = { viewModel.clearFilter(it) },
                        onClearAll = { viewModel.clearFilters() }
                    )
                }
            }
        },
        floatingActionButton = {
            // Action buttons
            ActionButtons(
                onAnalyzeClick = onShowCorrelationAnalysis,
                onMyLocationClick = {
                    handleMyLocationRequest(locationOverlay, mapView, context)
                },
                onReportClick = onReportSighting
            )
        }
    ) { paddingValues ->
        // Main content area (Map and overlays)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map view
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    createMapView(ctx, { newMapView ->
                        mapView = newMapView
                    }, { newLocationOverlay ->
                        locationOverlay = newLocationOverlay
                    }, { newClusterManager ->
                        markerClusterManager = newClusterManager
                    })
                },
                update = { view ->
                    updateMapWithSightings(
                        view,
                        uiState.sightings,
                        markerClusterManager,
                        markerIcons,
                        onSightingClick
                    )
                }
            )

            // Overlays - loading indicator, empty state, error message
            RenderOverlays(
                uiState = uiState,
                onDismissError = {
                    uiState = uiState.copy(errorMessage = null)
                    viewModel.clearErrorMessage()
                }
            )
        }
    }

    // Show filter bottom sheet if needed
    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            onDismiss = {
                uiState = uiState.copy(showFilterSheet = false)
            },
            onApplyFilters = { shape, state, country ->
                viewModel.updateFilters(shape = shape, state = state, country = country)
            },
            onClearFilters = { viewModel.clearFilters() },
            sheetState = bottomSheetState
        )
    }
}



/**
 * Creates and configures the MapView.
 */
private fun createMapView(
    context: Context,
    onMapViewCreated: (MapView) -> Unit,
    onLocationOverlayCreated: (MyLocationNewOverlay) -> Unit,
    onClusterManagerCreated: (MarkerClusterManager) -> Unit
): MapView {
    return MapView(context).apply {
        Timber.d("Creating and configuring MapView")

        // Configure map
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)

        // Use modern zoom controls instead of deprecated built-in ones
        setZoomButtonVisibility(false)

        // Set initial position
        controller.setZoom(INITIAL_ZOOM_LEVEL)
        controller.setCenter(USA_CENTER_GEOPOINT)

        // Set up location overlay
        val locationProvider = GpsMyLocationProvider(context)
        val myLocationOverlay = MyLocationNewOverlay(locationProvider, this).apply {
            try {
                enableMyLocation()
                Timber.d("Location overlay enabled")
            } catch (e: SecurityException) {
                Timber.w("Location permission not granted: ${e.message}")
            }
        }
        overlays.add(myLocationOverlay)
        onLocationOverlayCreated(myLocationOverlay)

        // Create marker cluster manager
        val clusterManager = MarkerClusterManager(context, this).apply {
            setAnimation(true)
            setMaxClusteringZoomLevel(MAX_ZOOM_LEVEL)
        }
        onClusterManagerCreated(clusterManager)

        // Provide the created MapView back to the caller
        onMapViewCreated(this)

        Timber.d("MapView configuration complete")
    }
}

/**
 * Loads marker icons from resources.
 */
private fun loadMarkerIcons(context: Context): Map<String, Drawable?> {
    val defaultIcon = ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
        ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)

    val userSubmittedIcon = ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
        ?: defaultIcon

    return mapOf(
        "default" to defaultIcon,
        "user_submitted" to userSubmittedIcon
    )
}

/**
 * Handles the "My Location" button click.
 */
private fun handleMyLocationRequest(
    locationOverlay: MyLocationNewOverlay?,
    mapView: MapView?,
    context: Context
) {
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
}

/**
 * Renders the floating action buttons.
 */
@Composable
private fun ActionButtons(
    onAnalyzeClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        // Correlation Analysis button
        FloatingActionButton(
            onClick = onAnalyzeClick,
            modifier = Modifier.padding(vertical = 8.dp),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Analyze Data"
            )
        }

        // My Location button
        FloatingActionButton(
            onClick = onMyLocationClick,
            modifier = Modifier.padding(vertical = 8.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                contentDescription = "My Location"
            )
        }

        // Report Sighting button
        FloatingActionButton(
            onClick = onReportClick,
            modifier = Modifier.padding(vertical = 8.dp),
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

/**
 * Renders overlay UI elements on top of the map.
 */
@Composable
private fun RenderOverlays(
    uiState: MapUiState,
    onDismissError: () -> Unit
) {
    // Show loading indicator
    if (uiState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f)
        )
    }

    // Show empty state message when no sightings and not loading
    if (!uiState.isLoading && uiState.sightings.isEmpty()) {
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

    // Show error message if any
    uiState.errorMessage?.let { errorMessage ->
        Snackbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            action = {
                TextButton(onClick = onDismissError) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(errorMessage)
        }
    }
}

/**
 * Updates the map with sighting markers.
 * This function clears existing markers and adds new ones based on the provided sightings.
 */
private fun updateMapWithSightings(
    view: MapView,
    sightings: List<Sighting>,
    markerClusterManager: MarkerClusterManager?,
    markerIcons: Map<String, Drawable?>,
    onSightingClick: (Int) -> Unit
) {
    // Early exit if no data or cluster manager
    if (sightings.isEmpty() || markerClusterManager == null) {
        Timber.w("Cannot update markers: sightings empty=${sightings.isEmpty()}, clusterManager=${markerClusterManager != null}")
        return
    }

    Timber.d("Updating map with ${sightings.size} sightings")

    // Clear existing markers
    markerClusterManager.clearItems()

    // Track valid markers for statistics
    var validMarkersCount = 0

    // Track bounds for zooming
    val boundingBox = BoundingBox()

    // Add markers for all valid sightings
    sightings.forEach { sighting ->
        // Skip invalid coordinates
        if (!isValidCoordinate(sighting.latitude, sighting.longitude)) {
            Timber.w("Invalid coordinates for sighting ${sighting.id}: ${sighting.latitude},${sighting.longitude}")
            return@forEach
        }

        // Select appropriate icon
        val markerIcon = if (sighting.isUserSubmitted) {
            markerIcons["user_submitted"]
        } else {
            markerIcons["default"]
        }

        try {
            // Create the marker
            val marker = createSightingMarker(
                view,
                sighting,
                markerIcon,
                onSightingClick
            )

            // Add to cluster manager
            markerClusterManager.addItem(marker)
            validMarkersCount++

            // Add to bounding box
            boundingBox.include(GeoPoint(sighting.latitude, sighting.longitude))
        } catch (e: Exception) {
            Timber.e(e, "Error creating marker for sighting ${sighting.id}")
        }
    }

    // Log statistics
    Timber.d("Added $validMarkersCount valid markers out of ${sightings.size} sightings")

    // Refresh map with new markers
    markerClusterManager.invalidate()
    view.invalidate()

    // Zoom to show all markers if we have any
    if (validMarkersCount > 0 && !boundingBox.isNull()) {
        try {
            // Ensure the bounding box has minimum dimensions
            ensureMinimumBoundingBoxSize(boundingBox)

            // Animate to the bounding box
            view.zoomToBoundingBox(boundingBox, true, 50, MAX_ZOOM_LEVEL, 1000L)
            Timber.d("Zoomed to show all markers")
        } catch (e: Exception) {
            Timber.e(e, "Error zooming to markers: ${e.message}")
        }
    }
}

/**
 * Creates a marker for a sighting with all appropriate settings.
 */
private fun createSightingMarker(
    view: MapView,
    sighting: Sighting,
    icon: Drawable?,
    onSightingClick: (Int) -> Unit
): Marker {
    return Marker(view).apply {
        id = sighting.id.toString()
        position = GeoPoint(sighting.latitude, sighting.longitude)
        title = sighting.city ?: "Sighting ${sighting.id}"
        snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
        this.icon = icon
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        relatedObject = sighting
        infoWindow = SightingInfoWindow(view, onSightingClick)
        setOnMarkerClickListener { marker, mapView ->
            InfoWindow.closeAllInfoWindowsOn(mapView)
            marker.showInfoWindow()
            true
        }
    }
}

/**
 * Checks if coordinates are valid for display on a map.
 */
private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    // Only perform basic range checks - don't filter out 0,0 coordinates
    // since they might be valid in some cases
    return latitude.isFinite() && longitude.isFinite() &&
            latitude >= -90.0 && latitude <= 90.0 &&
            longitude >= -180.0 && longitude <= 180.0
}

/**
 * Ensures a bounding box has minimum dimensions to avoid zoom errors.
 */
private fun ensureMinimumBoundingBoxSize(boundingBox: BoundingBox) {
    // Get current dimensions
    val latSpan = boundingBox.latitudeSpan
    val lonSpan = boundingBox.longitudeSpan

    // Apply minimum size if needed
    if (latSpan < MIN_BOUNDING_BOX_SPAN_DEGREES) {
        val midLat = boundingBox.centerLatitude
        boundingBox.latNorth = midLat + MIN_BOUNDING_BOX_SPAN_DEGREES / 2
        boundingBox.latSouth = midLat - MIN_BOUNDING_BOX_SPAN_DEGREES / 2
    }

    if (lonSpan < MIN_BOUNDING_BOX_SPAN_DEGREES) {
        val midLon = boundingBox.centerLongitude
        boundingBox.lonEast = midLon + MIN_BOUNDING_BOX_SPAN_DEGREES / 2
        boundingBox.lonWest = midLon - MIN_BOUNDING_BOX_SPAN_DEGREES / 2
    }
}