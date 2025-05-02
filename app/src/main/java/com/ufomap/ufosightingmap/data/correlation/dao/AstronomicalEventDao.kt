package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ufomap.ufosightingmap.data.Sighting
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
    suspend fun insertAll(events: List<AstronomicalEvent>)

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