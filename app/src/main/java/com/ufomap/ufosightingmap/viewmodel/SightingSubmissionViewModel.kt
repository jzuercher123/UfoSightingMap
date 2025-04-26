package com.ufomap.ufosightingmap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.Sighting
import com.ufomap.ufosightingmap.data.SightingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ViewModel for the sighting submission screen
 * Handles the submission of new UFO sightings by users
 */
class SightingSubmissionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SightingRepository
    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Initial)
    val submissionState: StateFlow<SubmissionState> = _submissionState

    // Device ID for anonymous submissions
    private val deviceId = getDeviceId(application.applicationContext)

    init {
        val sightingDao = AppDatabase.getDatabase(application).sightingDao()
        repository = SightingRepository(sightingDao, application.applicationContext)
    }

    /**
     * Submit a new UFO sighting report
     */
    fun submitSighting(
        dateTime: String,
        city: String,
        state: String?,
        country: String,
        shape: String?,
        duration: String?,
        summary: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Submitting
            try {
                // Format current date for submission timestamp
                val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

                // Create a new sighting object
                val sighting = Sighting(
                    dateTime = dateTime,
                    city = city,
                    state = state,
                    country = country,
                    shape = shape,
                    duration = duration,
                    summary = summary,
                    posted = currentDate,
                    latitude = latitude,
                    longitude = longitude,
                    submittedBy = deviceId,
                    submissionDate = currentDate,
                    isUserSubmitted = true,
                    submissionStatus = "pending" // New submissions start as pending
                )

                // Save to database via repository
                val id = repository.addUserSighting(sighting)
                _submissionState.value = SubmissionState.Success(id.toInt())
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Get all sightings submitted by the current user's device
     */
    fun getUserSubmissions() {
        viewModelScope.launch {
            try {
                // The Flow is collected in the UI
                repository.getUserSubmissions(deviceId)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    /**
     * Generate or retrieve a persistent device ID for anonymous submissions
     */
    private fun getDeviceId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var deviceId = sharedPrefs.getString("device_id", null)

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("device_id", deviceId).apply()
        }

        return deviceId
    }

    /**
     * States for the submission process
     */
    sealed class SubmissionState {
        object Initial : SubmissionState()
        object Submitting : SubmissionState()
        data class Success(val id: Int) : SubmissionState()
        data class Error(val message: String) : SubmissionState()
    }
}