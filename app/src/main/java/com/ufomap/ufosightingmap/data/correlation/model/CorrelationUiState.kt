package com.ufomap.ufosightingmap.ui.correlation.model

import com.ufomap.ufosightingmap.data.correlation.dao.DistanceDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.EventTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.MeteorShowerCorrelation
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDensityDistribution
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithAstronomicalEvents
import com.ufomap.ufosightingmap.data.correlation.dao.SightingWithBaseDistance
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherTypeDistribution
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent

/**
 * UI state for the Correlation Analysis screen.
 * Contains all data needed to render the correlation analysis UI.
 */
data class CorrelationUiState(
    // Common state
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdateTime: Long = 0,

    // Military base correlation
    val militaryBases: List<MilitaryBase> = emptyList(),
    val sightingsWithBaseDistance: List<SightingWithBaseDistance> = emptyList(),
    val militaryBaseDistanceDistribution: List<DistanceDistribution> = emptyList(),
    val militaryBaseCorrelationPercentage: Float = 0f,
    val currentBaseRadiusKm: Double = 50.0,

    // Astronomical event correlation
    val astronomicalEvents: List<AstronomicalEvent> = emptyList(),
    val sightingsWithAstronomicalEvents: List<SightingWithAstronomicalEvents> = emptyList(),
    val eventTypeDistribution: List<EventTypeDistribution> = emptyList(),
    val meteorShowerCorrelation: List<MeteorShowerCorrelation> = emptyList(),
    val astronomicalEventCorrelationPercentage: Float = 0f,

    // Weather correlation
    val weatherEvents: List<WeatherEvent> = emptyList(),
    val unusualWeatherEvents: List<WeatherEvent> = emptyList(),
    val weatherTypeDistribution: List<WeatherTypeDistribution> = emptyList(),
    val unusualWeatherPercentage: Float = 0f,

    // Population correlation
    val populationData: List<PopulationData> = emptyList(),
    val populationDensityDistribution: List<PopulationDensityDistribution> = emptyList(),
    val populationAverageDensity: Float = 0f
) {
    /**
     * Check if any data is currently loading
     */
    val isRefreshing: Boolean get() = isLoading

    /**
     * Check if any error occurred
     */
    val hasError: Boolean get() = error != null

    /**
     * Check if we have any data to display
     */
    val hasData: Boolean get() =
        militaryBases.isNotEmpty() ||
                astronomicalEvents.isNotEmpty() ||
                weatherEvents.isNotEmpty() ||
                populationData.isNotEmpty()
}