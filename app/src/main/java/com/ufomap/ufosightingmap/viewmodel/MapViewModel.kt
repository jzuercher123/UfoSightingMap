package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SightingRepository

    // Filter state
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Exposed sightings as StateFlow
    val sightings: StateFlow<List<Sighting>>

    init {
        val sightingDao = AppDatabase.getDatabase(application).sightingDao()
        repository = SightingRepository(sightingDao, application.applicationContext)

        // Initialize the database with data from JSON if it's empty
        repository.initializeDatabaseIfNeeded(viewModelScope)

        // Create a flow that changes whenever the filter state changes
        @OptIn(ExperimentalCoroutinesApi::class)
        sightings = _filterState
            .flatMapLatest { filterState ->
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
        _filterState.value = FilterState()
    }
}