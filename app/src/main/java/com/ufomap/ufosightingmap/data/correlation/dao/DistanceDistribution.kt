package com.ufomap.ufosightingmap.data.correlation.dao

/**
 * Data class for distance distribution statistics between UFO sightings and military bases
 */
data class DistanceDistribution(
    val distance_band: String,
    val sighting_count: Int
)