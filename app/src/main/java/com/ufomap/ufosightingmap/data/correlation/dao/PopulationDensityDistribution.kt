package com.ufomap.ufosightingmap.data.correlation.dao

/**
 * Data class for population density distribution statistics
 */
data class PopulationDensityDistribution(
    val density_category: String,
    val sighting_count: Int
)