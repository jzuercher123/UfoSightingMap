package com.ufomap.ufosightingmap.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API interface for the weather.gov API
 * Documentation: https://www.weather.gov/documentation/services-web-api
 */
interface WeatherEventApi {
    /**
     * Get active alerts for a specific area
     */
    @GET("alerts/active/area/{area}")
    suspend fun getActiveAlertsByArea(@Path("area") area: String): AlertsResponse

    /**
     * Get active alerts for a specific point (lat,long)
     */
    @GET("alerts")
    suspend fun getActiveAlertsByPoint(
        @Query("point") point: String,
        @Query("status") status: String = "actual"
    ): AlertsResponse

    /**
     * Get severe weather events (alerts with severity "severe" or "extreme")
     */
    @GET("alerts")
    suspend fun getSevereWeatherEvents(
        @Query("severity") severity: String = "severe,extreme",
        @Query("status") status: String = "actual"
    ): AlertsResponse

    /**
     * Get the current weather conditions at a point
     */
    @GET("points/{point}/stations")
    suspend fun getStationsNearPoint(@Path("point") point: String): StationsResponse

    /**
     * Get observations for a specific station
     */
    @GET("stations/{stationId}/observations/latest")
    suspend fun getLatestObservation(@Path("stationId") stationId: String): ObservationResponse

    /**
     * Convert API responses to domain model objects
     */
    suspend fun getWeatherEvents(): List<Any> = emptyList()

    // Simple data classes to make the code compile
    data class AlertsResponse(val features: List<AlertFeature> = emptyList())
    data class AlertFeature(val properties: AlertProperties = AlertProperties())
    data class AlertProperties(
        val id: String = "",
        val areaDesc: String = "",
        val sent: String = "",
        val effective: String = "",
        val onset: String? = null,
        val expires: String = "",
        val ends: String? = null,
        val status: String = "",
        val messageType: String = "",
        val category: String = "",
        val severity: String = "",
        val certainty: String = "",
        val urgency: String = "",
        val event: String = "",
        val headline: String? = null,
        val description: String = "",
        val instruction: String? = null
    )
    data class StationsResponse(val observationStations: List<String> = emptyList())
    data class ObservationResponse(val properties: ObservationProperties = ObservationProperties())
    data class ObservationProperties(
        val temperature: QuantitativeValue = QuantitativeValue(),
        val windSpeed: QuantitativeValue = QuantitativeValue(),
        val windDirection: QuantitativeValue = QuantitativeValue(),
        val barometricPressure: QuantitativeValue = QuantitativeValue(),
        val seaLevelPressure: QuantitativeValue = QuantitativeValue(),
        val visibility: QuantitativeValue = QuantitativeValue(),
        val relativeHumidity: QuantitativeValue = QuantitativeValue(),
        val windChill: QuantitativeValue = QuantitativeValue(),
        val heatIndex: QuantitativeValue = QuantitativeValue(),
        val textDescription: String = ""
    )
    data class QuantitativeValue(
        val value: Double? = null,
        val unitCode: String = ""
    )
}