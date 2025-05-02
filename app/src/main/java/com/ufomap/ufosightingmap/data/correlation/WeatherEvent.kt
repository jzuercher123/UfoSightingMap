package com.ufomap.ufosightingmap.data.correlation.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data model for weather and atmospheric events that could influence UFO sightings.
 * Research has shown correlations between weather conditions (particularly thunderstorms)
 * and reported UFO sightings.
 */
@Entity(
    tableName = "weather_events",
    indices = [Index("date"), Index("latitude", "longitude")]
)
data class WeatherEvent(
    @PrimaryKey
    val id: String,

    // Location data
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?,
    val country: String,

    // Time information
    val date: Long, // UTC timestamp

    // Event type and characteristics
    val type: WeatherType,
    val severity: Int? = null, // 1-5 scale when applicable

    // Meteorological data
    val temperature: Double? = null, // Celsius
    val humidity: Double? = null, // Percentage
    val cloudCover: Int? = null, // Percentage
    val windSpeed: Double? = null, // km/h
    val windDirection: String? = null, // N, S, E, W, etc.
    val pressure: Double? = null, // hPa
    val visibility: Double? = null, // km

    // Atmospheric conditions
    val hasInversionLayer: Boolean = false, // Temperature inversion
    val hasLightRefractionConditions: Boolean = false,
    val electricalActivity: Int? = null, // Lightning strikes count when available

    // Import metadata
    val dataSource: String,
    val lastUpdated: Long // timestamp
) {
    enum class WeatherType {
        THUNDERSTORM,
        LIGHTNING,
        FOG,
        TEMPERATURE_INVERSION,
        HEAVY_RAIN,
        SNOW,
        HAIL,
        TORNADO,
        HURRICANE,
        CLEAR_SKY,
        AURORA,
        SPRITES,
        BALL_LIGHTNING,
        OTHER
    }
}