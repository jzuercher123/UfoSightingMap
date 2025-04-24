package com.ufomap.ufosightingmap.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ufomap.ufosightingmap.R
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Default map location
val USA_CENTER_GEOPOINT = GeoPoint(39.8283, -98.5795)
const val INITIAL_ZOOM_LEVEL = 4.0

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSightingClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Correctly collect state from ViewModel's StateFlow
    val sightings: List<Sighting> by viewModel.sightings.collectAsState()
    // Correct check for loading state
    val isLoading = sightings.isEmpty() && viewModel.sightings.value.isEmpty()

    var mapView: MapView? by remember { mutableStateOf(null) }

    val defaultMarkerIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_marker_default)
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

                                    // Store the sighting data object with the marker
                                    relatedObject = sighting

                                    // Use the SightingInfoWindow with navigation callback
                                    infoWindow = SightingInfoWindow(view, onSightingClick)

                                    setOnMarkerClickListener { marker, mapView ->
                                        InfoWindow.closeAllInfoWindowsOn(mapView)
                                        marker.showInfoWindow()

                                        // We could directly navigate here, but we're choosing to show
                                        // the info window first and let the user click for details
                                        /*
                                        (marker.relatedObject as? Sighting)?.id?.let { sightingId ->
                                            onSightingClick(sightingId)
                                        }
                                        */
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

            // User location centering button
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
                // Using a drawable resource for the location icon
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                    contentDescription = "My Location"
                )
            }

            // Show loading indicator if needed
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}