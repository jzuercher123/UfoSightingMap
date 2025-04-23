package com.ufomap.ufosightingmap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SightingDao {

    // Insert a list of sightings. Replace if conflict occurs based on primary key (though unlikely with autoGenerate).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sightings: List<Sighting>)

    // Get all sightings as a Flow (observes changes).
    @Query("SELECT * FROM sightings")
    fun getAllSightings(): Flow<List<Sighting>>

    // Get sightings within specific geographical bounds (useful for map view).
    // Note: Room doesn't directly support complex geographical queries.
    // This is a basic bounding box query. More complex queries might need raw queries or spatial extensions.
    @Query("SELECT * FROM sightings WHERE latitude BETWEEN :south AND :north AND longitude BETWEEN :west AND :east")
    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>>

    // Query to check if the database has any data.
    @Query("SELECT COUNT(*) FROM sightings")
    suspend fun count(): Int
}