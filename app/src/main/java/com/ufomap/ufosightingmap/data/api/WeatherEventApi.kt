package com.ufomap.ufosightingmap.data.api

import com.google.gson.GsonBuilder
import com.ufomap.ufosightingmap.BuildConfig
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
    suspend fun getWeatherEvents(): List<WeatherEvent> {
        // Implementation will convert API responses to domain models
        // This would typically fetch alerts and observations and merge the data
        return emptyList() // Placeholder implementation
    }

    // Response data classes
    data class AlertsResponse(
        val features: List<AlertFeature>
    )

    data class AlertFeature(
        val properties: AlertProperties
    )

    data class AlertProperties(
        val id: String,
        val areaDesc: String,
        val sent: String,
        val effective: String,
        val onset: String?,
        val expires: String,
        val ends: String?,
        val status: String,
        val messageType: String,
        val category: String,
        val severity: String,
        val certainty: String,
        val urgency: String,
        val event: String,
        val headline: String?,
        val description: String,
        val instruction: String?
    )

    data class StationsResponse(
        val observationStations: List<String>
    )

    data class ObservationResponse(
        val properties: ObservationProperties
    )

    data class ObservationProperties(
        val temperature: QuantitativeValue,
        val windSpeed: QuantitativeValue,
        val windDirection: QuantitativeValue,
        val barometricPressure: QuantitativeValue,
        val seaLevelPressure: QuantitativeValue,
        val visibility: QuantitativeValue,
        val relativeHumidity: QuantitativeValue,
        val windChill: QuantitativeValue,
        val heatIndex: QuantitativeValue,
        val textDescription: String
    )

    data class QuantitativeValue(
        val value: Double?,
        val unitCode: String
    )
}

/**
 * Factory to create the Weather API service
 */
object WeatherApiServiceFactory {

    fun create(): WeatherEventApi {
        // Set up the OkHttp client with required headers
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Add required User-Agent header
                val request = chain.request().newBuilder()
                    .header("User-Agent",
                        "UFOSightingMapApp/1.0 (com.ufomap.ufosightingmap; contact@example.com)")
                    .header("Accept", "application/geo+json")
                    .build()
                chain.proceed(request)
            }
            // Add logging in debug mode
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Set up Gson
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .create()

        // Create and return the API service
        return Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WeatherEventApi::class.java)
    }
}