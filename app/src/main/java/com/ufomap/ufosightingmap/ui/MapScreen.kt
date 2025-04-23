package com.ufomap.ufosightingmap.ui // Corrected package name

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.TextView // Import standard Android TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Import Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.* // Import core runtime functions like remember, collectAsState etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ufomap.ufosightingmap.R // Correct import for project resources
import com.ufomap.ufosightingmap.data.Sighting // Correct import
import com.ufomap.ufosightingmap.viewmodel.MapViewModel // Correct import
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow // Import osmdroid InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Default map location
val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795)
const val INITIAL_ZOOM_LEVEL = 4.0

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel) { // Correct ViewModel type
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Correctly collect state from ViewModel's StateFlow
    val sightings: List<Sighting> by viewModel.sightings.collectAsState()
    // Correct check for loading state
    val isLoading = sightings.isEmpty() && viewModel.sightings.value.isEmpty() // Check initial value too

    var mapView: MapView? by remember { mutableStateOf(null) }

    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default) // Use correct R import
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
    }

    var locationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }

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
            TopAppBar(title = { Text("UFO Sighting Map (OSM)") })
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
                    Log.d("MapScreen", "Updating MapView with ${sightings.size} sightings.") // Use size property

                    val overlaysToRemove = view.overlays.filterIsInstance<Marker>()
                        .filter { it.id != null }
                    view.overlays.removeAll(overlaysToRemove)

                    if (sightings.isNotEmpty() && defaultMarkerIcon != null) { // Check sightings list is not empty
                        sightings.forEach { sighting -> // Iterate over the list
                            val marker = Marker(view).apply {
                                id = sighting.id.toString() // Access properties correctly
                                position = GeoPoint(sighting.latitude, sighting.longitude)
                                title = sighting.city ?: "Sighting ${sighting.id}"
                                snippet = "Shape: ${sighting.shape ?: "Unknown"}\n${sighting.summary ?: ""}"
                                icon = defaultMarkerIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                // --- Simplified InfoWindow Handling ---
                                // We removed the dependency that provided R.layout.bonuspack_bubble
                                // For now, just log the click. Showing details would require
                                // either creating a custom InfoWindow class + layout, or
                                // handling the click to show info in a Compose element (e.g., BottomSheet).
                                setOnMarkerClickListener { marker, mapView ->
                                    Log.i("MarkerClick", "Clicked: ${marker.title} - ${marker.snippet}")
                                    // Close others if open
                                    InfoWindow.closeAllInfoWindowsOn(mapView)
                                    // You could potentially show a simple default text info window
                                    // but it requires more setup or a custom class.
                                    // For now, just logging.
                                    // marker.showInfoWindow() // This would show a basic window if configured
                                    true // Indicate click was handled
                                }
                                // --- End Simplified InfoWindow Handling ---
                            }
                            view.overlays.add(marker)
                        }
                    }
                    view.invalidate()
                }
            )

            // Correct check for loading state
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } // End Box
    } // End Scaffold
}
