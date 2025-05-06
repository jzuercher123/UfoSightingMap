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
import java.time.OffsetDateTime // Use java.time for modern date/time handling
import java.time.format.DateTimeFormatter
import java.util.Locale // Keep Locale for SimpleDateFormat if used elsewhere
import java.util.UUID
import java.text.SimpleDateFormat // Keep if createPlaceholderWeatherEvents uses it

/**
 * Repository for weather event data.
 * Manages interactions between the data sources (API, local database) and the rest of the app.
 */
class WeatherEventRepository(
    internal val weatherEventDao: WeatherEventDao,
    private val context: Context, // Keep context if needed for assets/resources
    private val api: WeatherEventApi? = null
) {
    private val TAG = "WeatherEventRepository"

    // Expose current weather events as a Flow from DAO
    // Note: This flow reflects the DB state, errors during API fetch are handled separately.
    val currentWeatherEvents: Flow<List<WeatherEvent>> = weatherEventDao.getCurrentWeatherEvents()
        .flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "Error getting current weather events from DB: ${e.message}", e)
            emit(emptyList()) // Emit empty list on DB error
        }

    /**
     * Initialize the weather event database with data if needed.
     * Checks count and staleness, then calls loadWeatherEventsFromApi.
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
                        } else {
                            Log.d(TAG, "Weather data is recent enough.")
                        }
                    } else {
                        Log.d(TAG,"Could not determine latest event timestamp, assuming fresh.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking weather event database state: ${e.message}", e)
                // Don't try to update ViewModel state directly here.
                // Let the ViewModel handle errors based on function results or flow exceptions.
            }
        }
    }

    /**
     * Force reload weather event data by clearing the DB and fetching from API.
     * Throws exception on failure.
     */
    suspend fun forceReloadData() {
        // No try-catch here, let the caller (ViewModel) handle exceptions
        Log.d(TAG, "Forcing weather event database clear and reload")
        weatherEventDao.deleteAll()
        loadWeatherEventsFromApi() // This function now handles its own errors internally
    }

    /**
     * Load weather events from the API, focusing on severe alerts.
     * Handles internal errors and falls back to placeholders.
     * Errors during API fetch are logged, caller should monitor flows for data changes.
     */
    private suspend fun loadWeatherEventsFromApi() {
        if (api == null) {
            Log.w(TAG, "Weather API service is null. Falling back to placeholders.")
            insertPlaceholderData() // Use a separate function for clarity
            return
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching severe weather alerts from weather.gov API...")
                val alertsResponse = api.getSevereWeatherEvents(
                    severity = "Severe,Extreme",
                    status = "Actual"
                )
                Log.d(TAG, "Received ${alertsResponse.features.size} severe alert features.")

                if (alertsResponse.features.isNotEmpty()) {
                    val weatherEvents = mapAlertsToWeatherEvents(alertsResponse.features)
                    if (weatherEvents.isNotEmpty()) {
                        Log.d(TAG, "Mapped ${weatherEvents.size} alerts to WeatherEvent objects.")
                        weatherEventDao.insertAll(weatherEvents) // Using default REPLACE strategy
                        Log.d(TAG, "Successfully inserted/updated severe weather events into database.")
                    } else {
                        Log.d(TAG, "No mappable severe weather events found in API response.")
                    }
                } else {
                    Log.d(TAG, "No active severe alerts found from API.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load weather events from API: ${e.message}", e)
                // Optionally: Propagate error differently if needed, but often just logging
                // and falling back to placeholders (if desired) is enough.
                // Consider adding a specific state/event for sync failure if UI needs it.
                insertPlaceholderData() // Fallback to placeholder data
            }
        }
    }

    /**
     * Helper function to insert placeholder data into the database.
     */
    private suspend fun insertPlaceholderData() {
        withContext(Dispatchers.IO) {
            try {
                if (weatherEventDao.count() == 0) { // Only insert if DB is truly empty
                    Log.d(TAG, "Inserting placeholder weather event data.")
                    val placeholderEvents = createPlaceholderWeatherEvents()
                    weatherEventDao.insertAll(placeholderEvents)
                    Log.d(TAG, "Added ${placeholderEvents.size} placeholder weather events.")
                } else {
                    Log.d(TAG, "Database not empty, skipping placeholder insertion.")
                }
            } catch (dbError: Exception) {
                Log.e(TAG, "Error inserting placeholder weather data: ${dbError.message}", dbError)
                // Log DB error, don't update external state directly
            }
        }
    }

    /**
     * Maps a list of API AlertFeatures to a list of WeatherEvent entities.
     */
    private fun mapAlertsToWeatherEvents(alertFeatures: List<WeatherEventApi.AlertFeature>): List<WeatherEvent> {
        val events = mutableListOf<WeatherEvent>()
        val now = System.currentTimeMillis()
        val apiDateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        for (feature in alertFeatures) {
            val props = feature.properties
            try {
                val onsetTime = props.onset?.let { runCatching { OffsetDateTime.parse(it, apiDateFormatter).toInstant().toEpochMilli() }.getOrNull() }
                val effectiveTime = runCatching { OffsetDateTime.parse(props.effective, apiDateFormatter).toInstant().toEpochMilli() }.getOrNull()

                // Require at least an effective time to proceed
                val eventDate = onsetTime ?: effectiveTime ?: continue // Skip if no valid time

                val weatherType = determineWeatherTypeFromEvent(props.event)

                // Location parsing remains a placeholder - requires a better strategy
                val lat = 40.0 // Placeholder
                val lon = -90.0 // Placeholder
                val (city, state) = parseLocationFromAreaDesc(props.areaDesc)

                val weatherEvent = WeatherEvent(
                    id = props.id ?: UUID.randomUUID().toString(),
                    latitude = lat,
                    longitude = lon,
                    city = city,
                    state = state,
                    country = "USA", // Assumption
                    date = eventDate,
                    type = weatherType,
                    severity = mapSeverity(props.severity),
                    temperature = null,
                    humidity = null,
                    cloudCover = null,
                    windSpeed = null,
                    windDirection = null,
                    pressure = null,
                    visibility = null,
                    hasInversionLayer = props.description.contains("inversion", ignoreCase = true),
                    hasLightRefractionConditions = false, // Hard to determine
                    electricalActivity = if (weatherType == WeatherEvent.WeatherType.THUNDERSTORM || weatherType == WeatherEvent.WeatherType.LIGHTNING) 1 else null,
                    dataSource = "weather.gov API (Alerts)",
                    lastUpdated = now
                )
                events.add(weatherEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping alert feature (ID: ${props.id ?: "unknown"}): ${e.message}", e)
            }
        }
        return events
    }

    // --- Helper functions for mapping ---

    private fun determineWeatherTypeFromEvent(eventString: String?): WeatherEvent.WeatherType {
        if (eventString == null) return WeatherEvent.WeatherType.OTHER
        return when {
            eventString.contains("Tornado", ignoreCase = true) -> WeatherEvent.WeatherType.TORNADO
            eventString.contains("Thunderstorm", ignoreCase = true) -> WeatherEvent.WeatherType.THUNDERSTORM
            eventString.contains("Flood", ignoreCase = true) -> WeatherEvent.WeatherType.HEAVY_RAIN
            eventString.contains("Wind", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER // Needs refinement
            eventString.contains("Fog", ignoreCase = true) -> WeatherEvent.WeatherType.FOG
            eventString.contains("Snow", ignoreCase = true) -> WeatherEvent.WeatherType.SNOW
            eventString.contains("Ice", ignoreCase = true) -> WeatherEvent.WeatherType.HAIL
            eventString.contains("Freeze", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER
            eventString.contains("Heat", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER
            eventString.contains("Fire", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER
            eventString.contains("Hurricane", ignoreCase = true) -> WeatherEvent.WeatherType.HURRICANE
            eventString.contains("Tropical Storm", ignoreCase = true) -> WeatherEvent.WeatherType.HURRICANE // Approximation
            // Add more specific mappings based on https://www.weather.gov/documentation/services-web-api#/default/get_alerts_active
            else -> WeatherEvent.WeatherType.OTHER
        }
    }

    private fun mapSeverity(severityString: String?): Int? {
        return when (severityString?.lowercase()) {
            "extreme" -> 5
            "severe" -> 4
            "moderate" -> 3
            "minor" -> 2
            "unknown" -> 1
            else -> null
        }
    }

    private fun parseLocationFromAreaDesc(areaDesc: String?): Pair<String?, String?> {
        if (areaDesc == null) return Pair(null, null)
        // This remains a basic placeholder. Real implementation needs better parsing
        // based on expected areaDesc formats (e.g., County names, state abbreviations)
        // Example: "McLean; De Witt; Piatt" -> City=null, State=null (maybe default to IL?)
        // Example: "Cook, IL" -> City=Cook, State=IL (simplistic)
        val parts = areaDesc.split(';')
        val firstPart = parts.firstOrNull()?.trim() ?: return Pair(null, null)

        return if (firstPart.contains(",")) {
            val city = firstPart.substringBefore(",").trim()
            val state = firstPart.substringAfter(",").trim().take(2) // Assume 2-char state code
            Pair(city.takeIf { it.isNotEmpty() }, state.takeIf { it.isNotEmpty() })
        } else {
            // Could be a county list or single county/zone name
            Pair(firstPart.takeIf { it.isNotEmpty() }, null) // Or attempt to find state code
        }
    }

    // --- Functions providing data flows ---

    /**
     * Get weather events near a specific location from the database.
     */
    fun getWeatherEventsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<WeatherEvent>> {
        return weatherEventDao.getWeatherEventsNearLocation(latitude, longitude, radiusKm)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting weather events near location from DB: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Get weather events by type from the database.
     */
    fun getWeatherEventsByType(type: WeatherEvent.WeatherType): Flow<List<WeatherEvent>> {
        return weatherEventDao.getWeatherEventsByType(type)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting weather events by type from DB: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Get weather events during a date range from the database.
     */
    fun getWeatherEventsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<WeatherEvent>> {
        return weatherEventDao.getWeatherEventsBetweenDates(startTimestamp, endTimestamp)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting weather events between dates from DB: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Get unusual weather events (those that might correlate with UFO sightings) from the database.
     */
    fun getUnusualWeatherEvents(): Flow<List<WeatherEvent>> {
        return weatherEventDao.getUnusualWeatherEvents()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting unusual weather events from DB: ${e.message}", e)
                emit(emptyList())
            }
    }

    /**
     * Create placeholder weather events for testing or fallback.
     */
    private fun createPlaceholderWeatherEvents(): List<WeatherEvent> {
        val now = System.currentTimeMillis()
        // Using SimpleDateFormat here just for placeholder consistency,
        // but java.time is preferred for actual date logic.
        // val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Reuse the existing placeholder list logic
        return listOf(
            WeatherEvent(
                id = "ph-weather-001", // Prefix IDs to distinguish
                latitude = 40.7128, longitude = -74.0060, city = "New York", state = "NY", country = "USA",
                date = now - (1 * 60 * 60 * 1000), // 1 hour ago
                type = WeatherEvent.WeatherType.THUNDERSTORM, severity = 3, temperature = 22.5, cloudCover = 85,
                electricalActivity = 42, dataSource = "Placeholder Data", lastUpdated = now
            ),
            WeatherEvent(
                id = "ph-weather-002",
                latitude = 34.0522, longitude = -118.2437, city = "Los Angeles", state = "CA", country = "USA",
                date = now - (2 * 60 * 60 * 1000), // 2 hours ago
                type = WeatherEvent.WeatherType.CLEAR_SKY, temperature = 28.3, cloudCover = 5,
                dataSource = "Placeholder Data", lastUpdated = now
            ),
            WeatherEvent(
                id = "ph-weather-003",
                latitude = 41.8781, longitude = -87.6298, city = "Chicago", state = "IL", country = "USA",
                date = now - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                type = WeatherEvent.WeatherType.FOG, temperature = 15.8, cloudCover = 90, visibility = 0.5,
                dataSource = "Placeholder Data", lastUpdated = now
            ),
            WeatherEvent(
                id = "ph-weather-004",
                latitude = 40.4842, longitude = -88.9937, city = "Bloomington", state = "IL", country = "USA",
                date = now - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                type = WeatherEvent.WeatherType.TEMPERATURE_INVERSION, hasInversionLayer = true, hasLightRefractionConditions = true,
                dataSource = "Placeholder Data", lastUpdated = now
            ),
            WeatherEvent(
                id = "ph-weather-005",
                latitude = 37.7749, longitude = -122.4194, city = "San Francisco", state = "CA", country = "USA",
                date = now - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                type = WeatherEvent.WeatherType.FOG, temperature = 15.2, visibility = 0.3, hasLightRefractionConditions = true,
                dataSource = "Placeholder Data", lastUpdated = now
            )
            // Add more diverse placeholder events if needed
        )
    }

    /**
     * Fetch current weather observation from the API for a specific location.
     * Returns Result object containing the WeatherEvent or an Exception.
     */
    suspend fun fetchCurrentWeather(latitude: Double, longitude: Double): Result<WeatherEvent> {
        if (api == null) {
            return Result.failure(Exception("Weather API service is not available"))
        }
        return withContext(Dispatchers.IO) {
            try {
                val pointStr = "$latitude,$longitude"
                Log.d(TAG, "Fetching stations near $pointStr")
                val stationsResponse = api.getStationsNearPoint(pointStr)

                if (stationsResponse.observationStations.isEmpty()) {
                    Log.w(TAG, "No weather stations found near $pointStr")
                    return@withContext Result.failure(Exception("No weather stations found near this location"))
                }

                val stationId = stationsResponse.observationStations.first()
                Log.d(TAG, "Fetching latest observation for station $stationId")
                val observationResponse = api.getLatestObservation(stationId)
                val obsProps = observationResponse.properties

                // Map observation data to WeatherEvent
                val now = System.currentTimeMillis()
                val (city, state) = Pair(null, null) // Need reverse geocoding for this

                val weatherEvent = WeatherEvent(
                    id = UUID.randomUUID().toString(), // Generate unique ID for observation event
                    latitude = latitude,
                    longitude = longitude,
                    city = city, // Requires reverse geocoding
                    state = state, // Requires reverse geocoding
                    country = "USA", // Assuming USA
                    date = now, // Observation time is 'now' relative to fetch
                    type = determineWeatherTypeFromObservation(obsProps.textDescription),
                    temperature = obsProps.temperature.value,
                    humidity = obsProps.relativeHumidity.value,
                    cloudCover = null, // Often inferred from textDescription, not direct value
                    windSpeed = obsProps.windSpeed.value,
                    windDirection = convertWindDirection(obsProps.windDirection.value),
                    pressure = obsProps.barometricPressure.value,
                    visibility = obsProps.visibility.value,
                    hasInversionLayer = obsProps.textDescription.contains("inversion", ignoreCase = true), // Basic check
                    hasLightRefractionConditions = false, // Hard to determine
                    electricalActivity = if (obsProps.textDescription.contains("thunder", ignoreCase = true)) 1 else null,
                    dataSource = "weather.gov API (Observation)",
                    lastUpdated = now
                )

                // Cache the observation result in the database
                // Use insertAll with REPLACE strategy (defined in DAO)
                weatherEventDao.insertAll(listOf(weatherEvent))
                Log.d(TAG, "Successfully fetched and cached current weather for $pointStr")
                Result.success(weatherEvent)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current weather for $latitude,$longitude: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Gets the percentage of sightings that occurred during unusual weather.
     * Accesses the DAO function directly.
     * Returns 0f on error.
     */
    suspend fun getPercentageSightingsDuringUnusualWeather(): Float {
        return try {
            withContext(Dispatchers.IO) {
                weatherEventDao.getPercentageSightingsDuringUnusualWeather() ?: 0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating percentage sightings during unusual weather: ${e.message}", e)
            0f // Return 0% on error
        }
    }


    /**
     * Determine weather type from observation description text.
     * Separate from alert event mapping.
     */
    private fun determineWeatherTypeFromObservation(description: String?): WeatherEvent.WeatherType {
        if (description == null) return WeatherEvent.WeatherType.OTHER
        return when {
            description.contains("Thunder", ignoreCase = true) -> WeatherEvent.WeatherType.THUNDERSTORM
            description.contains("Lightning", ignoreCase = true) -> WeatherEvent.WeatherType.LIGHTNING // Often part of Thunderstorm desc
            description.contains("Fog", ignoreCase = true) || description.contains("Mist", ignoreCase = true) -> WeatherEvent.WeatherType.FOG
            description.contains("Rain", ignoreCase = true) -> WeatherEvent.WeatherType.HEAVY_RAIN // Approximation
            description.contains("Snow", ignoreCase = true) -> WeatherEvent.WeatherType.SNOW
            description.contains("Hail", ignoreCase = true) || description.contains("Pellets", ignoreCase = true)-> WeatherEvent.WeatherType.HAIL
            description.contains("Clear", ignoreCase = true) -> WeatherEvent.WeatherType.CLEAR_SKY
            description.contains("Overcast", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER // Maybe map to CLOUDY?
            description.contains("Cloudy", ignoreCase = true) -> WeatherEvent.WeatherType.OTHER // Maybe map to CLOUDY?
            // Add more specific mappings based on typical observation texts
            else -> WeatherEvent.WeatherType.OTHER
        }
    }

    /**
     * Convert wind direction from degrees to cardinal direction.
     */
    private fun convertWindDirection(degrees: Double?): String? {
        if (degrees == null) return null
        // Ensure degrees are within 0-360 range
        val normalizedDegrees = (degrees % 360 + 360) % 360
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        // Calculate index: add half segment for better rounding, then map
        val index = ((normalizedDegrees + 11.25) / 22.5).toInt() % 16
        return directions[index]
    }
}