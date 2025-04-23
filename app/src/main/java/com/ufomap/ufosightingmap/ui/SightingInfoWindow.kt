// Create a proper InfoWindow implementation first
class SightingInfoWindow(mapView: MapView) : InfoWindow(R.layout.sighting_info_window, mapView) {
    override fun onOpen(item: Any?) {
        if (item !is Marker) return

        mView.findViewById<TextView>(R.id.title).text = item.title
        mView.findViewById<TextView>(R.id.shape).text = "Shape: ${item.snippet?.substringAfter("Shape: ")?.substringBefore("\n") ?: "Unknown"}"
        mView.findViewById<TextView>(R.id.description).text = item.snippet?.substringAfter("\n") ?: ""
        mView.findViewById<TextView>(R.id.date).text = (item.relatedObject as? Sighting)?.dateTime ?: "Unknown date"

        // Add button for showing full details (will implement later)
        mView.findViewById<Button>(R.id.details_button).setOnClickListener {
            // Store reference to launch detail activity later
            val sighting = item.relatedObject as? Sighting
            // Launch detail screen will be implemented later
        }
    }

    override fun onClose() { /* Clean up as needed */ }
}