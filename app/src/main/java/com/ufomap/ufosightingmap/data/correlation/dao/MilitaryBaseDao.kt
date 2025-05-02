package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for military base operations.
 * Provides methods to query the military base database and correlate with sightings.
 */
@Dao
interface MilitaryBaseDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bases: List<MilitaryBase>)

    @Query("SELECT * FROM military_bases")
    fun getAllBases(): Flow<List<MilitaryBase>>

    @Query("SELECT * FROM military_bases WHERE id = :baseId")
    suspend fun getBaseById(baseId: String): MilitaryBase?

    @Query("SELECT COUNT(*) FROM military_bases")
    suspend fun count(): Int

    @Query("DELETE FROM military_bases")
    suspend fun deleteAll()

    // Geospatial queries

    @Query("""
        SELECT * FROM military_bases 
        WHERE latitude BETWEEN :south AND :north 
        AND longitude BETWEEN :west AND :east
    """)
    fun getBasesInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<MilitaryBase>>

    @Query("""
        SELECT * FROM military_bases
        WHERE state = :state
    """)
    fun getBasesByState(state: String): Flow<List<MilitaryBase>>

    // Correlation queries

    /**
     * Find bases within a specific distance of a coordinate point
     * Uses Haversine formula to calculate distance
     */
    @Query("""
        SELECT *, (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) AS distance 
        FROM military_bases
        HAVING distance <= :radiusKm
        ORDER BY distance
    """)
    fun getBasesNearPoint(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<MilitaryBase>>

    /**
     * Find the closest military base to a given coordinate
     */
    @Query("""
        SELECT *, (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) AS distance 
        FROM military_bases
        ORDER BY distance ASC
        LIMIT 1
    """)
    suspend fun getClosestBase(latitude: Double, longitude: Double): MilitaryBase?

    /**
     * Calculate the distance to the nearest military base for each sighting
     */
    @Transaction
    @Query("""
        SELECT s.*, 
        (
            SELECT MIN(
                6371 * acos(
                    cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                    cos(radians(mb.longitude) - radians(s.longitude)) + 
                    sin(radians(s.latitude)) * sin(radians(mb.latitude))
                )
            )
            FROM military_bases mb
        ) AS distance_to_nearest_base,
        (
            SELECT mb.id
            FROM military_bases mb
            ORDER BY (
                6371 * acos(
                    cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                    cos(radians(mb.longitude) - radians(s.longitude)) + 
                    sin(radians(s.latitude)) * sin(radians(mb.latitude))
                )
            ) ASC
            LIMIT 1
        ) AS nearest_base_id,
        (
            SELECT mb.name
            FROM military_bases mb
            ORDER BY (
                6371 * acos(
                    cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                    cos(radians(mb.longitude) - radians(s.longitude)) + 
                    sin(radians(s.latitude)) * sin(radians(mb.latitude))
                )
            ) ASC
            LIMIT 1
        ) AS nearest_base_name
        FROM sightings s
    """)
    fun getSightingsWithNearestBase(): Flow<List<SightingWithBaseDistance>>

    /**
     * Get sightings within a specified radius of any military base
     */
    @Transaction
    @Query("""
        SELECT DISTINCT s.*
        FROM sightings s
        WHERE EXISTS (
            SELECT 1
            FROM military_bases mb
            WHERE (
                6371 * acos(
                    cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                    cos(radians(mb.longitude) - radians(s.longitude)) + 
                    sin(radians(s.latitude)) * sin(radians(mb.latitude))
                )
            ) <= :radiusKm
        )
    """)
    fun getSightingsNearAnyBase(radiusKm: Double): Flow<List<Sighting>>

    /**
     * Get distribution of sightings by distance from military bases
     * Returns counts of sightings in distance bands (0-10km, 10-25km, 25-50km, etc.)
     */
    @Query("""
        SELECT 
            CASE 
                WHEN distance < 10 THEN '0-10 km'
                WHEN distance < 25 THEN '10-25 km'
                WHEN distance < 50 THEN '25-50 km'
                WHEN distance < 100 THEN '50-100 km'
                ELSE '100+ km'
            END AS distance_band,
            COUNT(*) as sighting_count
        FROM (
            SELECT s.id, MIN(
                6371 * acos(
                    cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                    cos(radians(mb.longitude) - radians(s.longitude)) + 
                    sin(radians(s.latitude)) * sin(radians(mb.latitude))
                )
            ) AS distance
            FROM sightings s
            CROSS JOIN military_bases mb
            GROUP BY s.id
        )
        GROUP BY distance_band
        ORDER BY MIN(distance)
    """)
    fun getSightingCountsByDistanceBand(): Flow<List<DistanceDistribution>>

    /**
     * Get the count of all sightings within a specific radius of any military base
     */
    @Query("""
        SELECT COUNT(DISTINCT s.id)
        FROM sightings s
        JOIN military_bases mb
        WHERE (
            6371 * acos(
                cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                cos(radians(mb.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(mb.latitude))
            )
        ) <= :radiusKm
    """)
    suspend fun countSightingsWithinRadius(radiusKm: Double): Int

    /**
     * Get the percentage of all sightings that are within a specific radius of any military base
     */
    @Query("""
        SELECT (
            CAST(COUNT(DISTINCT CASE 
                WHEN MIN(
                    6371 * acos(
                        cos(radians(s.latitude)) * cos(radians(mb.latitude)) * 
                        cos(radians(mb.longitude) - radians(s.longitude)) + 
                        sin(radians(s.latitude)) * sin(radians(mb.latitude))
                    )
                ) <= :radiusKm THEN s.id 
                ELSE NULL 
            END) AS FLOAT) / 
            CAST(COUNT(DISTINCT s.id) AS FLOAT)
        ) * 100 AS percentage
        FROM sightings s
        CROSS JOIN military_bases mb
    """)
    suspend fun getPercentageSightingsWithinRadius(radiusKm: Double): Float
}

/**
 * Data class for storing sighting distance to the nearest military base
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
    val distance_to_nearest_base: Double?,
    val nearest_base_id: String?,
    val nearest_base_name: String?
)

/**
 * Data class for distance distribution statistics
 */
data class DistanceDistribution(
    val distance_band: String,
    val sighting_count: Int
)