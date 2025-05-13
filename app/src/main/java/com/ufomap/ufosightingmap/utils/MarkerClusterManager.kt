package com.ufomap.ufosightingmap.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.min
import android.view.MotionEvent


/**
 * A simple marker clustering implementation for OSMDroid.
 * Groups nearby markers into clusters for better performance and UI clarity.
 */
class MarkerClusterManager(
    private val context: Context,
    private val mapView: MapView
) {
    // List of all markers
    private val markers = mutableListOf<Marker>()

    // Cluster overlay
    private val clusterOverlay = ClusterOverlay()

    // Cluster settings
    private var maxClusteringZoomLevel = 17.0
    private var clusterDistance = 100 // px
    private var enableAnimation = false

    // Current clusters
    private val visibleClusters = mutableListOf<MarkerCluster>()

    init {
        // Add cluster overlay to map
        mapView.overlays.add(clusterOverlay)
    }

    /**
     * Add a marker to be managed by the cluster manager
     */
    fun addItem(marker: Marker) {
        markers.add(marker)
    }

    /**
     * Remove all managed markers
     */
    fun clearItems() {
        markers.clear()
        visibleClusters.clear()
    }

    /**
     * Set whether to animate markers when clustering/unclustering
     */
    fun setAnimation(enabled: Boolean) {
        enableAnimation = enabled
    }

    /**
     * Set the maximum zoom level at which clustering is performed
     */
    fun setMaxClusteringZoomLevel(zoomLevel: Double) {
        maxClusteringZoomLevel = zoomLevel
    }

    /**
     * Set the distance (in pixels) within which markers will be clustered
     */
    fun setClusterDistance(distanceInPixels: Int) {
        clusterDistance = distanceInPixels
    }

    /**
     * Update the clustering based on current map state
     */
    fun invalidate() {
        // Skip clustering if we're zoomed in too far
        if (mapView.zoomLevelDouble > maxClusteringZoomLevel) {
            // Show all markers individually
            visibleClusters.clear()

            // Add all individual markers to map
            mapView.overlays.removeAll(markers)
            mapView.overlays.addAll(markers)

            return
        }

        // Remove all individual markers
        mapView.overlays.removeAll(markers)

        // Create clusters
        createClusters()

        // Force map refresh
        mapView.invalidate()
    }

    /**
     * Create clusters from markers based on proximity
     */
    private fun createClusters() {
        // Clear existing clusters
        visibleClusters.clear()

        // Skip if no markers
        if (markers.isEmpty()) return

        // Process each marker
        val processed = mutableSetOf<Marker>()

        for (marker in markers) {
            // Skip already processed markers
            if (marker in processed) continue

            // Find nearby markers
            val nearbyMarkers = findNearbyMarkers(marker, processed)

            // Create a cluster for the marker and its neighbors
            if (nearbyMarkers.size > 1) {
                // Multiple markers - create a cluster
                val cluster = MarkerCluster()
                cluster.addItems(nearbyMarkers)
                visibleClusters.add(cluster)
                processed.addAll(nearbyMarkers)
            } else {
                // Single marker - add it directly
                mapView.overlays.add(marker)
                processed.add(marker)
            }
        }
    }

    /**
     * Find markers near the given marker
     */
    private fun findNearbyMarkers(marker: Marker, exclude: Set<Marker>): List<Marker> {
        val result = mutableListOf(marker)

        // Get screen coordinates of this marker
        val markerPoint = mapView.projection.toPixels(marker.position, null)

        // Check all other markers
        for (other in markers) {
            if (other === marker || other in exclude) continue

            // Get distance in screen pixels
            val otherPoint = mapView.projection.toPixels(other.position, null)
            val dx = markerPoint.x - otherPoint.x
            val dy = markerPoint.y - otherPoint.y
            val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

            // If within cluster distance, add to result
            if (distance <= clusterDistance) {
                result.add(other)
            }
        }

        return result
    }

    /**
     * Inner class representing a cluster of markers
     */
    inner class MarkerCluster {
        private val items = mutableListOf<Marker>()

        fun addItem(marker: Marker) {
            items.add(marker)
        }

        fun addItems(markers: Collection<Marker>) {
            items.addAll(markers)
        }

        val size: Int
            get() = items.size

        val position: GeoPoint
            get() {
                // Calculate center position of all markers in cluster
                var latSum = 0.0
                var lngSum = 0.0

                items.forEach { marker ->
                    latSum += marker.position.latitude
                    lngSum += marker.position.longitude
                }

                return GeoPoint(latSum / items.size, lngSum / items.size)
            }

        fun getItems(): List<Marker> = items.toList()
    }

    /**
     * Custom overlay to draw clusters on the map
     */
    inner class ClusterOverlay : Overlay() {
        private val textPaint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }

        private val bgPaint = Paint().apply {
            color = Color.parseColor("#6200EE") // Material primary color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return

            // Draw each cluster
            for (cluster in visibleClusters) {
                // Skip small clusters
                if (cluster.size < 2) continue

                // Get cluster position on screen
                val point = mapView.projection.toPixels(cluster.position, null)

                // Determine cluster size (bigger clusters = bigger markers)
                val radius = min(80, 40 + (Math.log10(cluster.size.toDouble()) * 10).toInt())

                // Draw cluster background
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius.toFloat(), bgPaint)

                // Draw outline
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius.toFloat(), strokePaint)

                // Draw count text
                val text = if (cluster.size > 99) "99+" else cluster.size.toString()
                val bounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                canvas.drawText(
                    text,
                    point.x.toFloat(),
                    point.y.toFloat() + (bounds.height() / 2),
                    textPaint
                )
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
            e ?: return false
            mapView ?: return false

            // Check if tap is on a cluster
            val tappedPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())

            for (cluster in visibleClusters) {
                // Convert positions to screen coordinates
                val clusterPoint = mapView.projection.toPixels(cluster.position, null)

                // Check if tap is within cluster circle
                val distance = kotlin.math.sqrt(
                    Math.pow(e.x - clusterPoint.x.toDouble(), 2.0) +
                            Math.pow(e.y - clusterPoint.y.toDouble(), 2.0)
                )

                // Determine visual radius of cluster
                val radius = min(80, 40 + (Math.log10(cluster.size.toDouble()) * 10).toInt())

                if (distance <= radius) {
                    // Tap is on this cluster
                    handleClusterTap(cluster, mapView)
                    return true
                }
            }

            return false
        }

        private fun handleClusterTap(cluster: MarkerCluster, mapView: MapView) {
            // If not many items, just zoom in
            if (cluster.size <= 10) {
                // Zoom in on the cluster
                mapView.controller.animateTo(cluster.position)
                mapView.controller.zoomIn()
                return
            }

            // For larger clusters, show a selection dialog
            // (In a real implementation, this would show a bottom sheet or dialog
            // listing the markers in the cluster)

            // For now, just zoom in
            mapView.controller.animateTo(cluster.position)
            mapView.controller.zoomIn()
        }
    }

    fun addItems(items: Collection<Marker>) {
        markers.addAll(items)
    }

    // Add method to remove individual markers
    fun removeItem(marker: Marker): Boolean {
        val removed = markers.remove(marker)
        if (removed) {
            // Force re-clustering if a marker was removed
            invalidate()
        }
        return removed
    }


    /**
     * Create a bitmap drawable for cluster markers
     */
    private fun createClusterMarker(count: Int): BitmapDrawable {
        val radius = min(80, 40 + (Math.log10(count.toDouble()) * 10).toInt())
        val bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#6200EE")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), bgPaint)

        // Draw outline
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), strokePaint)

        // Draw text
        val text = if (count > 99) "99+" else count.toString()
        val textPaint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(
            text,
            radius.toFloat(),
            radius.toFloat() + (bounds.height() / 2),
            textPaint
        )

        return BitmapDrawable(context.resources, bitmap)
    }
}