package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.Sighting
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the sighting detail screen
 * Responsible for loading the specific sighting data based on ID
 */
class SightingDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val sightingDao = AppDatabase.getDatabase(application).sightingDao()

    /**
     * Get a specific sighting by ID as a Flow
     * Returns null if the sighting doesn't exist
     */
    fun getSighting(id: Int): Flow<Sighting?> {
        return sightingDao.getSightingById(id)
    }
}