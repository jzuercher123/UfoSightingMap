package com.ufomap.ufosightingmap.data.correlation.dao

import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent

/**
 * Data class for storing sightings with concurrent astronomical events
 */
data class SightingWithAstronomicalEvents(
    // All fields from Sighting
    val id: Int,
    val dateTime: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val shape: String?,
    val duration: String?,
    val summary: String?,
    val posted: String?,
    val latitude: Double,
    val longitude: Double,
    val submittedBy: String?,
    val submissionDate: String?,
    val isUserSubmitted: Boolean,
    val submissionStatus: String,

    // Additional correlation fields
    val concurrent_event_ids: String?,
    val concurrent_event_names: String?
) {
    /**
     * Convert the comma-separated event IDs into a list
     */
    fun getEventIdsList(): List<String> {
        return concurrent_event_ids?.split(",") ?: emptyList()
    }

    /**
     * Convert the comma-separated event names into a list
     */
    fun getEventNamesList(): List<String> {
        return concurrent_event_names?.split(", ") ?: emptyList()
    }
}

/**
 * Data class for astronomical event type distribution
 */
data class EventTypeDistribution(
    val event_type: AstronomicalEvent.EventType,
    val sighting_count: Int
)

/**
 * Data class for meteor shower correlation statistics
 */
data class MeteorShowerCorrelation(
    val event_name: String,
    val sighting_count: Int,
    val fireball_count: Int
) {
    /**
     * Calculate the percentage of "fireball" shaped sightings
     * during this meteor shower
     */
    fun getFireballPercentage(): Float {
        if (sighting_count == 0) return 0f
        return (fireball_count.toFloat() / sighting_count) * 100
    }
}