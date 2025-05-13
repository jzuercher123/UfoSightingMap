package com.ufomap.ufosightingmap.data

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ufomap.ufosightingmap.utils.loadSightingsFromJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Repository for UFO Sighting data.
 * Manages interactions between the data sources (local database, JSON assets) and the rest of the app.
 */
class SightingRepository(private val sightingDao: SightingDao, private val context: Context) {

    private val TAG = "SightingRepository"

    // Track loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading

    // Track error state
    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error

    // Expose all sightings as a Flow for reactive UI updates with error handling
    val allSightings: Flow<List<Sighting>> = sightingDao.getAllSightings()
        .onEach { list ->
            Log.d(TAG, "Flow emitted ${list.size} sightings")
        }
        .catch { e ->
            Log.e(TAG, "Error in allSightings flow: ${e.message}", e)
            _error.value = "Failed to load sightings: ${e.message}"
            emit(emptyList())
        }

    // Provide paginated sightings for improved performance with large datasets
    fun getSightingsPaged(pageSize: Int = 20): Flow<PagingData<Sighting>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            )
        ) {
            sightingDao.getSightingsPaged()
        }.flow
    }

    /**
     * Get sightings within a geographical boundary
     */
    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>> {
        return sightingDao.getSightingsInBounds(north, south, east, west)
            .catch { e ->
                Log.e(TAG, "Error getting sightings in bounds: ${e.message}", e)
                _error.value = "Failed to get sightings in map bounds: ${e.message}"
                emit(emptyList())
            }
    }

    /**
     * Initialize the database with sightings from the JSON asset file if the database is empty.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val count = sightingDao.count()
                if (count == 0) {
                    Log.d(TAG, "Database is empty. Loading from JSON asset.")
                    loadSightingsFromJsonAndInsert()
                } else {
                    Log.d(TAG, "Database already contains $count sightings.")
                }
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database state", e)
                _error.value = "Failed to initialize database: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Force clear the database and reload data from JSON
     */
    suspend fun forceReloadData() {
        try {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "Forcing database clear and reload")
            // Clear existing data
            sightingDao.clearAllSightings()
            // Load JSON data
            loadSightingsFromJsonAndInsert()
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during force reload: ${e.message}", e)
            _error.value = "Failed to reload data: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Debug function to check if we can access asset files
     */
    // In SightingRepository.kt
    fun debugAssetFiles() {
        try {
            val files = context.assets.list("")
            Log.d(TAG, "Asset files: ${files?.joinToString()}")

            if (files?.contains("sightings.json") == true) {
                val jsonSize = context.assets.open("sightings.json").available()
                Log.d(TAG, "sightings.json file size: $jsonSize bytes")

                // Read the first 100 chars to verify content
                val start = context.assets.open("sightings.json").bufferedReader().use {
                    it.readText().take(100)
                }
                Log.d(TAG, "JSON starts with: $start...")

                // Check Room database state
                viewModelScope.launch(Dispatchers.IO) {
                    val count = sightingDao.count()
                    Log.d(TAG, "Current database contains $count sightings")
                }
            } else {
                Log.e(TAG, "sightings.json NOT FOUND in assets")
                _error.value = "sightings.json not found in assets"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking assets: ${e.message}", e)
            _error.value = "Failed to check asset files: ${e.message}"
        }
    }

    /**
     * Gets a raw count of sightings in the database.
     * Unlike flow-based methods, this returns a direct count for immediate use.
     */
    suspend fun getRawCount(): Int {
        return sightingDao.count()
    }


    /**
     * Load sightings from the JSON asset file and insert them into the database.
     * Called during initialization if the database is empty.
     */
    private suspend fun loadSightingsFromJsonAndInsert() {
        try {
            Log.d(TAG, "Attempting to load sightings from JSON")
            val sightingsList = loadSightingsFromJson(context, "sightings.json")
            Log.d(TAG, "JSON parsing result: ${sightingsList?.size ?: "null"} sightings")

            if (sightingsList != null && sightingsList.isNotEmpty()) {
                Log.d(TAG, "Inserting ${sightingsList.size} sightings into database")
                sightingDao.insertAll(sightingsList)
                Log.d(TAG, "Successfully loaded sightings from JSON.")
            } else {
                Log.e(TAG, "Failed to load sightings - data was null or empty")
                _error.value = "Failed to parse sightings data from JSON"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading JSON file: ${e.message}", e)
            _error.value = "Failed to read sightings.json: ${e.message}"
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sightings from JSON: ${e.message}", e)
            _error.value = "Failed to load sightings: ${e.message}"
        }
    }

    /**
     * Refresh sightings from a network source
     * TODO: Implement when adding network functionality
     */
    suspend fun refreshSightings() {
        // Future implementation for network data fetching
    }

    /**
     * Get filtered sightings based on search criteria
     */
    fun getFilteredSightings(
        shape: String? = null,
        city: String? = null,
        country: String? = null,
        state: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        searchText: String? = null
    ): Flow<List<Sighting>> {
        return sightingDao.getFilteredSightings(
            shape, city, country, state, startDate, endDate, searchText
        ).catch { e ->
            Log.e(TAG, "Error getting filtered sightings: ${e.message}", e)
            _error.value = "Failed to filter sightings: ${e.message}"
            emit(emptyList())
        }
    }

    suspend fun addUserSighting(sighting: Sighting): Result<Long> {
        return try {
            val id = sightingDao.insertSighting(sighting)
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user sighting: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user's submissions with error handling
    fun getUserSubmissions(submittedBy: String): Flow<List<Sighting>> {
        return sightingDao.getUserSubmissions(submittedBy)
            .catch { e ->
                Log.e(TAG, "Error getting user submissions: ${e.message}", e)
                _error.value = "Failed to load your submissions: ${e.message}"
                emit(emptyList())
            }
    }

    // Clear error status
    fun clearError() {
        _error.value = null
    }
}