package com.ufomap.ufosightingmap.data.correlation.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data model for astronomical events that could be mistaken for UFOs.
 * Many studies have shown that astronomical phenomena are often misidentified as UFOs.
 */
@Entity(
    tableName = "astronomical_events",
    indices = [Index("startDate"), Index("endDate")]
)
data class AstronomicalEvent(
    @PrimaryKey
    val id: String,

    // Event identification
    val name: String,
    val type: EventType,

    // Time ranges
    val startDate: Long, // UTC timestamp
    val peakDate: Long? = null, // For events with peak activity (like meteor showers)
    val endDate: Long, // UTC timestamp

    // Visibility factors
    val visibilityRating: Int, // 1-10 scale of how visible/bright
    val bestViewingTime: String? = null, // e.g. "After midnight", "Evening"
    val visibleRegions: List<String> = listOf(), // Geographic regions where visible

    // Descriptive information
    val description: String,
    val expectedRate: Int? = null, // For meteor showers (meteors per hour)

    // Position data (when applicable)
    val radiantConstellation: String? = null, // For meteor showers
    val peakElevation: Double? = null, // Degrees above horizon
    val peakAzimuth: Double? = null, // Degrees from north

    // Satellite specific (for satellite passes)
    val satelliteName: String? = null,
    val satelliteId: String? = null,
    val brightness: Double? = null, // Magnitude

    // Import metadata
    val dataSource: String,
    val lastUpdated: Long // timestamp
) {
    enum class EventType {
        METEOR_SHOWER,
        PLANETARY_CONJUNCTION,
        BRIGHT_PLANET,
        SATELLITE_PASS,
        LUNAR_PHASE,
        STARLINK_TRAIN,
        ASTEROID_PASS,
        COMET,
        ATMOSPHERIC_PHENOMENON,
        OTHER
    }
}