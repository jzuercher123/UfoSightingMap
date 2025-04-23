package com.ufomap.ufosightingmap.viewmodel // Corrected package name

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase // Correct import
import com.ufomap.ufosightingmap.data.Sighting // Correct import
import com.ufomap.ufosightingmap.data.SightingRepository // Correct import
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch // Ensure launch is imported if needed, though covered by viewModelScope

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SightingRepository // Use correct type

    // Expose sightings as StateFlow
    val sightings: StateFlow<List<Sighting>> // Use correct Sighting type

    init {
        // Get DAO instance using correct AppDatabase import
        val sightingDao = AppDatabase.getDatabase(application).sightingDao()
        // Initialize Repository using correct SightingRepository import
        repository = SightingRepository(sightingDao, application.applicationContext)

        // Initialize the database with data from JSON if it's empty.
        // Use the correctly referenced repository method
        repository.initializeDatabaseIfNeeded(viewModelScope)

        // Start collecting the Flow from the repository and expose it as StateFlow.
        // Use the correctly referenced repository property
        sightings = repository.allSightings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList<Sighting>() // Provide explicit type argument
        )
    }

    // Optional: Function to fetch sightings based on map bounds
    // fun getSightingsForBounds(north: Double, south: Double, east: Double, west: Double) {
    //     // This would typically update a different StateFlow or trigger repository fetch
    // }
}
