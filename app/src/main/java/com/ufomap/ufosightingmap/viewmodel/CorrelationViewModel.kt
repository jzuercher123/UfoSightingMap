package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.correlation.dao.DistanceDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.EventTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.MeteorShowerCorrelation
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithAstronomicalEvents
// Removed: import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance // This type wasn't defined in DAOs
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData // Keep model import
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.data.repositories.AstronomicalEventRepository // Correct import path
import com.ufomap.ufosightingmap.data.repositories.MilitaryBaseRepository // Correct import path
import com.ufomap.ufosightingmap.data.repositories.PopulationDataRepository // Assuming this exists or implement direct DAO access
import com.ufomap.ufosightingmap.data.repositories.WeatherEventRepository // Correct import path

// Import necessary DAO for direct access if PopulationDataRepository is not implemented
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance

import java.util.Date

/**
 * ViewModel for the correlation analysis screen.
 * Coordinates data operations between the repositories and UI.
 */
class CorrelationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CorrelationViewModel"

    // Database and DAOs
    private val database = AppDatabase.getDatabase(application)
    private val militaryBaseDao = database.militaryBaseDao()
    private val astronomicalEventDao = database.astronomicalEventDao()
    private val weatherEventDao = database.weatherEventDao()
    private val populationDataDao = database.populationDataDao()
    // Sighting DAO needed for some correlation stats if not handled purely in other DAOs
    // private val sightingDao = database.sightingDao() // Uncomment if needed

    // Repositories
    private val militaryBaseRepository: MilitaryBaseRepository
    private val astronomicalEventRepository: AstronomicalEventRepository
    private val weatherEventRepository: WeatherEventRepository
    // Commented out until PopulationDataRepository is implemented
    // private val populationDataRepository: PopulationDataRepository

    // --- StateFlows for UI ---

    // Loading and Error State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val populationData: StateFlow<List<PopulationData>> = populationDataDao.getAllPopulationData()
        .catch { e ->
            Log.e(TAG, "Error in populationData flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val populationDensityDistribution: StateFlow<List<PopulationDensityDistribution>> = populationDataDao.getSightingsByPopulationDensity(2023) // Example year
        .catch { e ->
            Log.e(TAG, "Error in populationDensityDistribution flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _populationAverageDensity = MutableStateFlow(0f)
    val populationAverageDensity: StateFlow<Float> = _populationAverageDensity

    // Military Base Correlation State
    val militaryBases: StateFlow<List<MilitaryBase>>
    // Commented out: val sightingsWithBaseDistance: StateFlow<List<SightingWithBaseDistance>>
    val militaryBaseDistanceDistribution: StateFlow<List<DistanceDistribution>>
    private val _militaryBaseCorrelationPercentage = MutableStateFlow(0f)
    val militaryBaseCorrelationPercentage: StateFlow<Float> = _militaryBaseCorrelationPercentage.asStateFlow()
    private val _currentBaseRadiusKm = MutableStateFlow(50.0) // Default radius
    val currentBaseRadiusKm: StateFlow<Double> = _currentBaseRadiusKm.asStateFlow()

    val sightingsWithBaseDistance: StateFlow<List<SightingWithBaseDistance>> = militaryBaseDao.getSightingsWithNearestBase()
        .catch { e ->
            Log.e(TAG, "Error sightingsWithBaseDistance flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Astronomical Event Correlation State
    val astronomicalEvents: StateFlow<List<AstronomicalEvent>>
    val sightingsWithAstronomicalEvents: StateFlow<List<SightingWithAstronomicalEvents>>
    val eventTypeDistribution: StateFlow<List<EventTypeDistribution>>
    val meteorShowerCorrelation: StateFlow<List<MeteorShowerCorrelation>>
    private val _astronomicalEventCorrelationPercentage = MutableStateFlow(0f)
    val astronomicalEventCorrelationPercentage: StateFlow<Float> = _astronomicalEventCorrelationPercentage.asStateFlow()

    // Weather Correlation State
    val unusualWeatherEvents: StateFlow<List<WeatherEvent>> // Flow for unusual weather events
    val weatherTypeDistribution: StateFlow<List<WeatherTypeDistribution>> // Flow for distribution
    private val _unusualWeatherPercentage = MutableStateFlow(0f)
    val unusualWeatherPercentage: StateFlow<Float> = _unusualWeatherPercentage.asStateFlow()

    // Population Data Correlation State (Commented out until repository/logic is implemented)
    // val populationData: StateFlow<List<PopulationData>>
    // val populationDensityDistribution: StateFlow<List<PopulationDensityDistribution>>

    init {
        Log.d(TAG, "Initializing CorrelationViewModel")

        // Initialize implemented repositories
        militaryBaseRepository = MilitaryBaseRepository(militaryBaseDao, application)
        astronomicalEventRepository = AstronomicalEventRepository(astronomicalEventDao, application)
        weatherEventRepository = WeatherEventRepository(weatherEventDao, application)
        // populationDataRepository = PopulationDataRepository(populationDataDao, application) // Commented out

        // Initialize StateFlows from repositories or DAOs directly
        militaryBases = militaryBaseRepository.allBases
            .catch { e -> Log.e(TAG, "Error militaryBases flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val sightingsWithBaseDistance = militaryBaseDao.getSightingsWithNearestBase()
            .catch { e -> Log.e(TAG, "Error sightingsWithBaseDistance flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        militaryBaseDistanceDistribution = militaryBaseRepository.sightingDistanceDistribution
            .catch { e -> Log.e(TAG, "Error militaryBaseDistanceDistribution flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        astronomicalEvents = astronomicalEventRepository.allEvents
            .catch { e -> Log.e(TAG, "Error astronomicalEvents flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        sightingsWithAstronomicalEvents = astronomicalEventRepository.sightingsWithEvents
            .catch { e -> Log.e(TAG, "Error sightingsWithAstronomicalEvents flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        eventTypeDistribution = astronomicalEventRepository.sightingsByEventType
            .catch { e -> Log.e(TAG, "Error eventTypeDistribution flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        meteorShowerCorrelation = astronomicalEventRepository.meteorShowerCorrelation
            .catch { e -> Log.e(TAG, "Error meteorShowerCorrelation flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Weather specific flows
        unusualWeatherEvents = weatherEventRepository.getUnusualWeatherEvents()
            .catch { e -> Log.e(TAG, "Error unusualWeatherEvents flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Use repository function for consistency, though direct DAO access works too
        weatherTypeDistribution = weatherEventDao.getWeatherTypeDistribution()
            .catch { e -> Log.e(TAG, "Error weatherTypeDistribution flow: ${e.message}"); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Population specific flows (Commented out)
        // populationData = populationDataRepository.allPopulationData
        //     .catch { ... }
        //     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        // populationDensityDistribution = populationDataDao.getSightingsByPopulationDensity(2023) // Example year
        //     .catch { ... }
        //     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


        // Initialize the databases and load initial statistics
        initializeAndLoadData()
    }

    /**
     * Fetch military base data from OpenStreetMap
     */
    fun fetchMilitaryBaseData(replaceExisting: Boolean = false) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = militaryBaseRepository.fetchUSMilitaryBases(replaceExisting)

                result.fold(
                    onSuccess = { count ->
                        Log.d(TAG, "Successfully fetched $count military bases")
                        _errorMessage.value = null

                        // Reload statistics
                        loadCorrelationStatistics()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error fetching military bases: ${exception.message}", exception)
                        _errorMessage.value = "Failed to fetch military base data: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching military bases: ${e.message}", e)
                _errorMessage.value = "Exception fetching military base data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    public fun loadFromJsonAsset() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Force reload from JSON asset
                militaryBaseRepository.forceReloadData()

                // Reload statistics after data is loaded
                loadCorrelationStatistics()

                Log.d("MilitaryBaseCorrelationTab", "Successfully loaded military bases from local JSON asset")
            } catch (e: Exception) {
                Log.e("MilitaryBaseCorrelationTab", "Error loading from JSON asset: ${e.message}", e)
                _errorMessage.value = "Failed to load from JSON asset: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Initializes databases and loads initial correlation statistics. */
    private fun initializeAndLoadData() {
        Log.d(TAG, "Initializing correlation databases and loading stats")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Initialize all repositories (they handle internal checks)
                militaryBaseRepository.initializeDatabaseIfNeeded(viewModelScope)
                astronomicalEventRepository.initializeDatabaseIfNeeded(viewModelScope)
                weatherEventRepository.initializeDatabaseIfNeeded(viewModelScope)
                // populationDataRepository.initializeDatabaseIfNeeded(viewModelScope) // Commented out

                // Load initial statistics after ensuring data is likely present
                // We might need a better way to know when all repos are ready,
                // but loading stats here is a common pattern.
                loadCorrelationStatistics()

            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization or initial load: ${e.message}", e)
                _errorMessage.value = "Failed to load correlation data: ${e.message}"
            } finally {
                // Consider setting isLoading false only after stats are loaded,
                // or use separate loading states per data type.
                // For simplicity, setting false after init attempt.
                _isLoading.value = false
            }
        }
    }

    /** Reloads all correlation data from scratch. */
    fun reloadAllData() {
        Log.d(TAG, "Reloading all correlation data")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Force reload data in all implemented repositories
                militaryBaseRepository.forceReloadData()
                astronomicalEventRepository.forceReloadData()
                weatherEventRepository.forceReloadData()
                // populationDataRepository.forceReloadData() // Commented out

                // Reload statistics after data is refreshed
                loadCorrelationStatistics()

            } catch (e: Exception) {
                Log.e(TAG, "Error reloading correlation data: ${e.message}", e)
                _errorMessage.value = "Failed to reload correlation data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Loads various correlation percentage statistics. */
    private fun loadCorrelationStatistics() {
        viewModelScope.launch {
            try {

                _isLoading.value = true // Indicate loading for stats calculation
                // Military base correlation percentage (using current radius)
                _militaryBaseCorrelationPercentage.value =
                    militaryBaseRepository.getPercentageSightingsNearBases(_currentBaseRadiusKm.value)

                // Astronomical event correlation percentage
                _astronomicalEventCorrelationPercentage.value =
                    astronomicalEventRepository.getPercentageSightingsDuringEvents()

                // Weather correlation percentage - USE REPOSITORY FUNCTION
                _unusualWeatherPercentage.value =
                    weatherEventRepository.getPercentageSightingsDuringUnusualWeather()


                // Load population stats if needed (Commented out)
                // val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1 // Example: use previous year
                // _avgPopulationDensity.value = populationDataRepository.getAveragePopulationDensityForSightings(currentYear)
                // Example population density calculation
                val populationRepo = PopulationDataRepository(populationDataDao, getApplication())
                _populationAverageDensity.value = populationRepo.getAveragePopulationDensityForSightings()

                Log.d(TAG, "Correlation statistics loaded/reloaded.")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading correlation statistics: ${e.message}", e)
                _errorMessage.value = "Failed to load correlation stats: ${e.message}"
            }
            finally {
                // Set loading false after stats attempt, even if error occurred
                _isLoading.value = false
            }
        }
    }

    /** Sets the radius for military base proximity analysis and reloads related stats. */
    fun setMilitaryBaseRadius(radiusKm: Double) {
        val cleanRadius = radiusKm.coerceIn(1.0, 1000.0) // Basic validation
        if (cleanRadius != _currentBaseRadiusKm.value) {
            Log.d(TAG, "Setting military base radius to: $cleanRadius km")
            _currentBaseRadiusKm.value = cleanRadius

            // Update military base correlation percentage with new radius
            viewModelScope.launch {
                try {
                    _isLoading.value = true // Indicate loading for this specific stat update
                    _militaryBaseCorrelationPercentage.value =
                        militaryBaseRepository.getPercentageSightingsNearBases(cleanRadius)
                    Log.d(TAG, "Military base correlation updated for radius $cleanRadius km: ${_militaryBaseCorrelationPercentage.value}%")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating military base correlation: ${e.message}", e)
                    _errorMessage.value = "Failed to update military correlation: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /** Clears the current error message. */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // --- Functions providing specific data flows (can be called by UI if needed) ---

    /** Gets astronomical events happening on a specific date. */
    fun getAstronomicalEventsOnDate(date: Date): Flow<List<AstronomicalEvent>> =
        astronomicalEventRepository.getEventsOnDate(date)
            .catch { e ->
                Log.e(TAG, "Error getting events on date: ${e.message}", e)
                _errorMessage.value = "Failed to get events for date: ${e.message}"
                emit(emptyList()) // Emit empty on error
            }

    /** Gets astronomical events happening during a date range. */
    fun getAstronomicalEventsInDateRange(startDate: Date, endDate: Date): Flow<List<AstronomicalEvent>> =
        astronomicalEventRepository.getEventsInDateRange(startDate, endDate)
            .catch { e ->
                Log.e(TAG, "Error getting events in date range: ${e.message}", e)
                _errorMessage.value = "Failed to get events for range: ${e.message}"
                emit(emptyList())
            }

    /** Gets astronomical events of a specific type. */
    fun getAstronomicalEventsByType(type: AstronomicalEvent.EventType): Flow<List<AstronomicalEvent>> =
        astronomicalEventRepository.getEventsByType(type)
            .catch { e ->
                Log.e(TAG, "Error getting events by type: ${e.message}", e)
                _errorMessage.value = "Failed to get events by type: ${e.message}"
                emit(emptyList())
            }

    /** Gets weather events for a specific location. */
    fun getWeatherEventsForLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<WeatherEvent>> =
        weatherEventRepository.getWeatherEventsNearLocation(latitude, longitude, radiusKm)
            .catch { e ->
                Log.e(TAG, "Error getting weather events for location: ${e.message}", e)
                _errorMessage.value = "Failed to get weather events for location: ${e.message}"
                emit(emptyList())
            }

    /** Gets population data for a specific state (Example - requires implementation). */
    fun getPopulationDataForState(state: String): Flow<List<PopulationData>> {
        // Needs PopulationDataRepository implementation
        Log.w(TAG, "getPopulationDataForState called, but PopulationDataRepository might not be fully implemented.")
        // Placeholder implementation accessing DAO directly - Replace with repository access later
        return populationDataDao.getPopulationDataByState(state)
            .catch { e ->
                Log.e(TAG, "Error getting population data for state: ${e.message}", e)
                _errorMessage.value = "Failed to get population data: ${e.message}"
                emit(emptyList())
            }
        // return populationDataRepository.getPopulationDataByState(state)
        //     .catch { e ->
        //         Log.e(TAG, "Error getting population data for state: ${e.message}", e)
        //         _errorMessage.value = "Failed to get population data: ${e.message}"
        //         emit(emptyList())
        //     }
    }
}