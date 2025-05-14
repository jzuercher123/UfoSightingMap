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
import kotlinx.coroutines.withContext
import java.io.IOException
import timber.log.Timber // It's good practice to use Timber for logging


/**
 * Repository for UFO Sighting data.
 * Manages interactions between the data sources (local database, JSON assets) and the rest of the app.
 */
class SightingRepository(private val sightingDao: SightingDao, private val context: Context) {

    // Using Timber for logging consistently
    // private val TAG = "SightingRepository" // Replaced by Timber's automatic tag

    // Track loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading

    // Track error state
    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error

    // Expose all sightings as a Flow for reactive UI updates with error handling
    val allSightings: Flow<List<Sighting>> = sightingDao.getAllSightings()
        .onEach { list ->
            Timber.d("Flow emitted ${list.size} sightings")
        }
        .catch { e ->
            Timber.e(e, "Error in allSightings flow")
            _error.value = "Failed to load sightings: ${e.message}"
            emit(emptyList()) // Emit empty list on error to prevent crash
        }

    // Provide paginated sightings for improved performance with large datasets
    fun getSightingsPaged(pageSize: Int = 20): Flow<PagingData<Sighting>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false, // Typically false for network/db sources
                maxSize = pageSize * 3 // Example: Keep 3 pages in memory
            ),
            pagingSourceFactory = { sightingDao.getSightingsPaged() } // Corrected lambda usage
        ).flow
    }

    /**
     * Get sightings within a geographical boundary
     */
    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>> {
        return sightingDao.getSightingsInBounds(north, south, east, west)
            .catch { e ->
                Timber.e(e, "Error getting sightings in bounds")
                _error.value = "Failed to get sightings in map bounds: ${e.message}"
                emit(emptyList())
            }
    }

    suspend fun getRawCount(): Int = withContext(Dispatchers.IO) {
        return@withContext sightingDao.getRawCount()
    }


    /**
     * Initialize the database with sightings from the JSON asset file if the database is empty.
     * This function is designed to be called from a CoroutineScope.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { // Ensure this runs on a background thread
            try {
                _isLoading.value = true
                val count = sightingDao.count()
                if (count == 0) {
                    Timber.d("Database is empty. Loading from JSON asset.")
                    loadSightingsFromJsonAndInsert()
                } else {
                    Timber.d("Database already contains $count sightings.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking database state or initializing")
                _error.value = "Failed to initialize database: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Force clear the database and reload data from JSON.
     * This is a suspend function, so it should be called from a coroutine.
     */
    suspend fun forceReloadData() {
        // Ensure this is called on a background dispatcher if the caller isn't already on one
        // withContext(Dispatchers.IO) { ... } // Can be added if needed
        try {
            _isLoading.value = true
            _error.value = null // Clear previous errors
            Timber.d("Forcing database clear and reload")
            sightingDao.clearAllSightings()
            loadSightingsFromJsonAndInsert() // This is already suspend and uses IO internally if needed
        } catch (e: Exception) {
            Timber.e(e, "Error during force reload")
            _error.value = "Failed to reload data: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Debug function to check if we can access asset files.
     * This is a synchronous function.
     */
    fun debugAssetFiles() {
        try {
            val files = context.assets.list("")
            Timber.d("Asset files: ${files?.joinToString()}")

            if (files?.contains("sightings.json") == true) {
                context.assets.open("sightings.json").use { inputStream ->
                    val jsonSize = inputStream.available()
                    Timber.d("sightings.json file size: $jsonSize bytes")

                    // Read the first 100 chars to verify content
                    val start = inputStream.bufferedReader().use {
                        it.readText().take(100)
                    }
                    Timber.d("JSON starts with: $start...")
                }
            } else {
                Timber.e("sightings.json NOT FOUND in assets")
                _error.value = "sightings.json not found in assets"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking assets")
            _error.value = "Failed to check asset files: ${e.message}"
        }
    }

    /**
     * Load sightings from the JSON asset file and insert them into the database.
     * This is a suspend function, designed to be called from a background coroutine.
     */
    private suspend fun loadSightingsFromJsonAndInsert() {
        try {
            Timber.d("Attempting to load sightings from JSON")
            // loadSightingsFromJson is assumed to be a suspend function or handle its own threading
            val sightingsList = loadSightingsFromJson(context, "sightings.json")
            Timber.d("JSON parsing result: ${sightingsList?.size ?: "null"} sightings")

            if (!sightingsList.isNullOrEmpty()) {
                Timber.d("Inserting ${sightingsList.size} sightings into database")
                sightingDao.insertAll(sightingsList) // This should be a suspend function or run on Dispatchers.IO
                Timber.d("Successfully loaded sightings from JSON.")
            } else {
                Timber.e("Failed to load sightings - data was null or empty")
                _error.value = "Failed to parse sightings data from JSON"
            }
        } catch (e: IOException) {
            Timber.e(e, "Error reading JSON file")
            _error.value = "Failed to read sightings.json: ${e.message}"
        } catch (e: Exception) { // Catch more generic exceptions
            Timber.e(e, "Error loading sightings from JSON")
            _error.value = "Failed to load sightings: ${e.message}"
        }
    }

    /**
     * Refresh sightings from a network source.
     * Placeholder for future implementation.
     */
    suspend fun refreshSightings() {
        _isLoading.value = true
        // Simulate network delay
        // kotlinx.coroutines.delay(2000)
        // TODO: Implement actual network data fetching logic here.
        // For example:
        // try {
        //     val networkSightings = apiClient.fetchLatestSightings()
        //     sightingDao.clearAndInsert(networkSightings) // Example DAO operation
        //     _error.value = null
        // } catch (e: Exception) {
        //     Timber.e(e, "Failed to refresh sightings from network")
        //     _error.value = "Network refresh failed: ${e.message}"
        // } finally {
        //     _isLoading.value = false
        // }
        Timber.d("refreshSightings called (not implemented yet).")
        _isLoading.value = false // Remove this if you implement actual async work
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
            shape?.takeIf { it.isNotBlank() }, // Pass null if blank
            city?.takeIf { it.isNotBlank() },
            country?.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() },
            startDate?.takeIf { it.isNotBlank() },
            endDate?.takeIf { it.isNotBlank() },
            searchText?.takeIf { it.isNotBlank() }
        ).catch { e ->
            Timber.e(e, "Error getting filtered sightings")
            _error.value = "Failed to filter sightings: ${e.message}"
            emit(emptyList())
        }
    }

    suspend fun addUserSighting(sighting: Sighting): Result<Long> {
        return try {
            // Ensure this runs on a background thread if not already
            // withContext(Dispatchers.IO) {
            val id = sightingDao.insertSighting(sighting)
            // }
            if (id > 0) {
                Timber.d("User sighting added with ID: $id")
                Result.success(id)
            } else {
                Timber.e("Failed to insert user sighting, DAO returned ID: $id")
                Result.failure(Exception("Failed to insert sighting, invalid ID returned."))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding user sighting")
            Result.failure(e)
        }
    }

    fun getUserSubmissions(submittedBy: String): Flow<List<Sighting>> {
        return sightingDao.getUserSubmissions(submittedBy)
            .catch { e ->
                Timber.e(e, "Error getting user submissions for $submittedBy")
                _error.value = "Failed to load your submissions: ${e.message}"
                emit(emptyList())
            }
    }

    fun clearError() {
        _error.value = null
    }
}
