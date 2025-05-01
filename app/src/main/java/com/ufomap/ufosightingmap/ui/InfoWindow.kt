package com.ufomap.ufosightingmap.ui

import android.widget.TextView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.ufomap.ufosightingmap.R

/**
 * Basic InfoWindow implementation for showing marker information
 *
 * Note: This is a simplified info window. For sighting-specific
 * details with additional fields, use SightingInfoWindow class.
 */
class CustomInfoWindow(
    private val mapView: MapView
) : InfoWindow(R.layout.custom_info_window, mapView) {

    override fun onOpen(item: Any?) {
        if (item !is Marker) return

        // Find views in the layout
        val title = mView.findViewById<TextView>(R.id.title)
        val description = mView.findViewById<TextView>(R.id.description)

        // Set content from marker properties
        title.text = item.title ?: "Unknown Location"
        description.text = item.snippet ?: "No description available"
    }

    override fun onClose() {
        // Clean up resources if needed
    }

    companion object {
        /**
         * Helper method to show a custom info window for a marker
         */
        fun show(marker: Marker, mapView: MapView) {
            // Close any open info windows first
            InfoWindow.closeAllInfoWindowsOn(mapView)

            // Create and set the info window if needed
            if (marker.infoWindow !is CustomInfoWindow) {
                marker.infoWindow = CustomInfoWindow(mapView)
            }

            // Show the info window
            marker.showInfoWindow()
        }
    }
}