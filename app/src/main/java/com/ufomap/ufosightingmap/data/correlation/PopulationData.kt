package com.ufomap.ufosightingmap.data.correlation.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data model for population data.
 * Used for correlating UFO sightings with population density.
 * Research shows a sub-linear relationship between population density and sighting frequency.
 */
@Entity(
    tableName = "population_data",
    indices = [Index("fipsCode"), Index("year")]
)
data class PopulationData(
    @PrimaryKey
    val id: String,

    // Geographic identifiers
    val fipsCode: String, // Federal Information Processing Standards code
    val countyName: String,
    val stateName: String,
    val stateAbbreviation: String,
    val country: String = "USA",

    // Centroid coordinates (for mapping)
    val latitude: Double,
    val longitude: Double,

    // Population data
    val year: Int,
    val population: Int,
    val landAreaSqKm: Double,
    val populationDensity: Double, // People per sq km

    // Additional demographic data
    val urbanPercentage: Double? = null,
    val medianAge: Double? = null,
    val educationHighSchoolPercentage: Double? = null,
    val educationBachelorsPercentage: Double? = null,

    // Economic data
    val medianIncome: Double? = null,
    val unemploymentRate: Double? = null,

    // Technology adoption
    val internetAccessPercentage: Double? = null, // May influence reporting capability
    val smartphoneUsagePercentage: Double? = null, // May influence reporting capability

    // Import metadata
    val dataSource: String,
    val lastUpdated: Long // timestamp
)