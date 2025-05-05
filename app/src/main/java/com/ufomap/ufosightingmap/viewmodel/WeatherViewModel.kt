package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.api.WeatherEventApi
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.data.repositories.WeatherEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

/**
 * ViewModel for weather-related functionality
 * Manages weather data for display in the UI and correlation analysis
 */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "WeatherViewModel"

    // Repository
    private val repository: WeatherEventRepository

    // State flows for UI
    private val _currentWeather = MutableStateFlow<WeatherEvent?>(null)
    val currentWeather: StateFlow<WeatherEvent?> = _currentWeather

    private val _nearbyWeatherEvents = MutableStateFlow<List<WeatherEvent>>(emptyList())
    val nearbyWeatherEvents: StateFlow<List<WeatherEvent>> = _nearbyWeatherEvents

    private val _unusualWeatherEvents = MutableStateFlow<List<WeatherEvent>>(emptyList())
    val unusualWeatherEvents: StateFlow<List<WeatherEvent>> = _unusualWeatherEvents

    private val _weatherTypeDistribution = MutableStateFlow<List<WeatherTypeDistribution>>(emptyList())
    val weatherTypeDistribution: StateFlow<List<WeatherTypeDistribution>> = _weatherTypeDistribution

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        Log.d(TAG, "Initializing WeatherViewModel")

        // Create API service (could be injected with Dagger/Hilt in a real app)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherEventApi::class.java)

        // Set up repository
        val weatherEventDao = AppDatabase.getDatabase(application).weatherEventDao()
        repository = WeatherEventRepository(weatherEventDao, application, api)

        // Initialize data
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeDatabaseIfNeeded(viewModelScope)

            // Start collecting unusual weather events
            repository.getUnusualWeatherEvents()
                .catch { e ->
                    Log.e(TAG, "Error collecting unusual weather events: ${e.message}", e)
                    _error.value = "Failed to load unusual weather events: ${e.message}"
                }
                .collectLatest { events ->
                    _unusualWeatherEvents.value = events
                }

            _isLoading.value = false
        }
    }

    /**
     * Fetch current weather for a location
     */
    fun fetchCurrentWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = repository.fetchCurrentWeather(latitude, longitude)
                if (result.isSuccess) {
                    _currentWeather.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unknown error fetching weather"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current weather: ${e.message}", e)
                _error.value = "Error fetching weather: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get weather events near a location
     */
    fun loadNearbyWeatherEvents(latitude: Double, longitude: Double, radiusKm: Double = 50.0) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getWeatherEventsNearLocation(latitude, longitude, radiusKm)
                    .catch { e ->
                        Log.e(TAG, "Error loading nearby weather: ${e.message}", e)
                        _error.value = "Error loading nearby weather: ${e.message}"
                    }
                    .collectLatest { events ->
                        _nearbyWeatherEvents.value = events
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get weather events by type
     */
    fun getWeatherEventsByType(type: WeatherEvent.WeatherType) = repository.getWeatherEventsByType(type)

    /**
     * Get weather events between dates
     */
    fun getWeatherEventsBetweenDates(startDate: Date, endDate: Date) =
        repository.getWeatherEventsBetweenDates(startDate.time, endDate.time)

    /**
     * Load weather type distribution for visualization
     */
    fun loadWeatherTypeDistribution() {
        viewModelScope.launch {
            repository.weatherEventDao.getWeatherTypeDistribution()
                .catch { e ->
                    Log.e(TAG, "Error loading weather type distribution: ${e.message}", e)
                    _error.value = "Error loading weather statistics: ${e.message}"
                }
                .collectLatest { distribution ->
                    _weatherTypeDistribution.value = distribution
                }
        }
    }

    /**
     * Refresh weather data from API
     */
    fun refreshWeatherData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.forceReloadData()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing weather data: ${e.message}", e)
                _error.value = "Error refreshing weather data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}