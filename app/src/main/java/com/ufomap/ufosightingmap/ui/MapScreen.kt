package com.ufomap.ufosightingmap.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState

// Constants for map initialization
private const val INITIAL_ZOOM_LEVEL = 4.5
private val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795) // Center of USA
private const val TAG = "MapScreen"

// Define UI states for better state management
data class MapUiState(
    val isLoading: Boolean = true,
    val showFilterSheet: Boolean = false,
    val filterApplied: Boolean = false,
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

    // Use a single state object for better state management
    var uiState by remember { mutableStateOf(MapUiState()) }

    // Keep MapView reference separate since it's an Android View
    var mapView: MapView? by remember { mutableStateOf(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var markerClusterManager by remember { mutableStateOf<MarkerClusterManager?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    // Collect filter state
    val filterState by viewModel.filterState.collectAsState()

    // Setup marker icons
    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    val userSubmittedMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_user_submitted)
            ?: defaultMarkerIcon
    }

    // Collect sightings using LaunchedEffect to avoid doing it during composition
    LaunchedEffect(Unit) {
        viewModel.sightings.collectLatest { sightings ->
            uiState = uiState.copy(
                sightings = sightings,
                isLoading = sightings.isEmpty() && uiState.isLoading,
                filterApplied = filterState.hasActiveFilters()
            )

            // Add timeout for loading state
            if (uiState.isLoading) {
                delay(5000)
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
            // Column to stack multiple FABs
            Column(horizontalAlignment = Alignment.End) {
                // Correlation Analysis button
                FloatingActionButton(
                    onClick = onShowCorrelationAnalysis,
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
                    onClick = onReportSighting,
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
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Creating MapView")
                        }
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        // Use property instead of deprecated method
                        setBuiltInZoomControls(true)
                        controller.setZoom(INITIAL_ZOOM_LEVEL)
                        controller.setCenter(USA_CENTER_GEOPOINT)
                        mapView = this

                        // Setup location overlay
                        val locProvider = GpsMyLocationProvider(ctx)
                        val overlay = MyLocationNewOverlay(locProvider, this)
                        try {
                            overlay.enableMyLocation()
                        } catch (exception: SecurityException) {
                            Log.w(TAG, "Location permission not granted for MyLocationOverlay: ${exception.message}")
                        }
                        this.overlays.add(overlay)
                        locationOverlay = overlay

                        // Create marker cluster manager
                        markerClusterManager = MarkerClusterManager(ctx, this).apply {
                            setAnimation(true)
                            setMaxClusteringZoomLevel(15.0)
                        }

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "MapView created and initialized.")
                        }
                    }
                },
                update = { view ->
                    // Only update markers if we have a valid map and new sightings data
                    if (uiState.sightings.isNotEmpty() && markerClusterManager != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Updating map with ${uiState.sightings.size} sightings")
                        }

                        // Clear existing markers
                        markerClusterManager?.clearItems()

                        // Add new markers through cluster manager
                        uiState.sightings.forEach { sighting ->
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
                                setOnMarkerClickListener { m: Marker, mv: MapView ->
                                    InfoWindow.closeAllInfoWindowsOn(mv)
                                    m.showInfoWindow()
                                    true
                                }
                            }
                            markerClusterManager?.addItem(marker)
                        }

                        // Refresh map with new markers
                        markerClusterManager?.invalidate()
                        view.invalidate()
                    }
                }
            )

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
                        TextButton(onClick = {
                            uiState = uiState.copy(errorMessage = null)
                            viewModel.clearErrorMessage()
                        }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
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