/// Update the MapScreen.kt file with these changes

// Import statements to add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import com.ufomap.ufosightingmap.R
import com.ufomap.ufosightingmap.data.FilterState

// In the MapScreen function, add these modifications:

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSightingClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect states from ViewModel
    val sightings: List<Sighting> by viewModel.sightings.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    val isLoading = sightings.isEmpty() && viewModel.sightings.value.isEmpty()
    var mapView: MapView? by remember { mutableStateOf(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    var locationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }

    // Lifecycle handling (keep existing code)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map view (keep existing implementation)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        // Keep existing map setup code
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
                    // Keep existing marker update code
                    Log.d("MapScreen", "Updating MapView with ${sightings.size} sightings.")

                    val overlaysToRemove = view.overlays.filterIsInstance<Marker>()
                        .filter { it.id != null }
                    view.overlays.removeAll(overlaysToRemove)

                    if (sightings.isNotEmpty() && defaultMarkerIcon != null) {
                        Log.d("MapScreen", "Creating ${sightings.size} markers")
                        try {
                            sightings.forEach { sighting ->
                                val marker = Marker(view).apply {
                                    id = sighting.id.toString()
                                    position = GeoPoint(sighting.latitude, sighting.longitude)
                                    title = sighting.city ?: "Sighting ${sighting.id}"
                                    snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
                                    icon = defaultMarkerIcon
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                    relatedObject = sighting
                                    infoWindow = SightingInfoWindow(view, onSightingClick)

                                    setOnMarkerClickListener { marker, mapView ->
                                        InfoWindow.closeAllInfoWindowsOn(mapView)
                                        marker.showInfoWindow()
                                        true
                                    }
                                }
                                view.overlays.add(marker)
                            }
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error creating markers", e)
                        }
                    }
                    view.invalidate()
                }
            )

            // Location button (keep existing implementation)
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
                    .padding(16.dp),
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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