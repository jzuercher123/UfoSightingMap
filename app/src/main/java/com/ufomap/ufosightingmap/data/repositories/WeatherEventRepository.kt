package com.ufomap.ufosightingmap.data.repositories

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ufomap.ufosightingmap.data.api.WeatherEventApi
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherEventDao
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date

/**
 * Repository for weather event data.
 * Manages interactions between the data sources and the rest of the app.
 */
class WeatherEventRepository(
    private val weatherEventDao: WeatherEventDao,
    private val context: Context,
    private val api: WeatherEventApi? = null
) {
    private val TAG = "WeatherEventRepository"

    // Expose current weather events as a Flow
    val currentWeatherEvents: Flow<List<WeatherEvent>> = try {
        weatherEventDao.getCurrentWeatherEvents()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting current weather events: ${e.message}", e)
                emit(emptyList())
            }
    } catch (e: Exception) {
        Log.e(TAG, "Error setting up weather events flow: ${e.message}", e)
        flowOf(emptyList())
    }

    /**
     * Initialize the weather event database with data if needed
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val count = weatherEventDao.count()
                if (count == 0) {
                    Log.d(TAG, "Weather event database is empty. Loading initial data.")
                    loadWeatherEventsFromJson()
                } else {
                    Log.d(TAG, "Weather event database already contains $count events.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking weather event database state", e)
            }
        }
    }

    /**
     * Force reload weather event data
     */
    suspend fun forceReloadData() {
        try {
            Log.d(TAG, "Forcing weather event database clear and reload")
            weatherEventDao.deleteAll()
            loadWeatherEventsFromJson()
        } catch (e: Exception) {
            Log.e(TAG, "Error during weather event force reload: ${e.message}", e)
        }
    }

    /**
     * Load weather event data from a JSON asset file
     */
    private suspend fun loadWeatherEventsFromJson() {
        try {
            // Try fetching from API first if available
            if (api != null) {
                try {
                    val apiData = api.getWeatherEvents()
                    if (apiData.isNotEmpty()) {
                        Log.d(TAG, "Successfully fetched ${apiData.size} weather events from API")
                        weatherEventDao.insertAll(apiData)
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load weather events from API: ${e.message}")
                    // Continue to load from local asset
                }
            }

            // Fall back to local JSON asset (placeholder for now)
            // This would load from an asset file in a real implementation
            Log.d(TAG, "Using placeholder weather event data")
            val placeholderEvents = createPlaceholderWeatherEvents()
            weatherEventDao.insertAll(placeholderEvents)
            Log.d(TAG, "Added ${placeholderEvents.size} placeholder weather events")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading weather event data: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weather event data: ${e.message}", e)
        }
    }

    /**
     * Get weather events near a specific location
     */
    fun getWeatherEventsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<WeatherEvent>> {
        return try {
            weatherEventDao.getWeatherEventsNearLocation(latitude, longitude, radiusKm)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    Log.e(TAG, "Error getting weather events near location: ${e.message}", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up weather events near location flow: ${e.message}", e)
            flowOf(emptyList())
        }
    }

    /**
     * Create placeholder weather events for testing
     */
    private fun createPlaceholderWeatherEvents(): List<WeatherEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            WeatherEvent(
                id = "weather-001",
                latitude = 40.7128,
                longitude = -74.0060,
                city = "New York",
                state = "NY",
                country = "USA",
                date = now,
                type = WeatherEvent.WeatherType.THUNDERSTORM,
                severity = 3,
                temperature = 22.5,
                cloudCover = 85,
                electricalActivity = 42,
                dataSource = "Placeholder Data",
                lastUpdated = now
            ),
            WeatherEvent(
                id = "weather-002",
                latitude = 34.0522,
                longitude = -118.2437,
                city = "Los Angeles",
                state = "CA",
                country = "USA",
                date = now,
                type = WeatherEvent.WeatherType.CLEAR_SKY,
                temperature = 28.3,
                cloudCover = 5,
                dataSource = "Placeholder Data",
                lastUpdated = now
            ),
            WeatherEvent(
                id = "weather-003",
                latitude = 41.8781,
                longitude = -87.6298,
                city = "Chicago",
                state = "IL",
                country = "USA",
                date = now,
                type = WeatherEvent.WeatherType.FOG,
                temperature = 15.8,
                cloudCover = 90,
                visibility = 0.5,
                dataSource = "Placeholder Data",
                lastUpdated = now
            )
        )
    }
}