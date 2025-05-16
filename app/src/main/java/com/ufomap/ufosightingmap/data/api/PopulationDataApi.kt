package com.ufomap.ufosightingmap.data.api

import android.util.Log
import com.ufomap.ufosightingmap.BuildConfig
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val TAG = "PopulationDataApi"

/**
 * Interface defining the Census API endpoints for population data.
 * Used for correlation analysis between UFO sightings and population demographics.
 */
interface CensusApiService {
    /**
     * Get state-level population data
     *
     * @param year Year for which to fetch population data
     * @param variables Comma-separated list of variables to retrieve
     * @param region Region to filter data for
     * @param apiKey API key for accessing the Census API
     *
     * Returns a [Response] containing a list of lists of strings representing the population data.
     */
    @GET("{year}/pep/population")
    suspend fun getStatePopulation(
        @Path("year") year: String,
        @Query("get") variables: String = "POP,NAME,DENSITY",
        @Query("for") region: String = "state:*",
        @Query("key") apiKey: String
    ): Response<List<List<String>>>

    /**
     * Get county-level population data for more granular analysis
     *
     * @param year Year for which to fetch population data
     * @param variables Comma-separated list of variables to retrieve
     * @param region Region to filter data for
     * @param apiKey API key for accessing the Census API
     * @param stateFilter Optional filter for specific state
     *
     * Returns a [Response] containing a list of lists of strings representing the population data.
     */
    @GET("{year}/pep/population")
    suspend fun getCountyPopulation(
        @Path("year") year: String,
        @Query("get") variables: String = "POP,NAME,DENSITY,HISP,RACE,AGE,SEX",
        @Query("for") region: String = "county:*",
        @Query("in") stateFilter: String? = null,
        @Query("key") apiKey: String
    ): Response<List<List<String>>>

    /**
     * Get educational attainment data - useful for correlation analysis
     *
     * @param year Year for which to fetch education data
     * @param variables Comma-separated list of variables to retrieve
     * @param region Region to filter data for
     * @param apiKey API key for accessing the Census API
     *
     * Returns a [Response] containing a list of lists of strings representing the education data.
     */
    @GET("{year}/acs/acs5/subject")
    suspend fun getEducationData(
        @Path("year") year: String,
        @Query("get") variables: String = "NAME,S1501_C01_006E,S1501_C01_007E",
        @Query("for") region: String,
        @Query("key") apiKey: String
    ): Response<List<List<String>>>
}

/**
 * Main API client for Census population data, designed for the UFO sighting correlation analysis
 */
class PopulationDataApi(private val apiKey: String) {
    private companion object {
        const val BASE_URL = "https://api.census.gov/data/"
        const val TIMEOUT_SECONDS = 30L
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val apiService: CensusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CensusApiService::class.java)
    }

    /**
     * Fetch population data and map to application models for correlation analysis
     */
    suspend fun getPopulationData(year: Int = 2023): Result<List<PopulationData>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching population data for year $year")
            val response = apiService.getStatePopulation(
                year = year.toString(),
                apiKey = apiKey
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                return@withContext Result.failure(Exception("Census API returned ${response.code()}: ${response.message()}"))
            }

            val data = response.body()
            if (data.isNullOrEmpty() || data.size < 2) {
                Log.e(TAG, "Received empty or malformed data from Census API")
                return@withContext Result.failure(Exception("Received invalid data from Census API"))
            }

            // Process data: First row contains headers
            val headers = data[0]
            val popIndex = headers.indexOf("POP")
            val nameIndex = headers.indexOf("NAME")
            val densityIndex = headers.indexOf("DENSITY")
            val stateIndex = headers.indexOf("state")

            // Map to application model
            val populationData = data.subList(1, data.size).mapNotNull { row ->
                try {
                    // Extract data from row
                    val stateName = row[nameIndex]
                    val population = row[popIndex].toIntOrNull() ?: 0
                    val density = row[densityIndex].toDoubleOrNull() ?: 0.0
                    val stateCode = row[stateIndex]

                    // Create PopulationData object
                    PopulationData(
                        id = "census-$year-$stateCode",
                        fipsCode = stateCode,
                        countyName = "", // Not applicable for state level
                        stateName = stateName,
                        stateAbbreviation = getStateAbbreviation(stateName),
                        country = "USA",
                        latitude = 0.0, // Would need geocoding service to get precise coordinates
                        longitude = 0.0, // Would need geocoding service to get precise coordinates
                        year = year,
                        population = population,
                        landAreaSqKm = if (density > 0) population / density else 0.0,
                        populationDensity = density,
                        urbanPercentage = null, // Would need additional API call
                        dataSource = "U.S. Census Bureau API",
                        lastUpdated = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing row $row: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Successfully processed ${populationData.size} population records")
            Result.success(populationData)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching population data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch county-level data for more detailed correlation analysis
     */
    suspend fun getCountyPopulationData(
        year: Int = 2023,
        state: String? = null
    ): Result<List<PopulationData>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching county population data for year $year, state: $state")

            val stateFilter = state?.let { "state:$it" }
            val response = apiService.getCountyPopulation(
                year = year.toString(),
                stateFilter = stateFilter,
                apiKey = apiKey
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Census API returned ${response.code()}: ${response.message()}"))
            }

            // Process and return county data
            // Implementation similar to state processing but with county specifics

            Result.success(emptyList()) // Replace with actual processing
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching county population data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function to get state abbreviation from full name
     */
    private fun getStateAbbreviation(stateName: String): String {
        // Map of state names to abbreviations
        val stateMap = mapOf(
            "Alabama" to "AL", "Alaska" to "AK", "Arizona" to "AZ",
            /* Add all states here */
            "Wyoming" to "WY"
        )

        return stateMap[stateName] ?: stateName.take(2).uppercase()
    }
}

/**
 * Factory to create PopulationDataApi instances
 */
object PopulationDataApiFactory {
    private var instance: PopulationDataApi? = null

    fun getInstance(apiKey: String): PopulationDataApi {
        return instance ?: PopulationDataApi(apiKey).also { instance = it }
    }
}