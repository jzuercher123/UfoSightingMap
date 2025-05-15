package com.ufomap.ufosightingmap.ui

import android.R.id.closeButton
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.ufomap.ufosightingmap.R
import com.ufomap.ufosightingmap.data.Sighting

/**
 * Custom InfoWindow implementation for displaying UFO sighting information
 * when a marker is tapped on the map.
 */
class SightingInfoWindow(
    private val mapView: MapView,
    private val onSightingClick: ((Int) -> Unit)? = null
) : InfoWindow(R.layout.sighting_info_window, mapView) {

    override fun onClose() {
        // Clean up resources if needed
    }

    override fun onOpen(item: Any?) {
        if (item !is Marker) return

        // Get the sighting data associated with this marker
        val sighting = item.relatedObject as? Sighting
        

        // Find all views in the layout
        val titleTextView = mView.findViewById<TextView>(R.id.title)
        val dateTextView = mView.findViewById<TextView>(R.id.date)
        val shapeTextView = mView.findViewById<TextView>(R.id.shape)
        val descriptionTextView = mView.findViewById<TextView>(R.id.description)
        val detailsButton = mView.findViewById<Button>(R.id.details_button)

        // Set the text values
        titleTextView.text = item.title ?: "Unknown Location"
        dateTextView.text = sighting?.dateTime ?: "Unknown date"
        shapeTextView.text = "Shape: ${sighting?.shape ?: "Unknown"}"
        descriptionTextView.text = sighting?.summary ?: "No details available"

        // Configure the details button with navigation callback
        detailsButton.setOnClickListener {
            sighting?.id?.let { sightingId ->
                // Use the onSightingClick callback to navigate to the detail screen
                onSightingClick?.invoke(sightingId)
            } ?: run {
                // Show a toast if for some reason we don't have a valid sighting ID
                Toast.makeText(
                    mapView.context,
                    "Could not find details for this sighting",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Close this info window
            close()
        }

        // Add this: Configure the close button
        closeButton.setOnClickListener {
            // Close this info window
            close()
        }
    }

    annotation class ImageButton

    companion object {
        /**
         * Helper method to create and show an info window for a marker
         */
        fun show(
            marker: Marker,
            mapView: MapView,
            onSightingClick: ((Int) -> Unit)? = null
        ) {
            // Close any open info windows first
            InfoWindow.closeAllInfoWindowsOn(mapView)

            // Create and set the info window if needed
            if (marker.infoWindow !is SightingInfoWindow) {
                marker.infoWindow = SightingInfoWindow(mapView, onSightingClick)
            }

            // Show the info window
            marker.showInfoWindow()
        }
    }
}

private fun Int.setOnClickListener(function: () -> kotlin.Unit) {}
