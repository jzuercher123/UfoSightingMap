package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for weather event operations.
 * Provides methods to query weather events and correlate with sightings.
 */
@Dao
interface WeatherEventDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<WeatherEvent>)

    @Query("SELECT * FROM weather_events")
    fun getAllWeatherEvents(): Flow<List<WeatherEvent>>

    @Query("SELECT * FROM weather_events WHERE id = :eventId")
    suspend fun getWeatherEventById(eventId: String): WeatherEvent?

    @Query("SELECT COUNT(*) FROM weather_events")
    suspend fun count(): Int

    @Query("DELETE FROM weather_events")
    suspend fun deleteAll()

    // Time-based queries

    /**
     * Get current/recent weather events
     */
    @Query("SELECT * FROM weather_events WHERE date >= (strftime('%s', 'now') * 1000 - 86400000) ORDER BY date DESC")
    fun getCurrentWeatherEvents(): Flow<List<WeatherEvent>>

    /**
     * Get weather events between two dates
     */
    @Query("SELECT * FROM weather_events WHERE date BETWEEN :startTimestamp AND :endTimestamp ORDER BY date")
    fun getWeatherEventsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<WeatherEvent>>

    /**
     * Get the latest weather event
     */
    @Query("SELECT * FROM weather_events ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeatherEvent(): WeatherEvent?

    // Type-based queries

    /**
     * Get weather events by type
     */
    @Query("SELECT * FROM weather_events WHERE type = :type ORDER BY date DESC")
    fun getWeatherEventsByType(type: WeatherEvent.WeatherType): Flow<List<WeatherEvent>>

    /**
     * Get unusual weather events that might correlate with UFO sightings
     */
    @Query("""
        SELECT * FROM weather_events 
        WHERE type IN (
            'TEMPERATURE_INVERSION', 'FOG', 'SPRITES', 
            'BALL_LIGHTNING', 'AURORA', 'THUNDERSTORM'
        )
        OR hasInversionLayer = 1 
        OR hasLightRefractionConditions = 1
        ORDER BY date DESC
    """)
    fun getUnusualWeatherEvents(): Flow<List<WeatherEvent>>

    // Geospatial queries

    /**
     * Find weather events within a specific distance of a coordinate point
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
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    fun getWeatherEventsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<WeatherEvent>>

    // Correlation queries

    /**
     * Get distribution of sightings by weather type
     */
    @Query("""
        SELECT 
            we.type as weather_type,
            COUNT(DISTINCT s.id) as event_count
        FROM sightings s
        JOIN weather_events we ON 
            abs(strftime('%s', s.dateTime) * 1000 - we.date) < 86400000 AND
            (6371 * acos(
                cos(radians(s.latitude)) * cos(radians(we.latitude)) * 
                cos(radians(we.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(we.latitude))
            )) <= 50
        GROUP BY we.type
        ORDER BY event_count DESC
    """)
    fun getWeatherTypeDistribution(): Flow<List<WeatherTypeDistribution>>

    /**
     * Get the count of all sightings that occurred during unusual weather
     */
    @Query("""
        SELECT COUNT(DISTINCT s.id)
        FROM sightings s
        WHERE EXISTS (
            SELECT 1
            FROM weather_events we
            WHERE abs(strftime('%s', s.dateTime) * 1000 - we.date) < 86400000
            AND (6371 * acos(
                cos(radians(s.latitude)) * cos(radians(we.latitude)) * 
                cos(radians(we.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(we.latitude))
            )) <= 50
            AND (
                we.type IN ('TEMPERATURE_INVERSION', 'FOG', 'SPRITES', 'BALL_LIGHTNING', 'AURORA', 'THUNDERSTORM')
                OR we.hasInversionLayer = 1 
                OR we.hasLightRefractionConditions = 1
            )
        )
    """)
    suspend fun countSightingsDuringUnusualWeather(): Int

    /**
     * Get the percentage of all sightings that occurred during unusual weather
     */
    @Query("""
        SELECT (
            CAST(
                (SELECT COUNT(DISTINCT s1.id)
                 FROM sightings s1
                 WHERE EXISTS (
                     SELECT 1
                     FROM weather_events we1
                     WHERE abs(strftime('%s', s1.dateTime) * 1000 - we1.date) < 86400000
                     AND (6371 * acos(
                         cos(radians(s1.latitude)) * cos(radians(we1.latitude)) * 
                         cos(radians(we1.longitude) - radians(s1.longitude)) + 
                         sin(radians(s1.latitude)) * sin(radians(we1.latitude))
                     )) <= 50
                     AND (
                         we1.type IN ('TEMPERATURE_INVERSION', 'FOG', 'SPRITES', 'BALL_LIGHTNING', 'AURORA', 'THUNDERSTORM')
                         OR we1.hasInversionLayer = 1 
                         OR we1.hasLightRefractionConditions = 1
                     )
                 ))
            AS FLOAT) / 
            CAST((SELECT COUNT(*) FROM sightings) AS FLOAT)
        ) * 100 
    """)
    suspend fun getPercentageSightingsDuringUnusualWeather(): Float
}

/**
 * Data class for weather type distribution statistics
 */
data class WeatherTypeDistribution(
    val weather_type: WeatherEvent.WeatherType,
    val event_count: Int
)