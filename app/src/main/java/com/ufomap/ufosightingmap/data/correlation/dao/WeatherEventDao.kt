package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for weather events.
 * Provides methods to query weather events and correlate with sightings.
 */
@Dao
interface WeatherEventDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<WeatherEvent>)

    @Query("SELECT * FROM weather_events")
    fun getAllWeatherEvents(): Flow<List<WeatherEvent>>

    @Query("SELECT COUNT(*) FROM weather_events")
    suspend fun count(): Int

    @Query("DELETE FROM weather_events")
    suspend fun deleteAll()

    // Time-based queries

    /**
     * Get weather events between dates
     */
    @Query("""
        SELECT * FROM weather_events
        WHERE date >= :startTime AND date <= :endTime
        ORDER BY date DESC
    """)
    fun getWeatherEventsBetweenDates(startTime: Long, endTime: Long): Flow<List<WeatherEvent>>

    /**
     * Get current weather events (using system time)
     */
    @Query("""
        SELECT * FROM weather_events
        WHERE date >= (strftime('%s', 'now') * 1000) - 86400000
        ORDER BY date DESC
    """)
    fun getCurrentWeatherEvents(): Flow<List<WeatherEvent>>

    // Location-based queries

    /**
     * Get weather events near a specific location using Haversine formula
     */
    @Query("""
        SELECT *, (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) AS distance 
        FROM weather_events
        WHERE (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) <= :radiusKm
        ORDER BY distance
    """)
    fun getWeatherEventsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<WeatherEvent>>

    // Correlation queries

    /**
     * Get correlation between weather types and sightings
     */
    @Query("""
        SELECT 
            we.type as weather_type,
            COUNT(DISTINCT s.id) as sighting_count
        FROM weather_events we
        JOIN sightings s ON 
            (6371 * acos(
                cos(radians(s.latitude)) * cos(radians(we.latitude)) * 
                cos(radians(we.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(we.latitude))
            )) <= 50
            AND ABS(strftime('%s', s.dateTime) * 1000 - we.date) <= 86400000
        GROUP BY we.type
        ORDER BY sighting_count DESC
    """)
    fun getWeatherTypeSightingCorrelation(): Flow<List<WeatherTypeDistribution>>

    /**
     * Get the percentage of sightings that occur during unusual weather
     */
    @Query("""
        SELECT (
            CAST(
                (SELECT COUNT(DISTINCT s.id)
                 FROM sightings s
                 WHERE s.dateTime IS NOT NULL 
                 AND EXISTS (
                    SELECT 1 
                    FROM weather_events we
                    WHERE (6371 * acos(
                        cos(radians(s.latitude)) * cos(radians(we.latitude)) * 
                        cos(radians(we.longitude) - radians(s.longitude)) + 
                        sin(radians(s.latitude)) * sin(radians(we.latitude))
                    )) <= 50
                    AND ABS(strftime('%s', s.dateTime) * 1000 - we.date) <= 86400000
                    AND we.type IN ('THUNDERSTORM', 'FOG', 'TEMPERATURE_INVERSION', 'TORNADO', 'SPRITES', 'BALL_LIGHTNING')
                 ))
             AS FLOAT) / 
            CAST((SELECT COUNT(*) FROM sightings) AS FLOAT)
        ) * 100 AS percentage
    """)
    suspend fun getUnusualWeatherSightingPercentage(): Float
}