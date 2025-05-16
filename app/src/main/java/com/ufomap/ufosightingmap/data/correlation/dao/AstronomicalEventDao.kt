package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for astronomical events.
 * Provides methods to query astronomical events and correlate with sightings.
 */
@Dao
interface AstronomicalEventDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: Unit)

    @Query("SELECT * FROM astronomical_events")
    fun getAllEvents(): Flow<List<AstronomicalEvent>>

    @Query("SELECT * FROM astronomical_events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): AstronomicalEvent?

    @Query("SELECT COUNT(*) FROM astronomical_events")
    suspend fun count(): Int

    @Query("DELETE FROM astronomical_events")
    suspend fun deleteAll()

    // Type-based queries

    @Query("""
        SELECT * FROM astronomical_events 
        WHERE type = :eventType
        ORDER BY startDate
    """)
    fun getEventsByType(eventType: AstronomicalEvent.EventType): Flow<List<AstronomicalEvent>>

    @Query("""
        SELECT * FROM astronomical_events 
        WHERE visibilityRating >= :minVisibility
        ORDER BY visibilityRating DESC
    """)
    fun getHighVisibilityEvents(minVisibility: Int): Flow<List<AstronomicalEvent>>

    // Time-based queries

    /**
     * Get astronomical events occurring on a specific date
     * Time is converted to midnight-to-midnight range in UTC
     */
    @Query("""
        SELECT * FROM astronomical_events
        WHERE :date BETWEEN startDate AND endDate
        ORDER BY startDate
    """)
    fun getEventsOnDate(date: Long): Flow<List<AstronomicalEvent>>

    /**
     * Get astronomical events occurring during a date range
     */
    @Query("""
        SELECT * FROM astronomical_events
        WHERE 
            (startDate BETWEEN :startDate AND :endDate) OR
            (endDate BETWEEN :startDate AND :endDate) OR
            (startDate <= :startDate AND endDate >= :endDate)
        ORDER BY startDate
    """)
    fun getEventsInDateRange(startDate: Long, endDate: Long): Flow<List<AstronomicalEvent>>

    // Correlation queries

    /**
     * Get sightings with concurrent astronomical events
     */
    @Transaction
    @Query("""
        SELECT s.*, 
        (
            SELECT GROUP_CONCAT(ae.id, ',')
            FROM astronomical_events ae
            WHERE s.dateTime IS NOT NULL AND
            strftime('%s', s.dateTime) * 1000 BETWEEN ae.startDate AND ae.endDate
        ) AS concurrent_event_ids,
        (
            SELECT GROUP_CONCAT(ae.name, ', ')
            FROM astronomical_events ae
            WHERE s.dateTime IS NOT NULL AND
            strftime('%s', s.dateTime) * 1000 BETWEEN ae.startDate AND ae.endDate
        ) AS concurrent_event_names
        FROM sightings s
        WHERE concurrent_event_ids IS NOT NULL
    """)
    fun getSightingsWithConcurrentEvents(): Flow<List<SightingWithAstronomicalEvents>>

    /**
     * Get distribution of sightings by event type
     */
    @Query("""
        SELECT ae.type as event_type, COUNT(DISTINCT s.id) as sighting_count
        FROM astronomical_events ae
        JOIN sightings s ON s.dateTime IS NOT NULL AND
            strftime('%s', s.dateTime) * 1000 BETWEEN ae.startDate AND ae.endDate
        GROUP BY ae.type
        ORDER BY sighting_count DESC
    """)
    fun getSightingCountsByEventType(): Flow<List<EventTypeDistribution>>

    /**
     * Get meteor shower correlation data
     */
    @Query("""
        SELECT 
            ae.name as event_name,
            COUNT(DISTINCT s.id) as sighting_count,
            COUNT(DISTINCT CASE WHEN s.shape = 'Fireball' THEN s.id ELSE NULL END) as fireball_count
        FROM astronomical_events ae
        JOIN sightings s ON s.dateTime IS NOT NULL AND
            strftime('%s', s.dateTime) * 1000 BETWEEN ae.startDate AND ae.endDate
        WHERE ae.type = 'METEOR_SHOWER'
        GROUP BY ae.name
        ORDER BY sighting_count DESC
    """)
    fun getMeteorShowerCorrelation(): Flow<List<MeteorShowerCorrelation>>

    /**
     * Get the percentage of all sightings that occurred during astronomical events
     */
    @Query("""
        SELECT (
            CAST(COUNT(DISTINCT CASE 
                WHEN s.dateTime IS NOT NULL AND EXISTS (
                    SELECT 1 FROM astronomical_events ae
                    WHERE strftime('%s', s.dateTime) * 1000 BETWEEN ae.startDate AND ae.endDate
                ) THEN s.id 
                ELSE NULL 
            END) AS FLOAT) / 
            CAST(COUNT(DISTINCT s.id) AS FLOAT)
        ) * 100 AS percentage
        FROM sightings s
    """)
    suspend fun getPercentageSightingsDuringEvents(): Float

    @Transaction
    suspend fun updateEvents(events: List<AstronomicalEvent>) {
        // Only update records that have actually changed
        val existingIds = events.map { it.id }
        val existingEvents = getEventsByIds(existingIds)

        val existingMap = existingEvents.associateBy { it.id }
        val toUpdate = events.filter { event ->
            val existing = existingMap[event.id]
            existing == null || existing != event
        }

        if (toUpdate.isNotEmpty()) {
            insertAll(toUpdate)
        }
    }

    @Query("SELECT * FROM astronomical_events WHERE id IN (:ids)")
    suspend fun getEventsByIds(ids: List<String>): List<AstronomicalEvent>
}