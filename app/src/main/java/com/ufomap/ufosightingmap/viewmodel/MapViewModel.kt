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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

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
        Log.d("MapViewModel", "Initializing MapViewModel")

        val sightingDao = AppDatabase.getDatabase(application).sightingDao()
        repository = SightingRepository(sightingDao, application.applicationContext)

        // Debug asset files first to check if they're accessible
        repository.debugAssetFiles()

        // Create a flow that changes whenever the filter state changes
        @OptIn(ExperimentalCoroutinesApi::class)
        sightings = _filterState
            .flatMapLatest { filterState ->
                Log.d("MapViewModel", "Filter state changed: ${filterState.hasActiveFilters()}")
                if (filterState.hasActiveFilters()) {
                    repository.getFilteredSightings(
                        shape = filterState.shape,
                        city = filterState.city,
                        country = filterState.country,
                        state = filterState.state,
                        startDate = filterState.startDate,
                        endDate = filterState.endDate,
                        searchText = filterState.searchText
                    ).onEach { list ->
                        Log.d("MapViewModel", "Filtered sightings flow emitted ${list.size} items")
                        _isLoading.value = false
                    }
                } else {
                    repository.allSightings.onEach { list ->
                        Log.d("MapViewModel", "All sightings flow emitted ${list.size} items")
                        _isLoading.value = false
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    }

    /**
     * Update the search text and apply filter
     */
    fun updateSearchQuery(query: String) {
        Log.d("MapViewModel", "Updating search query: '$query'")
        _filterState.value = _filterState.value.copy(
            searchText = query.takeIf { it.isNotBlank() },
            isFilterApplied = query.isNotBlank() || _filterState.value.hasActiveFilters()
        )
    }

    /**
     * Update filter values
     */
    fun updateFilters(
        shape: String? = _filterState.value.shape,
        city: String? = _filterState.value.city,
        country: String? = _filterState.value.country,
        state: String? = _filterState.value.state,
        startDate: String? = _filterState.value.startDate,
        endDate: String? = _filterState.value.endDate
    ) {
        Log.d("MapViewModel", "Updating filters - shape: $shape, state: $state, country: $country")
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
     * Clear a specific filter
     */
    fun clearFilter(filterName: String) {
        Log.d("MapViewModel", "Clearing filter: $filterName")
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
     * Clear all filters
     */
    fun clearFilters() {
        Log.d("MapViewModel", "Clearing all filters")
        _filterState.value = FilterState()
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorState.value = null
    }

    /**
     * Initialize database if needed
     */
    fun initializeDatabaseIfNeeded(scope: kotlinx.coroutines.CoroutineScope) {
        _isLoading.value = true
        repository.initializeDatabaseIfNeeded(scope)
    }

    /**
     * Force reload data
     */
    fun forceReloadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.forceReloadData()
            } catch (e: Exception) {
                _errorState.value = "Failed to reload data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}