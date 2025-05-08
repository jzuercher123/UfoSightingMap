// Create this file in: app/src/main/java/com/ufomap/ufosightingmap/data/correlation/dao/SightingWithBaseDistance.kt

package com.ufomap.ufosightingmap.data.correlation.dao

/**
 * Data class for storing sightings with nearest military base distance information
 */
data class SightingWithBaseDistance(
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
    val nearest_base_id: String?,
    val nearest_base_name: String?,
    val distance_to_nearest_base: Double?
)