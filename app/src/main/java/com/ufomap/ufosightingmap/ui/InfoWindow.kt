// Create a new file: app/src/main/java/com/ufomap/ufosightingmap/ui/CustomInfoWindow.kt
package com.ufomap.ufosightingmap.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.ufomap.ufosightingmap.R

class CustomInfoWindow(
    private val mapView: MapView
) : InfoWindow(R.layout.custom_info_window, mapView) {

    override fun onOpen(item: Any?) {
        if (item !is Marker) return

        val title = mView.findViewById<TextView>(R.id.title)
        val description = mView.findViewById<TextView>(R.id.description)

        title.text = item.title
        description.text = item.snippet
    }

    override fun onClose() {
        // Clean up resources if needed
    }
}