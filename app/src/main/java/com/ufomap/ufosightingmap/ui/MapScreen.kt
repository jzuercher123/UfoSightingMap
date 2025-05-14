package com.ufomap.ufosightingmap.ui

import org.osmdroid.views.CustomZoomButtonsController.Visibility
import timber.log.Timber
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.ufomap.ufosightingmap.utils.MarkerClusterManager
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import kotlinx.coroutines.flow.collectLatest
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
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
    val snackbarHostState = remember { SnackbarHostState() }

    var mapView: MapView? by remember { mutableStateOf(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var markerClusterManager by remember { mutableStateOf<MarkerClusterManager?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    val filterState by viewModel.filterState.collectAsState()
    val isLoadingFromViewModel by viewModel.isLoading.collectAsState()

    val defaultMarkerIcon: Drawable? = remember(context) {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    val userSubmittedMarkerIcon: Drawable? = remember(context, defaultMarkerIcon) {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
            ?: defaultMarkerIcon
    }

    LaunchedEffect(isLoadingFromViewModel, viewModel.sightings) {
        viewModel.sightings.collectLatest { sightings ->
            Timber.tag(TAG).d("Sightings loaded: ${sightings.size}")
            uiState = uiState.copy(
                sightings = sightings,
                isLoading = isLoadingFromViewModel
            )

            if (!isLoadingFromViewModel && uiState.isLoading) {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorState.collectLatest { errorMessage ->
            if (errorMessage != null) {
                Timber.tag(TAG).e("Error received: $errorMessage")
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    actionLabel = "Dismiss"
                )
                viewModel.clearErrorMessage()
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
                    mapView?.onDetach()
                    mapView = null
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
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                mapView?.onPause()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("UFO Sighting Map") },
                    actions = {
                        IconButton(onClick = {
                            Timber.tag(TAG).d("Refresh button clicked")
                            viewModel.refreshData()
                        }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh Data"
                            )
                        }
                        IconButton(onClick = onShowCorrelationAnalysis) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Correlation Analysis"
                            )
                        }
                        Box {
                            IconButton(onClick = {
                                uiState = uiState.copy(showFilterSheet = true)
                            }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter"
                                )
                            }
                            if (filterState.hasActiveFilters()) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                                ) {
                                    Text(uiState.sightings.size.toString())
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

                        // Set user agent at the Configuration level
                        Configuration.getInstance().userAgentValue = ctx.packageName

                        // Use standard tile source
                        setTileSource(TileSourceFactory.MAPNIK)

                        // Enable built-in zoom controls
                        setMultiTouchControls(true)
                        zoomController.setVisibility(Visibility.SHOW_AND_FADEOUT)

                        // Initial zoom and center
                        controller.setZoom(INITIAL_ZOOM_LEVEL)
                        controller.setCenter(USA_CENTER_GEOPOINT)

                        // Setup location overlay
                        val locProvider = GpsMyLocationProvider(ctx)
                        locProvider.locationUpdateMinTime = 10000 // 10 seconds
                        locProvider.locationUpdateMinDistance = 10f // 10 meters

                        val newLocationOverlay = MyLocationNewOverlay(locProvider, this)
                        try {
                            newLocationOverlay.enableMyLocation()
                            Log.d(TAG, "Location overlay enabled")
                        } catch (e: SecurityException) {
                            Timber.tag(TAG).w(e, "Location permission not granted for MyLocationOverlay.")
                        }
                        overlays.add(newLocationOverlay)
                        locationOverlay = newLocationOverlay

                        // FIXED: Create the cluster manager with better configuration
                        val manager = MarkerClusterManager(ctx, this).apply {
                            setAnimation(true)
                            setMaxClusteringZoomLevel(15.0) // Adjust based on your preference
                            setClusterDistance(100) // Adjust based on your preference
                        }

                        // IMPORTANT: Add the cluster manager's overlay to the map
                        // This ensures the cluster overlay is rendered
                        overlays.add(manager.getClusterOverlay())
                        markerClusterManager = manager

                        // ADDED: Add zoom listener to handle cluster updates
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                Log.d(TAG, "Zoom changed to: ${event?.zoomLevel}")
                                // Invalidate clusters when zoom changes
                                markerClusterManager?.invalidate()
                                return false
                            }
                        })

                        mapView = this
                        Timber.tag(TAG).d("MapView created and initialized in factory.")
                    }
                },
                update = { view ->
                    Timber.tag(TAG).d("MapView update called with ${uiState.sightings.size} sightings.")
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
                        .align(Alignment.Center)
                        .zIndex(1f)
                )
            }

            if (!uiState.isLoading && uiState.sightings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sightings found. Try adjusting filters or refreshing.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
                uiState = uiState.copy(showFilterSheet = false)
            },
            onClearFilters = {
                viewModel.clearFilters()
            },
            sheetState = bottomSheetState
        )
    }
}

