package com.ufomap.ufosightingmap.data // Corrected package name

import android.content.Context
import android.util.Log
import com.ufomap.ufosightingmap.utils.loadSightingsFromJson // Correct import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SightingRepository(private val sightingDao: SightingDao, private val context: Context) {

    val allSightings: Flow<List<Sighting>> = sightingDao.getAllSightings()

    suspend fun refreshSightings() {
        // TODO: Implement network fetching logic if needed
    }

    fun getSightingsInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<Sighting>> {
        return sightingDao.getSightingsInBounds(north, south, east, west)
    }

    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            if (sightingDao.count() == 0) {
                Log.d("SightingRepository", "Database is empty. Loading from JSON asset.")
                try {
                    // Call the correctly imported function
                    val sightingsList: List<Sighting>? = loadSightingsFromJson(context, "sightings.json")
                    // Check if the list is not null before using it
                    if (sightingsList != null) {
                        sightingDao.insertAll(sightingsList) // Pass the correctly typed list
                        Log.d("SightingRepository", "Successfully loaded ${sightingsList.size} sightings from JSON.") // Use size on the list
                    } else {
                        Log.e("SightingRepository", "Failed to load sightings from JSON (returned null).")
                    }
                } catch (e: Exception) {
                    Log.e("SightingRepository", "Error loading sightings from JSON", e)
                }
            } else {
                Log.d("SightingRepository", "Database already populated.")
            }
        }
    }
}
