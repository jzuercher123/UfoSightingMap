package com.ufomap.ufosightingmap.data.repositories

import android.content.Context
import com.ufomap.ufosightingmap.data.api.OverpassApi
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ufomap.ufosightingmap.data.api.MilitaryBaseApi
import com.ufomap.ufosightingmap.data.correlation.dao.DistanceDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.MilitaryBaseDao
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.utils.SmartCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Repository for military base data.
 * Manages data operations between the data sources (local database, JSON assets, APIs)
 * and the rest of the app.
 */
class MilitaryBaseRepository(
    private val militaryBaseDao: MilitaryBaseDao,
    private val context: Context,
    private val api: MilitaryBaseApi? = null
) {
    private val TAG = "MilitaryBaseRepository"
    private val overpassApi = OverpassApi()

    private val militaryBaseCache = SmartCache(
        maxAgeMillis = 7 * 24 * 60 * 60_000, // Weekly
        fetchData = { /* fetch implementation */ }
    )

    /**
     * Fetch military base data from OpenStreetMap
     * @param boundingBox Optional bounding box to limit search [south,west,north,east]
     * @param replaceExisting Whether to replace existing data or append
     */
    suspend fun fetchMilitaryBasesFromOpenStreetMap(
        boundingBox: String? = null,
        replaceExisting: Boolean = false
    ): Result<Int> {
        return try {
            Log.d(TAG, "Fetching military bases from OpenStreetMap")

            // Fetch data from Overpass API
            val bases = overpassApi.fetchMilitaryBases(boundingBox)

            if (bases.isEmpty()) {
                Log.w(TAG, "No military bases found from OpenStreetMap")
                return Result.failure(Exception("No military bases found"))
            }

            Log.d(TAG, "Fetched ${bases.size} military bases from OpenStreetMap")

            // Clear existing data if requested
            if (replaceExisting) {
                Log.d(TAG, "Clearing existing military base data")
                militaryBaseDao.deleteAll()
            }

            // Insert the new data
            militaryBaseDao.insertAll(bases)
            Log.d(TAG, "Successfully stored military bases from OpenStreetMap")

            Result.success(bases.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching military bases from OpenStreetMap: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch US military bases with specific bounding box for US territory
     */
    suspend fun fetchUSMilitaryBases(replaceExisting: Boolean = false): Result<Int> {
        // US mainland bounding box: roughly the continental US
        val usBoundingBox = "24.52, -124.77, 49.38, -66.95"
        return fetchMilitaryBasesFromOpenStreetMap(usBoundingBox, replaceExisting)
    }

    // Expose all military bases as a Flow
    val allBases: Flow<List<MilitaryBase>> = militaryBaseDao.getAllBases()
        .flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "Error getting military bases: ${e.message}", e)
        }

    // Expose sightings with nearest base data
    val sightingsWithBaseDistance: Flow<List<SightingWithBaseDistance>> =
        militaryBaseDao.getSightingsWithNearestBase()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting sightings with base distance: ${e.message}", e)
            }

    // Expose distance distribution statistics
    val sightingDistanceDistribution: Flow<List<DistanceDistribution>> =
        militaryBaseDao.getSightingCountsByDistanceBand()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting distance distribution: ${e.message}", e)
            }

    /**
     * Initialize the military base database with data from the JSON asset file.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val count = militaryBaseDao.count()
                if (count == 0) {
                    Log.d(TAG, "Military base database is empty. Loading from JSON asset.")
                    loadMilitaryBasesFromJson()
                } else {
                    Log.d(TAG, "Military base database already contains $count bases.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking military base database state", e)
            }
        }
    }

    /**
     * Force reload military base data (useful for testing or when assets are updated)
     */
    suspend fun forceReloadData() {
        try {
            Log.d(TAG, "Forcing military base database clear and reload")
            militaryBaseDao.deleteAll()
            loadMilitaryBasesFromJson()
        } catch (e: Exception) {
            Log.e(TAG, "Error during military base force reload: ${e.message}", e)
        }
    }

    /**
     * Load military base data from the API if available, else fall back to local JSON file
     */
    private suspend fun loadMilitaryBasesFromJson() {
        try {
            // Try to fetch from API first if configured
            if (api != null) {
                try {
                    val apiData = api.getMilitaryBases()
                    if (apiData.isNotEmpty()) {
                        Log.d(TAG, "Successfully fetched ${apiData.size} military bases from API")
                        militaryBaseDao.insertAll(apiData)
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load military bases from API: ${e.message}")
                    // Continue to load from local asset
                }
            }

            // Fall back to local JSON asset
            Log.d(TAG, "Loading military base data from local JSON asset")
            val jsonString = context.assets.open("military_bases.json").bufferedReader().use { it.readText() }
            Log.d(TAG, "Successfully read military_bases.json file")

            val baseType = object : TypeToken<List<MilitaryBase>>() {}.type
            val bases = Gson().fromJson<List<MilitaryBase>>(jsonString, baseType)

            Log.d(TAG, "Parsed ${bases.size} military bases from JSON")
            militaryBaseDao.insertAll(bases)
            Log.d(TAG, "Successfully loaded military bases from JSON")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading military_bases.json: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing military base data: ${e.message}", e)
        }
    }

    /**
     * Get bases within specific geographical bounds
     */
    fun getBasesInBounds(north: Double, south: Double, east: Double, west: Double): Flow<List<MilitaryBase>> {
        return militaryBaseDao.getBasesInBounds(north, south, east, west)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting bases in bounds: ${e.message}", e)
            }
    }

    /**
     * Get bases near a specific coordinate point
     */
    fun getBasesNearPoint(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<MilitaryBase>> {
        return militaryBaseDao.getBasesNearPoint(latitude, longitude, radiusKm)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "Error getting bases near point: ${e.message}", e)
            }
    }

    /**
     * Get the closest military base to a specific location
     */
    suspend fun getClosestBase(latitude: Double, longitude: Double): MilitaryBase? {
        return try {
            militaryBaseDao.getClosestBase(latitude, longitude)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting closest base: ${e.message}", e)
            null
        }
    }

    /**
     * Get all sightings that occurred within a specific radius of any military base
     */
    fun getSightingsNearMilitaryBases(radiusKm: Double) = militaryBaseDao.getSightingsNearAnyBase(radiusKm)
        .flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "Error getting sightings near bases: ${e.message}", e)
        }

    /**
     * Get the percentage of all sightings that occurred near military bases
     */
    suspend fun getPercentageSightingsNearBases(radiusKm: Double): Float {
        return try {
            militaryBaseDao.getPercentageSightingsWithinRadius(radiusKm)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating percentage: ${e.message}", e)
            0f
        }
    }

    /**
     * Get count of sightings within a specific radius of any military base
     */
    suspend fun countSightingsWithinRadius(radiusKm: Double): Int {
        return try {
            militaryBaseDao.countSightingsWithinRadius(radiusKm)
        } catch (e: Exception) {
            Log.e(TAG, "Error counting sightings: ${e.message}", e)
            0
        }
    }
}