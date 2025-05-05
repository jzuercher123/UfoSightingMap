package com.ufomap.ufosightingmap.data.correlation.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for population data.
 * Provides methods to query population data and correlate with sightings.
 */
@Dao
interface PopulationDataDao {

    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(populationData: List<PopulationData>)

    @Query("SELECT * FROM population_data")
    fun getAllPopulationData(): Flow<List<PopulationData>>

    @Query("SELECT COUNT(*) FROM population_data")
    suspend fun count(): Int

    @Query("DELETE FROM population_data")
    suspend fun deleteAll()

    // Filtering operations
    @Query("SELECT * FROM population_data WHERE year = :year")
    fun getPopulationDataByYear(year: Int): Flow<List<PopulationData>>

    @Query("SELECT * FROM population_data WHERE stateAbbreviation = :state")
    fun getPopulationDataByState(state: String): Flow<List<PopulationData>>

    @Query("SELECT * FROM population_data WHERE countyName = :county AND stateAbbreviation = :state")
    fun getPopulationDataByCounty(county: String, state: String): Flow<List<PopulationData>>

    // Spatial queries
    @Query("""
        SELECT *, (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) AS distance 
        FROM population_data
        WHERE year = :year
        ORDER BY distance ASC
        LIMIT 1
    """)
    suspend fun getNearestPopulationData(latitude: Double, longitude: Double, year: Int): PopulationData?

    // Correlation queries
    @Query("""
        SELECT AVG(pd.populationDensity) as avg_density
        FROM sightings s
        JOIN population_data pd ON 
            s.state = pd.stateAbbreviation AND
            (6371 * acos(
                cos(radians(s.latitude)) * cos(radians(pd.latitude)) * 
                cos(radians(pd.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(pd.latitude))
            )) <= 50
        WHERE pd.year = :year
    """)
    suspend fun getAveragePopulationDensityForSightings(year: Int): Float

    @Query("""
        SELECT 
            CASE 
                WHEN pd.populationDensity < 10 THEN 'Rural (< 10 pop/km²)'
                WHEN pd.populationDensity < 100 THEN 'Suburban (10-100 pop/km²)'
                WHEN pd.populationDensity < 1000 THEN 'Urban (100-1000 pop/km²)'
                ELSE 'Metro (> 1000 pop/km²)'
            END AS density_category,
            COUNT(DISTINCT s.id) as sighting_count
        FROM sightings s
        JOIN population_data pd ON 
            s.state = pd.stateAbbreviation AND
            (6371 * acos(
                cos(radians(s.latitude)) * cos(radians(pd.latitude)) * 
                cos(radians(pd.longitude) - radians(s.longitude)) + 
                sin(radians(s.latitude)) * sin(radians(pd.latitude))
            )) <= 50
        WHERE pd.year = :year
        GROUP BY density_category
        ORDER BY MIN(pd.populationDensity)
    """)
    fun getSightingsByPopulationDensity(year: Int): Flow<List<PopulationDensityDistribution>>
}

/**
 * Data class for population density distribution statistics
 */
data class PopulationDensityDistribution(
    val density_category: String,
    val sighting_count: Int
)