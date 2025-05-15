package com.ufomap.ufosightingmap.data.repositories


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDensityDistribution
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.utils.SmartCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

/**
 * Repository for population data.
 * Manages data operations between the data sources (local database, JSON assets)
 * and the rest of the app.
 */
class PopulationDataRepository(
    private val populationDataDao: PopulationDataDao,
    private val context: Context
) {
    private val TAG = "PopulationDataRepo"

    private val populationCache = SmartCache(
        maxAgeMillis = 24 * 60 * 60_000, // Daily
        fetchData = { /* fetch implementation */ }
    )

    // Expose all population data as a Flow
    val allPopulationData: Flow<List<PopulationData>> = populationDataDao.getAllPopulationData()
        .flowOn(Dispatchers.IO)
        .catch { e ->
            Timber.e(e, "Error getting population data: ${e.message}")
            emit(emptyList()) // Emit empty list on error
        }

    // Population density distribution for visualization
    val populationDensityDistribution: Flow<List<PopulationDensityDistribution>> =
        populationDataDao.getSightingsByPopulationDensity(getCurrentYear())
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e(e, "Error getting population density distribution: ${e.message}")
                emit(emptyList()) // Emit empty list on error
            }

    /**
     * Initialize the population database with data if needed.
     */
    fun initializeDatabaseIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val count = populationDataDao.count()
                if (count == 0) {
                    Log.d(TAG, "Population database is empty. Loading from JSON asset.")
                    loadPopulationDataFromJson()
                } else {
                    Log.d(TAG, "Population database already contains $count records.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking population database state")
            }
        }
    }

    /**
     * Force reload population data (useful for testing or when assets are updated)
     */
    suspend fun forceReloadData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Forcing population database clear and reload")
                populationDataDao.deleteAll()
                loadPopulationDataFromJson()
            } catch (e: Exception) {
                Timber.e(e, "Error during population data force reload: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Load population data from JSON asset file
     */
    private suspend fun loadPopulationDataFromJson() {
        try {
            // Attempt to read from population_data.json
            val jsonFileName = "population_data.json"

            try {
                context.assets.open(jsonFileName).use { inputStream ->
                    val size = inputStream.available()
                    Log.d(TAG, "$jsonFileName size: $size bytes")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to find $jsonFileName in assets", e)
                // Fall back to creating sample data
                createSamplePopulationData()
                return
            }

            val jsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
            Log.d(TAG, "Successfully read $jsonFileName file")

            val dataType = object : TypeToken<List<PopulationData>>() {}.type
            val populationData = Gson().fromJson<List<PopulationData>>(jsonString, dataType)

            if (populationData.isNotEmpty()) {
                Log.d(TAG, "Parsed ${populationData.size} population data records from JSON")
                populationDataDao.insertAll(populationData)
                Log.d(TAG, "Successfully loaded population data from JSON")
            } else {
                Log.w(TAG, "Parsed JSON but found no population data")
                createSamplePopulationData()
            }
        } catch (e: IOException) {
            Timber.e(e, "Error reading population_data.json: ${e.message}")
            createSamplePopulationData()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing population data: ${e.message}")
            createSamplePopulationData()
        }
    }

    /**
     * Create sample population data for testing when no JSON file is available
     */
    private suspend fun createSamplePopulationData() {
        Log.d(TAG, "Creating sample population data")

        val sampleData = listOf(
            PopulationData(
                id = "us-il-cook-2023",
                fipsCode = "17031",
                countyName = "Cook County",
                stateName = "Illinois",
                stateAbbreviation = "IL",
                country = "USA",
                latitude = 41.8781,
                longitude = -87.6298,
                year = 2023,
                population = 5150233,
                landAreaSqKm = 2448.0,
                populationDensity = 2104.3,
                urbanPercentage = 98.5,
                medianAge = 37.2,
                educationHighSchoolPercentage = 87.3,
                educationBachelorsPercentage = 39.8,
                dataSource = "Sample Data",
                lastUpdated = System.currentTimeMillis()
            ),
            PopulationData(
                id = "us-ca-los-angeles-2023",
                fipsCode = "06037",
                countyName = "Los Angeles County",
                stateName = "California",
                stateAbbreviation = "CA",
                country = "USA",
                latitude = 34.0522,
                longitude = -118.2437,
                year = 2023,
                population = 9829544,
                landAreaSqKm = 10570.0,
                populationDensity = 930.0,
                urbanPercentage = 99.2,
                medianAge = 36.7,
                educationHighSchoolPercentage = 78.5,
                educationBachelorsPercentage = 33.5,
                dataSource = "Sample Data",
                lastUpdated = System.currentTimeMillis()
            ),
            PopulationData(
                id = "us-ny-new-york-2023",
                fipsCode = "36061",
                countyName = "New York County",
                stateName = "New York",
                stateAbbreviation = "NY",
                country = "USA",
                latitude = 40.7128,
                longitude = -74.0060,
                year = 2023,
                population = 1628706,
                landAreaSqKm = 59.0,
                populationDensity = 27606.0,
                urbanPercentage = 100.0,
                medianAge = 37.5,
                educationHighSchoolPercentage = 87.0,
                educationBachelorsPercentage = 61.8,
                dataSource = "Sample Data",
                lastUpdated = System.currentTimeMillis()
            ),
            PopulationData(
                id = "us-nv-clark-2023",
                fipsCode = "32003",
                countyName = "Clark County",
                stateName = "Nevada",
                stateAbbreviation = "NV",
                country = "USA",
                latitude = 36.1699,
                longitude = -115.1398,
                year = 2023,
                population = 2265461,
                landAreaSqKm = 20817.0,
                populationDensity = 108.8,
                urbanPercentage = 94.5,
                medianAge = 38.1,
                educationHighSchoolPercentage = 84.7,
                educationBachelorsPercentage = 24.9,
                dataSource = "Sample Data",
                lastUpdated = System.currentTimeMillis()
            ),
            PopulationData(
                id = "us-nm-roswell-2023",
                fipsCode = "35005",
                countyName = "Chaves County",
                stateName = "New Mexico",
                stateAbbreviation = "NM",
                country = "USA",
                latitude = 33.3943,
                longitude = -104.5230,
                year = 2023,
                population = 64615,
                landAreaSqKm = 15729.0,
                populationDensity = 4.1,
                urbanPercentage = 74.2,
                medianAge = 36.3,
                educationHighSchoolPercentage = 77.9,
                educationBachelorsPercentage = 16.2,
                dataSource = "Sample Data",
                lastUpdated = System.currentTimeMillis()
            )
        )

        populationDataDao.insertAll(sampleData)
        Log.d(TAG, "Inserted ${sampleData.size} sample population data records")
    }

    /**
     * Get current year (or previous year to ensure data exists)
     */
    private fun getCurrentYear(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1 // Use previous year for data stability
    }

    /**
     * Get average population density for areas with UFO sightings
     */
    suspend fun getAveragePopulationDensityForSightings(): Float {
        return try {
            withContext(Dispatchers.IO) {
                populationDataDao.getAveragePopulationDensityForSightings(getCurrentYear())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating average population density: ${e.message}")
            0f // Return 0 on error
        }
    }

    /**
     * Get population data for a specific state
     */
    fun getPopulationDataByState(state: String): Flow<List<PopulationData>> {
        return populationDataDao.getPopulationDataByState(state)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e(e, "Error getting population data for state $state: ${e.message}")
                emit(emptyList())
            }
    }

    /**
     * Get population data for a specific year
     */
    fun getPopulationDataByYear(year: Int): Flow<List<PopulationData>> {
        return populationDataDao.getPopulationDataByYear(year)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e(e, "Error getting population data for year $year: ${e.message}")
                emit(emptyList())
            }
    }
}