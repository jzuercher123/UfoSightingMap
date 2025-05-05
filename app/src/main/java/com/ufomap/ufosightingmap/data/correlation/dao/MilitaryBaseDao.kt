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
        WHERE (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) <= :radiusKm
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
     * Get sightings within a specified radius of any military base
     */
    @Transaction
    @Query("""
        SELECT DISTINCT sightings.*
        FROM sightings
        JOIN military_bases ON (
            6371 * acos(
                cos(radians(sightings.latitude)) * cos(radians(military_bases.latitude)) * 
                cos(radians(military_bases.longitude) - radians(sightings.longitude)) + 
                sin(radians(sightings.latitude)) * sin(radians(military_bases.latitude))
            )
        ) <= :radiusKm
    """)
    fun getSightingsNearAnyBase(radiusKm: Double): Flow<List<Sighting>>

    /**
     * Get distribution of sightings by distance from military bases
     * Returns counts of sightings in distance bands (0-10km, 10-25km, 25-50km, etc.)
     */
    @Query("""
        SELECT 
            CASE 
                WHEN min_distance < 10 THEN '0-10 km'
                WHEN min_distance < 25 THEN '10-25 km'
                WHEN min_distance < 50 THEN '25-50 km'
                WHEN min_distance < 100 THEN '50-100 km'
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
            ) AS min_distance
            FROM sightings s
            CROSS JOIN military_bases mb
            GROUP BY s.id
        ) AS distances
        GROUP BY distance_band
        ORDER BY MIN(min_distance)
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
            CAST(
                (SELECT COUNT(DISTINCT s1.id)
                 FROM sightings s1
                 WHERE EXISTS (
                     SELECT 1
                     FROM military_bases mb1
                     WHERE (
                         6371 * acos(
                             cos(radians(s1.latitude)) * cos(radians(mb1.latitude)) * 
                             cos(radians(mb1.longitude) - radians(s1.longitude)) + 
                             sin(radians(s1.latitude)) * sin(radians(mb1.latitude))
                         )
                     ) <= :radiusKm
                 ))
            AS FLOAT) / 
            CAST((SELECT COUNT(*) FROM sightings) AS FLOAT)
        ) * 100 
    """)
    suspend fun getPercentageSightingsWithinRadius(radiusKm: Double): Float
}