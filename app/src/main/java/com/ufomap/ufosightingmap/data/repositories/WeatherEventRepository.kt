package com.ufomap.ufosightingmap.data.repositories

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository for weather event data.
 * Manages interactions between the data sources (API, local database) and the rest of the app.
 */
class WeatherEventRepository(
    internal val weatherEventDao: WeatherEventDao,
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
                    loadWeatherEventsFromApi()
                } else {
                    Log.d(TAG, "Weather event database already contains $count events.")

                    // Check if data is stale (older than 3 hours)
                    val latestEvent = weatherEventDao.getLatestWeatherEvent()
                    if (latestEvent != null) {
                        val threeHoursAgo = System.currentTimeMillis() - (3 * 60 * 60 * 1000)
                        if (latestEvent.lastUpdated < threeHoursAgo) {
                            Log.d(TAG, "Weather data is stale. Refreshing from API.")
                            loadWeatherEventsFromApi()
                        }
                    }
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
            loadWeatherEventsFromApi()
        } catch (e: Exception) {
            Log.e(TAG, "Error during weather event force reload: ${e.message}", e)
        }
    }

    /**
     * Load weather events from the API
     */
    private suspend fun loadWeatherEventsFromApi() {
        withContext(Dispatchers.IO) {
            try {
                if (api != null) {
                    try {
                        // Fetch weather data from API
                        val apiData = api.getWeatherEvents()
                        if (apiData.isNotEmpty()) {
                            Log.d(TAG, "Successfully fetched ${apiData.size} weather events from API")
                            weatherEventDao.insertAll(apiData)
                            return@withContext
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load weather events from API: ${e.message}", e)
                    }
                }

                // Fall back to creating placeholder data if API fails or isn't available
                Log.d(TAG, "Using placeholder weather event data")
                val placeholderEvents = createPlaceholderWeatherEvents()
                weatherEventDao.insertAll(placeholderEvents)
                Log.d(TAG, "Added ${placeholderEvents.size} placeholder weather events")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading weather events: ${e.message}", e)
            }
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
     * Get weather events by type
     */
    fun getWeatherEventsByType(type: WeatherEvent.WeatherType): Flow<List<WeatherEvent>> {
        return weatherEventDao.getWeatherEventsByType(type)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting weather events by type: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Get weather events during a date range
     */
    fun getWeatherEventsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<WeatherEvent>> {
        return weatherEventDao.getWeatherEventsBetweenDates(startTimestamp, endTimestamp)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting weather events between dates: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Get unusual weather events (those that might correlate with UFO sightings)
     */
    fun getUnusualWeatherEvents(): Flow<List<WeatherEvent>> {
        return weatherEventDao.getUnusualWeatherEvents()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting unusual weather events: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Create placeholder weather events for testing
     */
    private fun createPlaceholderWeatherEvents(): List<WeatherEvent> {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
            ),
            WeatherEvent(
                id = "weather-004",
                latitude = 40.6936,
                longitude = -89.5890,
                city = "Peoria",
                state = "IL",
                country = "USA",
                date = now - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                type = WeatherEvent.WeatherType.TEMPERATURE_INVERSION,
                hasInversionLayer = true,
                hasLightRefractionConditions = true,
                dataSource = "Placeholder Data",
                lastUpdated = now
            ),
            WeatherEvent(
                id = "weather-005",
                latitude = 37.7749,
                longitude = -122.4194,
                city = "San Francisco",
                state = "CA",
                country = "USA",
                date = now - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                type = WeatherEvent.WeatherType.FOG,
                temperature = 15.2,
                visibility = 0.3,
                hasLightRefractionConditions = true,
                dataSource = "Placeholder Data",
                lastUpdated = now
            )
        )
    }

    /**
     * Fetch current weather from the API for a specific location
     * This method can be called from the UI to get up-to-date weather
     */
    suspend fun fetchCurrentWeather(latitude: Double, longitude: Double): Result<WeatherEvent> {
        return withContext(Dispatchers.IO) {
            try {
                if (api == null) {
                    return@withContext Result.failure(Exception("Weather API not configured"))
                }

                // Format the point string for the API
                val pointStr = "$latitude,$longitude"

                // First get nearby stations
                val stationsResponse = api.getStationsNearPoint(pointStr)
                if (stationsResponse.observationStations.isEmpty()) {
                    return@withContext Result.failure(Exception("No weather stations found near this location"))
                }

                // Get the closest station's data
                val stationId = stationsResponse.observationStations.first()
                val observation = api.getLatestObservation(stationId)

                // Convert to our domain model
                val weatherEvent = WeatherEvent(
                    id = UUID.randomUUID().toString(),
                    latitude = latitude,
                    longitude = longitude,
                    city = null, // We'd need reverse geocoding to get this
                    state = null,
                    country = "USA", // Assuming US for now
                    date = System.currentTimeMillis(),
                    type = determineWeatherType(observation.properties.textDescription),
                    temperature = observation.properties.temperature.value,
                    humidity = observation.properties.relativeHumidity.value,
                    visibility = observation.properties.visibility.value,
                    windSpeed = observation.properties.windSpeed.value,
                    windDirection = convertWindDirection(observation.properties.windDirection.value),
                    pressure = observation.properties.barometricPressure.value,
                    dataSource = "weather.gov API",
                    lastUpdated = System.currentTimeMillis()
                )

                // Cache the result in the database
                weatherEventDao.insertAll(listOf(weatherEvent))

                Result.success(weatherEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current weather: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Determine weather type from description
     */
    private fun determineWeatherType(description: String): WeatherEvent.WeatherType {
        return when {
            description.contains("thunderstorm", ignoreCase = true) -> WeatherEvent.WeatherType.THUNDERSTORM
            description.contains("lightning", ignoreCase = true) -> WeatherEvent.WeatherType.LIGHTNING
            description.contains("fog", ignoreCase = true) -> WeatherEvent.WeatherType.FOG
            description.contains("rain", ignoreCase = true) -> WeatherEvent.WeatherType.HEAVY_RAIN
            description.contains("snow", ignoreCase = true) -> WeatherEvent.WeatherType.SNOW
            description.contains("hail", ignoreCase = true) -> WeatherEvent.WeatherType.HAIL
            description.contains("clear", ignoreCase = true) -> WeatherEvent.WeatherType.CLEAR_SKY
            else -> WeatherEvent.WeatherType.OTHER
        }
    }

    /**
     * Convert wind direction from degrees to cardinal direction
     */
    private fun convertWindDirection(degrees: Double?): String? {
        if (degrees == null) return null

        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((degrees % 360) / 22.5).toInt()
        return directions[index % 16]
    }
}