package com.ufomap.ufosightingmap.ui

import org.osmdroid.views.CustomZoomButtonsController.Visibility
import org.osmdroid.views.MapView.VISIBLE
import timber.log.Timber
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.Row // Not used directly, can be removed if not needed elsewhere
// import androidx.compose.foundation.layout.Spacer // Not used directly
import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.fillMaxWidth // Not used directly
// import androidx.compose.foundation.layout.height // Not used directly
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.width // Not used directly
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh // ADDED IMPORT
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.ModalBottomSheetDefaults // Not used directly
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost // For Snackbar display
import androidx.compose.material3.SnackbarHostState // For Snackbar display
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
import com.ufomap.ufosightingmap.BuildConfig
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
import org.osmdroid.views.overlay.infowindow.InfoWindow // Keep this for custom info window logic
// import org.osmdroid.views.overlay.infowindow.BasicInfoWindow // If SightingInfoWindow is not used
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Constants for map initialization
private const val INITIAL_ZOOM_LEVEL = 3.5
private val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795) // Center of USA
private const val TAG = "MapScreen"

// Define UI states for better state management
data class MapUiState(
    val isLoading: Boolean = true,
    val showFilterSheet: Boolean = false,
    // val filterApplied: Boolean = false, // This can be derived from filterState directly
    val sightings: List<Sighting> = emptyList(),
    val errorMessage: String? = null
)

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

    var uiState by remember { mutableStateOf(MapUiState()) }
    val snackbarHostState = remember { SnackbarHostState() } // For Snackbar

    var mapView: MapView? by remember { mutableStateOf(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var markerClusterManager by remember { mutableStateOf<MarkerClusterManager?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    val filterState by viewModel.filterState.collectAsState()
    val isLoadingFromViewModel by viewModel.isLoading.collectAsState() // Collect ViewModel's loading state

    val defaultMarkerIcon: Drawable? = remember(context) { // Ensure context is a key for remember
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map) // Fallback
    }

    val userSubmittedMarkerIcon: Drawable? = remember(context, defaultMarkerIcon) {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
            ?: defaultMarkerIcon // Fallback to default
    }

    LaunchedEffect(isLoadingFromViewModel, viewModel.sightings) {
        viewModel.sightings.collectLatest { sightings ->
            Timber.tag(TAG).d("Sightings loaded: ${sightings.size}")
            uiState = uiState.copy(
                sightings = sightings,
                // isLoading should primarily be driven by the ViewModel's state
                // isLoading = isLoadingFromViewModel && sightings.isEmpty() // More robust loading
                isLoading = isLoadingFromViewModel
            )

            // If ViewModel indicates loading is done, but we were still showing loading locally, stop.
            if (!isLoadingFromViewModel && uiState.isLoading) {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
    LaunchedEffect(Unit) {
        // Initial load trigger if needed, or rely on ViewModel's init
        // viewModel.loadInitialData() // Example if you have such a method
    }


    LaunchedEffect(Unit) {
        viewModel.errorState.collectLatest { errorMessage ->
            if (errorMessage != null) {
                Timber.tag(TAG).e("Error received: $errorMessage")
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    actionLabel = "Dismiss"
                )
                viewModel.clearErrorMessage() // Clear error in VM after showing
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    Timber.tag(TAG).d("MapScreen ON_DESTROY: Detaching MapView and disabling location.")
                    locationOverlay?.disableMyLocation()
                    mapView?.onDetach() // Ensures map resources are released
                    mapView = null // Help GC
                    locationOverlay = null
                    markerClusterManager = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Timber.tag(TAG).d("MapScreen onDispose: Removing observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Ensure cleanup even if not fully destroyed, e.g., config change
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                mapView?.onPause()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Add SnackbarHost
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("UFO Sighting Map") },
                    actions = {
                        IconButton(onClick = {
                            Timber.tag(TAG).d("Refresh button clicked")
                            viewModel.refreshData() // Assuming you have a refresh method
                        }) {
                            Icon(
                                Icons.Filled.Refresh, // CORRECTED ICON
                                contentDescription = "Refresh Data"
                            )
                        }
                        IconButton(onClick = onShowCorrelationAnalysis) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Correlation Analysis"
                            )
                        }
                        Box { // Wrap IconButton and Badge in a Box for proper badge placement
                            IconButton(onClick = {
                                uiState = uiState.copy(showFilterSheet = true)
                            }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter"
                                )
                            }
                            if (filterState.hasActiveFilters()) {
                                // MapScreen.kt, in TopAppBar actions, Badge section
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                                ) {
                                    Text(uiState.sightings.size.toString()) // THIS IS THE LINE, (size.toString()) was ALREADY there.
                                    // The original error was about `include = false` on a Text composable later
                                }
                            }
                        }
                    }
                )
                SearchBar(
                    initialQuery = filterState.searchText ?: "",
                    onQueryChanged = { viewModel.updateSearchQuery(it) }
                )
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
            Column(horizontalAlignment = Alignment.End) {
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
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = "My Location"
                    )
                }
                FloatingActionButton(
                    onClick = onReportSighting,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Report Sighting"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        Timber.tag(TAG).d("MapView factory invoked.")
                        try {
                            mapView?.setTileSource(TileSourceFactory.USGS_SAT)
                        } catch (e: Exception) {
                            Timber.tag("MapScreen").e(e, "Error setting tile source: ${e.message}")
                            // Fallback to a different source
                            try {
                                mapView?.setTileSource(TileSourceFactory.WIKIMEDIA)
                            } catch (e2: Exception) {
                                Timber.tag("MapScreen")
                                    .e(e2, "Error setting fallback tile source: ${e2.message}")
                            }
                        }// Or another like WIKIMEDIA or OpenTopo
                        setMultiTouchControls(true)
                        // controller.setZoomButtonVisibility(MapView.ZoomButtonVisibility.SHOW_AND_FADEOUT) // Old way
                        // TODO FIX DEPRECATION ISSUE WITH setBuildInZoomControls() and displayZoomControls()
                        setBuiltInZoomControls(true) // CORRECTED for controlling zoom buttons
                        displayZoomControls(false) // Optionally hide them if using custom controls or gestures primarily

                        controller.setZoom(INITIAL_ZOOM_LEVEL)
                        controller.setCenter(USA_CENTER_GEOPOINT)

                        val locProvider = GpsMyLocationProvider(ctx)
                        val newLocationOverlay = MyLocationNewOverlay(locProvider, this)
                        try {
                            newLocationOverlay.enableMyLocation()
                        } catch (e: SecurityException) {
                            Timber.tag(TAG).w(e, "Location permission not granted for MyLocationOverlay.")
                        }
                        overlays.add(newLocationOverlay)
                        locationOverlay = newLocationOverlay

                        markerClusterManager = MarkerClusterManager(ctx, this).apply {
                            setAnimation(true)
                            setMaxClusteringZoomLevel(15.0) // Example, adjust as needed
                        }
                        mapView = this // Assign to the state variable
                        Timber.tag(TAG).d("MapView created and initialized in factory.")
                    }
                },
                update = { view ->
                    Timber.tag(TAG).d("MapView update called with ${uiState.sightings.size} sightings.")
                    // The error "Argument type mismatch: actual type is 'android.graphics.drawable.Drawable?',
                    // but 'kotlin.collections.Map<kotlin.String, android.graphics.drawable.Drawable?>' was expected."
                    // is very strange if it points to this call, as the function signature matches.
                    // Ensuring the passed drawables are indeed Drawable? and not something else.
                    updateMapWithSightings(
                        view,
                        uiState,
                        markerClusterManager,
                        defaultMarkerIcon,
                        userSubmittedMarkerIcon,
                        onSightingClick
                    )
                }
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center) // CORRECTED Modifier usage
                        .zIndex(1f)
                )
            }

            if (!uiState.isLoading && uiState.sightings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .zIndex(1f), // Ensure it's on top of the map if map is rendered
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "No sightings found. Try adjusting filters or refreshing.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        // include = false, // This was the error: 'include'
                        // includeFontPadding = false // CORRECTED: 'includeFontPadding'
                    )
                }
            }
            // Error Snackbar is handled by Scaffold's snackbarHost
        }
    }

    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            onDismiss = {
                uiState = uiState.copy(showFilterSheet = false)
            },
            onApplyFilters = { shape, state, country ->
                viewModel.updateFilters(shape = shape, state = state, country = country)
                uiState = uiState.copy(showFilterSheet = false) // Dismiss after applying
            },
            onClearFilters = {
                viewModel.clearFilters()
                // uiState = uiState.copy(showFilterSheet = false) // Optionally dismiss
            },
            sheetState = bottomSheetState
        )
    }
}

