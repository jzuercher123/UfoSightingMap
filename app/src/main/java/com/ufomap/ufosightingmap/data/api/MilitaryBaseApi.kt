package com.ufomap.ufosightingmap.data.api

import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import retrofit2.http.GET

/**
 * API interface for fetching military base data
 * Note: This is currently a placeholder interface as we're using local data
 */
interface MilitaryBaseApi {
    @GET("military_bases")
    suspend fun getMilitaryBases(): List<MilitaryBase>
}