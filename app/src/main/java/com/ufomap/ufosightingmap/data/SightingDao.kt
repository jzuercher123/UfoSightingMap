package com.ufomap.ufosightingmap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SightingDao {

    // Insert a list of sightings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sightings: List<Sighting>)

    // Get all sightings as a Flow
    @Query("SELECT * FROM sightings")
    fun getAllSightings(): Flow<List<Sighting>>

    // Get sightings within specific geographical bounds
    @Query("SELECT * FROM sightings WHERE latitude BETWEEN :south AND :north AND longitude BETWEEN :west AND :east")
    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>>

    // Query to check if the database has any data
    @Query("SELECT COUNT(*) FROM sightings")
    suspend fun count(): Int

    // Get a specific sighting by ID
    @Query("SELECT * FROM sightings WHERE id = :id LIMIT 1")
    fun getSightingById(id: Int): Flow<Sighting?>

    // Optimized query for filtered sightings - using indexed parameters and avoiding unnecessary wildcards
    @Query("SELECT * FROM sightings WHERE " +
            "(:shape IS NULL OR shape = :shape) AND " +
            "(:city IS NULL OR city = :city) AND " +
            "(:country IS NULL OR country = :country) AND " +
            "(:state IS NULL OR state = :state) AND " +
            "(:startDate IS NULL OR dateTime >= :startDate) AND " +
            "(:endDate IS NULL OR dateTime <= :endDate) AND " +
            "(:searchText IS NULL OR " +
            "city LIKE '%' || :searchText || '%' OR " +
            "state LIKE '%' || :searchText || '%' OR " +
            "country LIKE '%' || :searchText || '%' OR " +
            "summary LIKE '%' || :searchText || '%' OR " +
            "shape LIKE '%' || :searchText || '%')")
    fun getFilteredSightings(
        shape: String? = null,
        city: String? = null,
        country: String? = null,
        state: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        searchText: String? = null
    ): Flow<List<Sighting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSighting(sighting: Sighting): Long

    @Query("SELECT * FROM sightings WHERE submittedBy = :submittedBy ORDER BY submissionDate DESC")
    fun getUserSubmissions(submittedBy: String): Flow<List<Sighting>>
}