private fun updateMapWithSightings(
    view: MapView,
    uiState: MapUiState,
    markerClusterMgr: MarkerClusterManager?,
    defaultIcon: Drawable?,
    userSubmittedIcon: Drawable?,
    onSightingClick: (Int) -> Unit
) {
    val clusterManager = markerClusterMgr ?: run {
        Timber.tag(TAG).w("MarkerClusterManager is null in updateMapWithSightings. Skipping update.")
        return
    }

    Timber.tag(TAG).d("Updating map with ${uiState.sightings.size} sightings")

    // Clear existing markers and clusters
    clusterManager.clearItems()

    // Create new markers
    var validMarkersCount = 0
    uiState.sightings.forEach { sighting ->
        if (sighting.latitude.isFinite() && sighting.longitude.isFinite() &&
            sighting.latitude >= -90.0 && sighting.latitude <= 90.0 &&
            sighting.longitude >= -180.0 && sighting.longitude <= 180.0) {

            val marker = Marker(view).apply {
                id = sighting.id.toString()
                position = GeoPoint(sighting.latitude, sighting.longitude)
                title = sighting.city ?: "Sighting #${sighting.id}"
                snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
                icon = if (sighting.isUserSubmitted) userSubmittedIcon ?: defaultIcon else defaultIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                relatedObject = sighting

                infoWindow = SightingInfoWindow(view, onSightingClick)
                setOnMarkerClickListener { m, mv ->
                    InfoWindow.closeAllInfoWindowsOn(mv)
                    if (m.isInfoWindowShown) {
                        m.closeInfoWindow()
                    } else {
                        m.showInfoWindow()
                    }
                    mv.controller.animateTo(m.position)
                    true
                }
            }
            clusterManager.addItem(marker)
            validMarkersCount++
        } else {
            Timber.tag(TAG).w("Skipping sighting with invalid coordinates: ID ${sighting.id} at ${sighting.latitude}, ${sighting.longitude}")
        }
    }

    Timber.tag(TAG).d("Added $validMarkersCount valid markers to cluster manager")

    // Force cluster update
    clusterManager.invalidate()

    // If we have valid sightings, zoom to fit them all
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

        if (pointsIncluded > 0 && (boundingBox.latitudeSpan > 1E-6 || boundingBox.longitudeSpanWithDateLine > 1E-6)) {
            try {
                view.zoomToBoundingBox(boundingBox, true, 50)
                Timber.tag(TAG).d("Zoomed to bounding box of $pointsIncluded sightings.")
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).e(e, "Error zooming to bounding box. Box: N:${boundingBox.latNorth} S:${boundingBox.latSouth} E:${boundingBox.lonEast} W:${boundingBox.lonWest}")
            }
        } else if (pointsIncluded == 1) {
            view.controller.animateTo(GeoPoint(uiState.sightings.first().latitude, uiState.sightings.first().longitude))
            view.controller.setZoom(10.0)
        }
    }
}

// Helper extension method for BoundingBox
private fun BoundingBox.include(point: GeoPoint) {
    val north = maxOf(this.latNorth, point.latitude)
    val south = minOf(this.latSouth, point.latitude)
    val east = maxOf(this.lonEast, point.longitude)
    val west = minOf(this.lonWest, point.longitude)

    // Set values for this bounding box
    this.latNorth = north
    this.latSouth = south
    this.lonEast = east
    this.lonWest = west
}