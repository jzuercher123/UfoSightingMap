package com.ufomap.ufosightingmap.data.correlation.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data model for military bases.
 * Used for correlating UFO sightings with military installations.
 * Research has shown significant correlations between sighting locations and proximity to bases.
 */
@Entity(
    tableName = "military_bases",
    indices = [Index("latitude"), Index("longitude")]
)
data class MilitaryBase(
    @PrimaryKey
    val id: String,

    // Basic information
    val name: String,
    val type: String, // AIR_FORCE, NAVY, ARMY, etc.
    val branch: String,

    // Location data
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?,
    val country: String,

    // Additional metadata
    val isActive: Boolean = true,
    val establishedYear: Int? = null,
    val sizeAcres: Double? = null,
    val hasAirfield: Boolean = false,

    // Special fields for potential correlation factors
    val hasNuclearCapabilities: Boolean = false,
    val hasResearchFacilities: Boolean = false,
    val hasRestrictedAirspace: Boolean = false,

    // Import metadata
    val dataSource: String,
    val lastUpdated: Long // timestamp
)