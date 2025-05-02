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
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.data.correlation.repositories.AstronomicalEventRepository
import com.ufomap.ufosightingmap.data.correlation.repositories.MilitaryBaseRepository
import com.ufomap.ufosightingmap.data.correlation.repositories.PopulationDataRepository
import com.ufomap.ufosightingmap.data.correlation.repositories.WeatherEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for the correlation analysis screen.
 * Coordinates data operations between the repositories and UI.
 */
class CorrelationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CorrelationViewModel"

    // Set up repositories
    private val militaryBaseRepository: MilitaryBaseRepository
    private val astronomicalEventRepository: AstronomicalEventRepository
    private val weatherEventRepository: WeatherEventRepository
    private val populationDataRepository: PopulationDataRepository

    // Military base correlation state
    val militaryBases: StateFlow<List<MilitaryBase>>
    val sightingsWithBaseDistance: StateFlow<List<SightingWithBaseDistance>>
    val militaryBaseDistanceDistribution: StateFlow<List<DistanceDistribution>>
    private val _militaryBaseCorrelationPercentage = MutableStateFlow(0f)
    val militaryBaseCorrelationPercentage: StateFlow<Float> = _militaryBaseCorrelationPercentage

    // Astronomical event correlation state
    val astronomicalEvents: StateFlow<List<AstronomicalEvent>>
    val sightingsWithAstronomicalEvents: StateFlow<List<SightingWithAstronomicalEvents>>
    val eventTypeDistribution: StateFlow<List<EventTypeDistribution>>
    val meteorShowerCorrelation: StateFlow<List<MeteorShowerCorrelation>>
    private val _astronomicalEventCorrelationPercentage = MutableStateFlow(0f)
    val astronomicalEventCorrelationPercentage: StateFlow<Float> = _astronomicalEventCorrelationPercentage

    // Weather correlation state
    val currentWeatherEvents: StateFlow<List<WeatherEvent>>

    // Population data correlation state
    val populationData: StateFlow<List<PopulationData>>

    // Current correlation filter state
    private val _currentBaseRadiusKm = MutableStateFlow(50.0)
    val currentBaseRadiusKm: StateFlow<Double> = _currentBaseRadiusKm

    // Loading and error state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        Log.d(TAG, "Initializing CorrelationViewModel")

        // Initialize Database and DAOs
        val database = AppDatabase.getDatabase(application)
        val militaryBaseDao = database.militaryBaseDao()
        val astronomicalEventDao = database.astronomicalEventDao()
        val weatherEventDao = database.weatherEventDao()
        val populationDataDao = database.populationDataDao()

        // Initialize repositories
        militaryBaseRepository = MilitaryBaseRepository(militaryBaseDao, application)
        astronomicalEventRepository = AstronomicalEventRepository(astronomicalEventDao, application)
        weatherEventRepository = WeatherEventRepository(weatherEventDao, application)
        populationDataRepository = PopulationDataRepository(populationDataDao, application)

        // Initialize StateFlows from repositories
        militaryBases = militaryBaseRepository.allBases
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        sightingsWithBaseDistance = militaryBaseRepository.sightingsWithBaseDistance
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        militaryBaseDistanceDistribution = militaryBaseRepository.sightingDistanceDistribution
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        astronomicalEvents = astronomicalEventRepository.allEvents
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        sightingsWithAstronomicalEvents = astronomicalEventRepository.sightingsWithEvents
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        eventTypeDistribution = astronomicalEventRepository.sightingsByEventType
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        meteorShowerCorrelation = astronomicalEventRepository.meteorShowerCorrelation
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        currentWeatherEvents = weatherEventRepository.currentWeatherEvents
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        populationData = populationDataRepository.allPopulationData
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Initialize the databases
        initializeDatabases()

        // Load initial correlation statistics
        loadCorrelationStatistics()
    }

    /**
     * Initialize all correlation databases if needed
     */
    private fun initializeDatabases() {
        Log.d(TAG, "Initializing correlation databases")
        _isLoading.value = true

        militaryBaseRepository.initializeDatabaseIfNeeded(viewModelScope)
        astronomicalEventRepository.initializeDatabaseIfNeeded(viewModelScope)
        weatherEventRepository.initializeDatabaseIfNeeded(viewModelScope)
        populationDataRepository.initializeDatabaseIfNeeded(viewModelScope)

        viewModelScope.launch {
            try {
                // Check if data is loaded by monitoring the first repository
                militaryBases.collectLatest {
                    if (it.isNotEmpty()) {
                        Log.d(TAG, "Correlation data loaded successfully")
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing correlation databases: ${e.message}", e)
                _errorMessage.value = "Failed to load correlation data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Reload all correlation data
     */
    fun reloadAllData() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                militaryBaseRepository.forceReloadData()
                astronomicalEventRepository.forceReloadData()
                weatherEventRepository.forceReloadData()
                populationDataRepository.forceReloadData()

                loadCorrelationStatistics()
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading correlation data: ${e.message}", e)
                _errorMessage.value = "Failed to reload correlation data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load correlation statistics
     */
    private fun loadCorrelationStatistics() {
        viewModelScope.launch {
            try {
                // Military base correlation percentage
                _militaryBaseCorrelationPercentage.value =
                    militaryBaseRepository.getPercentageSightingsNearBases(_currentBaseRadiusKm.value)

                // Astronomical event correlation percentage
                _astronomicalEventCorrelationPercentage.value =
                    astronomicalEventRepository.getPercentageSightingsDuringEvents()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading correlation statistics: ${e.message}", e)
                _errorMessage.value = "Failed to load correlation statistics: ${e.message}"
            }
        }
    }

    /**
     * Set the radius for military base proximity analysis
     */
    fun setMilitaryBaseRadius(radiusKm: Double) {
        if (radiusKm != _currentBaseRadiusKm.value) {
            _currentBaseRadiusKm.value = radiusKm

            // Update military base correlation percentage with new radius
            viewModelScope.launch {
                try {
                    _militaryBaseCorrelationPercentage.value =
                        militaryBaseRepository.getPercentageSightingsNearBases(radiusKm)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating military base correlation: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Get astronomical events happening on a specific date
     */
    fun getAstronomicalEventsOnDate(date: Date) =
        astronomicalEventRepository.getEventsOnDate(date)
            .catch { e ->
                Log.e(TAG, "Error getting events on date: ${e.message}", e)
                _errorMessage.value = "Failed to get events: ${e.message}"
            }

    /**
     * Get astronomical events happening during a date range
     */
    fun getAstronomicalEventsInDateRange(startDate: Date, endDate: Date) =
        astronomicalEventRepository.getEventsInDateRange(startDate, endDate)
            .catch { e ->
                Log.e(TAG, "Error getting events in date range: ${e.message}", e)
                _errorMessage.value = "Failed to get events: ${e.message}"
            }

    /**
     * Get astronomical events of a specific type
     */
    fun getAstronomicalEventsByType(type: AstronomicalEvent.EventType) =
        astronomicalEventRepository.getEventsByType(type)
            .catch { e ->
                Log.e(TAG, "Error getting events by type: ${e.message}", e)
                _errorMessage.value = "Failed to get events: ${e.message}"
            }

    /**
     * Get weather events for a specific location
     */
    fun getWeatherEventsForLocation(latitude: Double, longitude: Double, radiusKm: Double) =
        weatherEventRepository.getWeatherEventsNearLocation(latitude, longitude, radiusKm)
            .catch { e ->
                Log.e(TAG, "Error getting weather events: ${e.message}", e)
                _errorMessage.value = "Failed to get weather events: ${e.message}"
            }

    /**
     * Get population data for a specific state
     */
    fun getPopulationDataForState(state: String) =
        populationDataRepository.getPopulationDataByState(state)
            .catch { e ->
                Log.e(TAG, "Error getting population data: ${e.message}", e)
                _errorMessage.value = "Failed to get population data: ${e.message}"
            }
}