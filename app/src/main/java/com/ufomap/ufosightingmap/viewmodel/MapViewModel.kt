package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.FilterState
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.data.SightingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the map screen that manages sighting data and filtering.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapViewModel"
    private val repository: SightingRepository

    // Filter state
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Error state
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Exposed sightings as StateFlow
    val sightings: StateFlow<List<Sighting>>

    init {
        Log.d(TAG, "Initializing MapViewModel")

        // Initialize repository and database
        val sightingDao = AppDatabase.getDatabase(application).sightingDao()
        repository = SightingRepository(sightingDao, application.applicationContext)

        // Ensure database has data
        repository.initializeDatabaseIfNeeded(viewModelScope)

        // Verify assets are accessible
        repository.debugAssetFiles()
        verifyAssets(application)

        // Create a flow that changes whenever the filter state changes
        @OptIn(ExperimentalCoroutinesApi::class)
        sightings = _filterState
            .flatMapLatest { filterState ->
                Log.d(TAG, "Filter state changed: ${filterState.hasActiveFilters()}")
                if (filterState.hasActiveFilters()) {
                    repository.getFilteredSightings(
                        shape = filterState.shape,
                        city = filterState.city,
                        country = filterState.country,
                        state = filterState.state,
                        startDate = filterState.startDate,
                        endDate = filterState.endDate,
                        searchText = filterState.searchText
                    )
                } else {
                    repository.allSightings
                }
            }
            .onEach { list ->
                Log.d(TAG, "Sightings flow emitted ${list.size} items")

                // Log sample data for debugging
                if (list.isNotEmpty()) {
                    val sample = list.take(minOf(3, list.size))
                    Log.d(TAG, "Sample sightings: ${sample.joinToString { sighting ->
                        "${sighting.id}: ${sighting.city ?: "Unknown"} (${sighting.latitude}, ${sighting.longitude})"
                    }}")
                }

                _isLoading.value = false
            }
            .catch { e ->
                Log.e(TAG, "Error in sightings flow: ${e.message}", e)
                _errorState.value = "Error loading sightings: ${e.message}"
                _isLoading.value = false
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

        // Check database status after initialization
        checkDatabaseStatus()
    }

    /**
     * Verifies that assets are accessible.
     */
    private fun verifyAssets(application: Application) {
        viewModelScope.launch {
            try {
                val assetFiles = application.assets.list("")
                Log.d(TAG, "Available asset files: ${assetFiles?.joinToString()}")

                if (assetFiles?.contains("sightings.json") == true) {
                    val size = application.assets.open("sightings.json").available()
                    Log.d(TAG, "sightings.json size: $size bytes")

                    // Read a sample to verify content
                    val sample = application.assets.open("sightings.json").bufferedReader().use {
                        it.readText().take(100)
                    }
                    Log.d(TAG, "JSON content starts with: $sample...")
                } else {
                    Log.e(TAG, "sightings.json NOT FOUND in assets!")
                    _errorState.value = "Critical error: sightings.json not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking assets: ${e.message}", e)
                _errorState.value = "Error accessing assets: ${e.message}"
            }
        }
    }

    /**
     * Checks database status and logs information.
     */
    private fun checkDatabaseStatus() {
        viewModelScope.launch {
            try {
                val count = repository.getRawCount()
                Log.d(TAG, "Database contains $count sightings")

                if (count == 0) {
                    Log.w(TAG, "Database appears to be empty! Forcing data load.")
                    forceReloadData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database: ${e.message}", e)
            }
        }
    }

    /**
     * Updates the search query and applies filtering.
     */
    fun updateSearchQuery(query: String) {
        Log.d(TAG, "Updating search query: '$query'")
        _filterState.value = _filterState.value.copy(
            searchText = query.takeIf { it.isNotBlank() },
            isFilterApplied = query.isNotBlank() || _filterState.value.hasActiveFilters()
        )
    }

    /**
     * Updates filter values.
     */
    fun updateFilters(
        shape: String? = _filterState.value.shape,
        city: String? = _filterState.value.city,
        country: String? = _filterState.value.country,
        state: String? = _filterState.value.state,
        startDate: String? = _filterState.value.startDate,
        endDate: String? = _filterState.value.endDate
    ) {
        Log.d(TAG, "Updating filters - shape: $shape, state: $state, country: $country")
        _filterState.value = _filterState.value.copy(
            shape = shape,
            city = city,
            country = country,
            state = state,
            startDate = startDate,
            endDate = endDate,
            isFilterApplied = true
        )
    }

    /**
     * Clears a specific filter.
     */
    fun clearFilter(filterName: String) {
        Log.d(TAG, "Clearing filter: $filterName")
        _filterState.value = when (filterName) {
            "shape" -> _filterState.value.copy(shape = null)
            "city" -> _filterState.value.copy(city = null)
            "country" -> _filterState.value.copy(country = null)
            "state" -> _filterState.value.copy(state = null)
            "startDate" -> _filterState.value.copy(startDate = null)
            "endDate" -> _filterState.value.copy(endDate = null)
            "searchText" -> _filterState.value.copy(searchText = null)
            else -> _filterState.value
        }.apply {
            // Recalculate if any filters are still applied
            val stillHasFilters = hasActiveFilters()
            _filterState.value = copy(isFilterApplied = stillHasFilters)
        }
    }

    /**
     * Clears all filters.
     */
    fun clearFilters() {
        Log.d(TAG, "Clearing all filters")
        _filterState.value = FilterState()
    }

    /**
     * Clears error message.
     */
    fun clearErrorMessage() {
        _errorState.value = null
    }

    /**
     * Forces reload of sighting data from JSON.
     */
    fun forceReloadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null

            try {
                Log.d(TAG, "Force reloading sightings data")
                repository.forceReloadData()

                // Verify data was loaded
                val count = repository.getRawCount()
                Log.d(TAG, "Data reload complete - database now contains $count sightings")

                if (count == 0) {
                    _errorState.value = "Failed to load data - database still empty"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading data: ${e.message}", e)
                _errorState.value = "Failed to reload data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Places sighting markers on the map (debug helper).
     */
    fun placeSightingMarkers() {
        try {
            repository.debugAssetFiles()
            repository.getFilteredSightings()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to place markers: ${e.message}", e)
            _errorState.value = "Failed to place markers: ${e.message}"
        }
    }
}