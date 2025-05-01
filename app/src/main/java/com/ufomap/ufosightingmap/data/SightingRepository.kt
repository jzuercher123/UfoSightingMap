package com.ufomap.ufosightingmap.data

import android.content.Context
import android.util.Log
import com.ufomap.ufosightingmap.utils.loadSightingsFromJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Repository for UFO Sighting data.
 * Manages interactions between the data sources (local database, JSON assets) and the rest of the app.
 */
class SightingRepository(private val sightingDao: SightingDao, private val context: Context) {

    // Expose all sightings as a Flow for reactive UI updates
    val allSightings: Flow<List<Sighting>> = sightingDao.getAllSightings().onEach { list ->
        Log.d("SightingRepository", "Flow emitted ${list.size} sightings")
    }

    /**
     * Get sightings within a geographical boundary
     */
    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>> {
        return sightingDao.getSightingsInBounds(north, south, east, west)
    }

    /**
     * Initialize the database with sightings from the JSON asset file if the database is empty.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val count = sightingDao.count()
                if (count == 0) {
                    Log.d("SightingRepository", "Database is empty. Loading from JSON asset.")
                    loadSightingsFromJsonAndInsert()
                } else {
                    Log.d("SightingRepository", "Database already contains $count sightings.")
                }
            } catch (e: Exception) {
                Log.e("SightingRepository", "Error checking database state", e)
            }
        }
    }

    /**
     * Force clear the database and reload data from JSON
     */
    suspend fun forceReloadData() {
        try {
            Log.d("SightingRepository", "Forcing database clear and reload")
            // Clear existing data
            sightingDao.clearAllSightings()
            // Load JSON data
            loadSightingsFromJsonAndInsert()
        } catch (e: Exception) {
            Log.e("SightingRepository", "Error during force reload: ${e.message}", e)
        }
    }

    /**
     * Debug function to check if we can access asset files
     */
    fun debugAssetFiles() {
        try {
            val files = context.assets.list("")
            Log.d("SightingRepository", "Asset files: ${files?.joinToString()}")

            if (files?.contains("sightings.json") == true) {
                val jsonSize = context.assets.open("sightings.json").available()
                Log.d("SightingRepository", "sightings.json file size: $jsonSize bytes")

                // Read the first 100 chars to verify content
                val start = context.assets.open("sightings.json").bufferedReader().use {
                    it.readText().take(100)
                }
                Log.d("SightingRepository", "JSON starts with: $start...")
            } else {
                Log.e("SightingRepository", "sightings.json NOT FOUND in assets")
            }
        } catch (e: Exception) {
            Log.e("SightingRepository", "Error checking assets: ${e.message}", e)
        }
    }

    /**
     * Load sightings from the JSON asset file and insert them into the database.
     * Called during initialization if the database is empty.
     */
    private suspend fun loadSightingsFromJsonAndInsert() {
        try {
            Log.d("SightingRepository", "Attempting to load sightings from JSON")
            val sightingsList = loadSightingsFromJson(context, "sightings.json")
            Log.d("SightingRepository", "JSON parsing result: ${sightingsList?.size ?: "null"} sightings")

            if (sightingsList != null && sightingsList.isNotEmpty()) {
                Log.d("SightingRepository", "Inserting ${sightingsList.size} sightings into database")
                sightingDao.insertAll(sightingsList)
                Log.d("SightingRepository", "Successfully loaded sightings from JSON.")
            } else {
                Log.e("SightingRepository", "Failed to load sightings - data was null or empty")
            }
        } catch (e: Exception) {
            Log.e("SightingRepository", "Error loading sightings from JSON: ${e.message}", e)
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
        )
    }

    suspend fun addUserSighting(sighting: Sighting): Long {
        return sightingDao.insertSighting(sighting)
    }

    // Get user's submissions
    fun getUserSubmissions(submittedBy: String): Flow<List<Sighting>> {
        return sightingDao.getUserSubmissions(submittedBy)
    }
}