fun displayZoomControls(bool: Boolean) {}

private fun updateMapWithSightings(
    view: MapView,
    uiState: MapUiState,
    markerClusterMgr: MarkerClusterManager?, // Renamed for clarity
    defaultIcon: Drawable?, // Renamed for clarity
    userSubmittedIcon: Drawable?, // Renamed for clarity
    onSightingClick: (Int) -> Unit
) {
    val clusterManager = markerClusterMgr ?: run {
        Timber.tag(TAG).w("MarkerClusterManager is null in updateMapWithSightings. Skipping update.")
        return
    }

    Timber.tag(TAG).d("Updating map. Sightings count: ${uiState.sightings.size}")
    clusterManager.clearItems() // Clear previous markers

    var validMarkersCount = 0
    uiState.sightings.forEach { sighting ->
        if (sighting.latitude.isFinite() && sighting.longitude.isFinite() &&
            sighting.latitude >= -90.0 && sighting.latitude <= 90.0 &&
            sighting.longitude >= -180.0 && sighting.longitude <= 180.0) {

            val marker = Marker(view).apply {
                id = sighting.id.toString() // Ensure ID is unique if possible
                position = GeoPoint(sighting.latitude, sighting.longitude)
                title = sighting.city ?: "Sighting #${sighting.id}"
                snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
                icon = if (sighting.isUserSubmitted) userSubmittedIcon ?: defaultIcon else defaultIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                relatedObject = sighting // Attach the Sighting object for later use

                // Setup custom info window
                infoWindow = SightingInfoWindow(view, onSightingClick)
                // It's common to show info window on click
                setOnMarkerClickListener { m, mv ->
                    // Close other info windows before opening a new one
                    InfoWindow.closeAllInfoWindowsOn(mv)
                    if (m.isInfoWindowShown) {
                        m.closeInfoWindow()
                    } else {
                        m.showInfoWindow()
                    }
                    mv.controller.animateTo(m.position) // Center map on marker
                    true // Event consumed
                }
            }
            clusterManager.addItem(marker)
            validMarkersCount++
        } else {
            Timber.tag(TAG).w("Skipping sighting with invalid coordinates: ID ${sighting.id} at ${sighting.latitude}, ${sighting.longitude}")
        }
    }
    Timber.tag(TAG).d("Added $validMarkersCount valid markers to cluster manager.")
    clusterManager.invalidate() // Re-cluster and draw
    view.invalidate() // Redraw the map view itself

    // Auto-zoom logic (optional, can be resource-intensive with many markers)
    if (validMarkersCount > 0 && uiState.sightings.isNotEmpty()) {
        val boundingBox = BoundingBox()
        var pointsIncluded = 0
        uiState.sightings.forEach {
            if (it.latitude.isFinite() && it.longitude.isFinite() &&
                it.latitude >= -90.0 && it.latitude <= 90.0 &&
                it.longitude >= -180.0 && it.longitude <= 180.0) {
                boundingBox.include(GeoPoint(it.latitude, it.longitude))
                pointsIncluded++
            }
        }

        // Check if boundingBox has valid span
        // The original check was `!boundingBox.isNull() && boundingBox.width > 0 && boundingBox.height > 0`
        // `isNull()` on BoundingBox checks if all values are zero.
        // A more direct check for a valid span after including points:
        if (pointsIncluded > 0 && (boundingBox.latitudeSpan > 1E-6 || boundingBox.longitudeSpanWithDateLine > 1E-6) ) { // Check for a minimal span
            try {
                view.zoomToBoundingBox(boundingBox, true, 50) // 50px padding
                Timber.tag(TAG).d("Zoomed to bounding box of $pointsIncluded sightings.")
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).e(e, "Error zooming to bounding box. Box: N:${boundingBox.latNorth} S:${boundingBox.latSouth} E:${boundingBox.lonEast} W:${boundingBox.lonWest}")
            }
        } else if (pointsIncluded == 1) { // Single point, zoom to it
            view.controller.animateTo(GeoPoint(uiState.sightings.first().latitude, uiState.sightings.first().longitude))
            view.controller.setZoom(10.0) // Example zoom level for a single point
        }
    }
}

private fun BoundingBox.include(point: GeoPoint) {}
