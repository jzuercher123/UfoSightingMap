package com.ufomap.ufosightingmap.data // Ensure package name matches

import androidx.room.Entity
import androidx.room.PrimaryKey
// No Google Maps imports needed here anymore

// Extend Sighting.kt to include submission metadata
@Entity(tableName = "sightings")
data class Sighting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
    // New fields for user submissions
    val submittedBy: String? = null,
    val submissionDate: String? = null,
    val isUserSubmitted: Boolean = false,
    val submissionStatus: String = "approved" // "pending", "approved", "rejected"
)