package com.ufomap.ufosightingmap.data.repositories // Make sure package name is correct

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Import the interface, even if empty
import com.ufomap.ufosightingmap.data.api.AstronomicalEventApi
import com.ufomap.ufosightingmap.data.correlation.dao.AstronomicalEventDao
import com.ufomap.ufosightingmap.data.correlation.dao.EventTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.MeteorShowerCorrelation
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithAstronomicalEvents
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Repository for astronomical event data.
 * Manages data operations between the data sources (local database, JSON assets, APIs)
 * and the rest of the app.
 */
class AstronomicalEventRepository(
    private val astronomicalEventDao: AstronomicalEventDao,
    private val context: Context,
    private val api: AstronomicalEventApi? = null // Accept nullable API
) {
    private val TAG = "AstroEventRepository"

    // Expose all events as a Flow
    val allEvents: Flow<List<AstronomicalEvent>> = astronomicalEventDao.getAllEvents()
        .flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "Error getting astronomical events: ${e.message}", e)
            emit(emptyList()) // Emit empty list on error
        }

    // Expose sightings with concurrent events data
    val sightingsWithEvents: Flow<List<SightingWithAstronomicalEvents>> =
        astronomicalEventDao.getSightingsWithConcurrentEvents()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting sightings with event correlation: ${e.message}", e)
                emit(emptyList()) // Emit empty list on error
            }

    // Expose event type distribution statistics
    val sightingsByEventType: Flow<List<EventTypeDistribution>> =
        astronomicalEventDao.getSightingCountsByEventType()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting event type distribution: ${e.message}", e)
                emit(emptyList()) // Emit empty list on error
            }

    // Expose meteor shower correlation data
    val meteorShowerCorrelation: Flow<List<MeteorShowerCorrelation>> =
        astronomicalEventDao.getMeteorShowerCorrelation()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting meteor shower correlation: ${e.message}", e)
                emit(emptyList()) // Emit empty list on error
            }

    /**
     * Initialize the astronomical event database with data from the JSON asset file.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val count = astronomicalEventDao.count()
                if (count == 0) {
                    Log.d(TAG, "Astronomical event database is empty. Loading from JSON asset.")
                    loadEventsFromJson()
                } else {
                    Log.d(TAG, "Astronomical event database already contains $count events.")
                    // Optional: Add logic to check for staleness/updates here if needed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking astronomical event database state", e)
            }
        }
    }

    /**
     * Force reload astronomical event data (useful for testing or when assets are updated)
     */
    suspend fun forceReloadData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Forcing astronomical event database clear and reload")
                astronomicalEventDao.deleteAll()
                loadEventsFromJson()
            } catch (e: Exception) {
                Log.e(TAG, "Error during astronomical event force reload: ${e.message}", e)
                // Let caller handle the exception
                throw e
            }
        }
    }

    /**
     * Load astronomical event data from the API if available, else fall back to local JSON file
     */
    private suspend fun loadEventsFromJson() {
        // No explicit 'suspend' needed if only doing file I/O,
        // but keep it if API call might be added later.
        // Run the file reading on IO dispatcher.
        withContext(Dispatchers.IO) {
            try {
                // Try to fetch from API first if configured and implemented
                // if (api != null) {
                //     try {
                //         val apiData = api.getAstronomicalEvents() // Assuming this method exists
                //         if (apiData.isNotEmpty()) {
                //             Log.d(TAG, "Successfully fetched ${apiData.size} astronomical events from API")
                //             // TODO: Map API DTOs to AstronomicalEvent entity
                //             // val mappedEvents = mapApiDataToEvents(apiData)
                //             // astronomicalEventDao.insertAll(mappedEvents)
                //             return@withContext
                //         }
                //     } catch (e: Exception) {
                //         Log.w(TAG, "Failed to load astronomical events from API: ${e.message}")
                //         // Continue to load from local asset
                //     }
                // }

                // Fall back to local JSON asset
                Log.d(TAG, "Loading astronomical event data from local JSON asset: astronomy_events_2025.json")
                val jsonString = context.assets.open("astronomy_events_2025.json").bufferedReader().use { it.readText() }
                Log.d(TAG, "Successfully read astronomy_events_2025.json file (${jsonString.length} chars)")

                // Define the type token for Gson
                val eventListType = object : TypeToken<List<AstronomicalEvent>>() {}.type
                val events: List<AstronomicalEvent> = Gson().fromJson(jsonString, eventListType)

                if (events.isNotEmpty()) {
                    Log.d(TAG, "Parsed ${events.size} astronomical events from JSON")
                    astronomicalEventDao.insertAll(events)
                    Log.d(TAG, "Successfully loaded astronomical events from JSON into DB.")
                } else {
                    Log.w(TAG, "Parsed JSON but found no astronomical events.")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading astronomy_events_2025.json: ${e.message}", e)
            } catch (e: Exception) { // Catch broader exceptions during parsing/DB insertion
                Log.e(TAG, "Error parsing or inserting astronomical event data: ${e.message}", e)
            }
        }
    }

    // --- Functions providing specific data flows ---

    /** Gets events occurring on a specific date */
    fun getEventsOnDate(date: Date): Flow<List<AstronomicalEvent>> {
        val timestamp = date.time
        return astronomicalEventDao.getEventsOnDate(timestamp)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting events on date: ${e.message}", e)
                emit(emptyList())
            }
    }

    /** Gets events occurring during a date range */
    fun getEventsInDateRange(startDate: Date, endDate: Date): Flow<List<AstronomicalEvent>> {
        val startTimestamp = startDate.time
        val endTimestamp = endDate.time
        return astronomicalEventDao.getEventsInDateRange(startTimestamp, endTimestamp)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting events in date range: ${e.message}", e)
                emit(emptyList())
            }
    }

    /** Gets events by type */
    fun getEventsByType(type: AstronomicalEvent.EventType): Flow<List<AstronomicalEvent>> {
        return astronomicalEventDao.getEventsByType(type)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting events by type: ${e.message}", e)
                emit(emptyList())
            }
    }

    /** Gets high visibility astronomical events (visibility rating >= threshold) */
    fun getHighVisibilityEvents(minVisibility: Int = 7): Flow<List<AstronomicalEvent>> {
        return astronomicalEventDao.getHighVisibilityEvents(minVisibility)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting high visibility events: ${e.message}", e)
                emit(emptyList())
            }
    }

    /** Gets the percentage of all sightings that occurred during astronomical events */
    suspend fun getPercentageSightingsDuringEvents(): Float {
        return try {
            withContext(Dispatchers.IO) {
                astronomicalEventDao.getPercentageSightingsDuringEvents() ?: 0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating percentage sightings during events: ${e.message}", e)
            0f // Return 0% on error
        }
    }

    /** Gets current astronomical events happening today */
    fun getCurrentEvents(): Flow<List<AstronomicalEvent>> {
        val today = Calendar.getInstance().time
        return getEventsOnDate(today)
    }

    /** Parses a date string in the format yyyy-MM-dd to a Date object */
    fun parseDate(dateString: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC") // Ensure consistent timezone
            format.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }
}