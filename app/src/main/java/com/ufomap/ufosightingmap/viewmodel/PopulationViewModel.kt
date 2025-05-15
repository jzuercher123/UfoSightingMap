package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.data.repositories.PopulationDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for population-related functionality
 * Manages population data for display in the UI and correlation analysis
 */
class PopulationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "PopulationViewModel"

    // Repository
    private val repository: PopulationDataRepository

    // State flows for UI
    private val _populationData = MutableStateFlow<List<PopulationData>>(emptyList())
    val populationData: StateFlow<List<PopulationData>> = _populationData

    private val _populationDensityDistribution = MutableStateFlow<List<PopulationDensityDistribution>>(emptyList())
    val populationDensityDistribution: StateFlow<List<PopulationDensityDistribution>> = _populationDensityDistribution

    private val _averagePopulationDensity = MutableStateFlow(0f)
    val averagePopulationDensity: StateFlow<Float> = _averagePopulationDensity

    private val _selectedYear = MutableStateFlow(getCurrentYear())
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        Log.d(TAG, "Initializing PopulationViewModel")

        // Initialize repository
        val populationDataDao = AppDatabase.getDatabase(application).populationDataDao()
        repository = PopulationDataRepository(populationDataDao, application)

        // Initialize data
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeDatabaseIfNeeded(viewModelScope)

            // Start collecting data from repository
            repository.allPopulationData
                .catch { e ->
                    Log.e(TAG, "Error collecting population data: ${e.message}", e)
                    _error.value = "Failed to load population data: ${e.message}"
                }
                .collectLatest { data ->
                    _populationData.value = data
                }

            repository.populationDensityDistribution
                .catch { e ->
                    Log.e(TAG, "Error collecting density distribution: ${e.message}", e)
                    _error.value = "Failed to load density distribution: ${e.message}"
                }
                .collectLatest { distribution ->
                    _populationDensityDistribution.value = distribution
                }

            loadAveragePopulationDensity()
            _isLoading.value = false
        }
    }

    /**
     * Load average population density for areas with UFO sightings
     */
    private fun loadAveragePopulationDensity() {
        viewModelScope.launch {
            try {
                val avgDensity = repository.getAveragePopulationDensityForSightings()
                _averagePopulationDensity.value = avgDensity
                Log.d(TAG, "Average population density: $avgDensity")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading average population density: ${e.message}", e)
                _error.value = "Error calculating population statistics: ${e.message}"
            }
        }
    }

    /**
     * Set the selected year for analysis
     */
    fun setYear(year: Int) {
        if (year != _selectedYear.value) {
            _selectedYear.value = year
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    repository.getPopulationDataByYear(year)
                        .catch { e ->
                            Timber.e(e, "Error getting population data for year $year")
                            _error.value = "Failed to load data for year $year: ${e.message}"
                        }
                        .collectLatest { data ->
                            _populationData.value = data
                        }
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Refresh population data from source
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.forceReloadData()
                loadAveragePopulationDensity()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing population data: ${e.message}", e)
                _error.value = "Error refreshing population data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get data by state
     */
    fun getDataByState(state: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPopulationDataByState(state)
                    .catch { e ->
                        Timber.e(e, "Error getting population data for state $state")
                        _error.value = "Failed to load data for state $state: ${e.message}"
                    }
                    .collectLatest { data ->
                        _populationData.value = data
                    }
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

    /**
     * Get current year (or previous year for data availability)
     */
    private fun getCurrentYear(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1
    }
}