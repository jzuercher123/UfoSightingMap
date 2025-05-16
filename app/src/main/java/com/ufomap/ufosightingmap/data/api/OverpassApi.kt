// app/src/main/java/com/ufomap/ufosightingmap/data/api/OverpassApi.kt

package com.ufomap.ufosightingmap.data.api

import android.util.Log
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Service class for fetching military base data from OpenStreetMap using the Overpass API
 */
class OverpassApi {
    private val TAG = "OverpassApi"
    private val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"

    /**
     * Fetches military installations from OpenStreetMap
     * Uses the Overpass QL query language to filter by military=* tag
     *
     * @param boundingBox Optional bounding box to limit search [south,west,north,east]
     * @param limit Maximum number of results to return
     * @return List of MilitaryBase objects parsed from the response
     */
    /**
     * Fetches military installations from OpenStreetMap with retry logic
     */
    suspend fun fetchMilitaryBases(
        boundingBox: String? = null,
        limit: Int = 500,
        maxRetries: Int = 3
    ): List<MilitaryBase> = withContext(Dispatchers.IO) {
        var retries = 0
        var lastException: Exception? = null

        while (retries < maxRetries) {
            try {
                val query = buildOverpassQuery(boundingBox, limit)
                Log.d(TAG, "Executing Overpass query (attempt ${retries + 1}): $query")

                val url = URL(OVERPASS_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 60000 // 60 seconds
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("User-Agent", "UFOSightingMapApp/1.0")

                // Send the query
                val postData = "data=${URLEncoder.encode(query, "UTF-8")}"
                connection.outputStream.use { os ->
                    os.write(postData.toByteArray(Charsets.UTF_8))
                }

                // Check for rate limiting or server errors
                val responseCode = connection.responseCode
                if (responseCode == 429) {
                    // Too many requests - wait and retry
                    val waitTime = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: (30000L * (retries + 1))
                    Timber.tag(TAG)
                        .w("Rate limited by Overpass API. Waiting ${waitTime}ms before retry.")
                    delay(waitTime)
                    retries++
                    continue
                } else if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                    Timber.tag(TAG).e("HTTP error code: $responseCode, body: $errorBody")

                    // Retry on server errors
                    if (responseCode >= 500) {
                        retries++
                        delay(5000L * (retries))
                        continue
                    }

                    return@withContext emptyList<MilitaryBase>()
                }

                // Read the response
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }

                // Parse the response
                return@withContext parseOverpassResponse(response)
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Error fetching military bases (attempt ${retries + 1}): ${e.message}", e)

                // Only retry on certain errors
                if (e is java.net.SocketTimeoutException || e is java.io.IOException) {
                    retries++
                    delay(5000L * (retries))
                    continue
                }

                return@withContext emptyList<MilitaryBase>()
            }
        }

        Log.e(TAG, "Failed to fetch military bases after $maxRetries attempts", lastException)
        return@withContext emptyList<MilitaryBase>()
    }

    /**
     * Builds an Overpass QL query to fetch military installations
     */
    /**
     * Builds an improved Overpass QL query to fetch military installations
     * Includes all military-related tags: military=*, military_service=*, landuse=military
     *
     * @param boundingBox Optional bounding box to limit search [south,west,north,east]
     * @param limit Maximum number of results to return
     *
     * Returns a string containing the Overpass QL query to fetch military installations.
     */
    private fun buildOverpassQuery(boundingBox: String?, limit: Int): String {
        val bbox = boundingBox ?: ""
        val bboxFilter = if (bbox.isNotEmpty()) "($bbox)" else ""

        return """
        [out:json][timeout:90];
        (
          // Military facilities
          node["military"${bboxFilter}];
          way["military"${bboxFilter}];
          relation["military"${bboxFilter}];
          
          // Military service branches
          node["military_service"${bboxFilter}];
          way["military_service"${bboxFilter}];
          relation["military_service"${bboxFilter}];
          
          // Military landuse
          node["landuse"="military"${bboxFilter}];
          way["landuse"="military"${bboxFilter}];
          relation["landuse"="military"${bboxFilter}];
        );
        out body center $limit;
        >;
        out skel;
    """.trimIndent()
    }

    /**
     * Parse the JSON response from Overpass API into MilitaryBase objects
     * Enhanced to handle military, military_service, and landuse=military tags
     *
     * @param response JSON response from Overpass API
     *
     * Returns a list of MilitaryBase objects parsed from the JSON response.
     */
    private fun parseOverpassResponse(response: String): List<MilitaryBase> {
        val militaryBases = mutableListOf<MilitaryBase>()

        try {
            val jsonResponse = JSONObject(response)
            val elements = jsonResponse.getJSONArray("elements")

            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val type = element.getString("type")

                // Skip if not a node, way with center, or relation with center
                if (type != "node" && !element.has("center")) continue

                // Get coordinates
                val lat =
                    if (type == "node") element.getDouble("lat") else element.getJSONObject("center")
                        .getDouble("lat")
                val lon =
                    if (type == "node") element.getDouble("lon") else element.getJSONObject("center")
                        .getDouble("lon")

                // Skip if coordinates are invalid
                if (lat == 0.0 && lon == 0.0) continue

                // Get tags if available
                val tags = if (element.has("tags")) element.getJSONObject("tags") else null
                if (tags == null) continue

                // Extract base properties from tags
                val name = tags.optString("name") ?: "Unnamed Military Site"

                // Check for military-related tags
                val isMilitary = tags.has("military")
                val hasMilitaryService = tags.has("military_service")
                val isMilitaryLand = tags.optString("landuse") == "military"

                // Skip if none of the military tags are present (shouldn't happen due to our query)
                if (!isMilitary && !hasMilitaryService && !isMilitaryLand) continue

                // Determine type from tags
                val militaryType = when {
                    isMilitary -> tags.optString("military", "base")
                    hasMilitaryService -> tags.optString("military_service", "unknown")
                    else -> "land" // landuse=military
                }

                // Try to determine country and state from various tags
                val country = tags.optString("addr:country", "USA") ?: "USA"
                val state = tags.optString("addr:state")
                val city = tags.optString("addr:city")

                // Create a unique ID
                val id = "osm-${type}-${element.getLong("id")}"

                // Determine active status - assume active unless explicitly marked otherwise
                val isActive = tags.optString("disused") != "yes" &&
                        tags.optString("abandoned") != "yes" &&
                        tags.optString("historic") != "yes"

                // Create MilitaryBase object
                val militaryBase = MilitaryBase(
                    id = id,
                    name = name,
                    type = militaryType.uppercase(),
                    branch = determineBranch(tags, militaryType),
                    latitude = lat,
                    longitude = lon,
                    city = city,
                    state = state,
                    country = country,
                    isActive = isActive,
                    establishedYear = tags.optString("start_date", "").take(4).toIntOrNull(),
                    hasAirfield = tags.has("aeroway") ||
                            militaryType == "airfield" ||
                            name.contains("air force", ignoreCase = true) ||
                            name.contains("airfield", ignoreCase = true) ||
                            name.contains("airport", ignoreCase = true),
                    hasResearchFacilities = militaryType == "research" ||
                            militaryType == "training_area" ||
                            name.contains("research", ignoreCase = true) ||
                            name.contains("laboratory", ignoreCase = true) ||
                            name.contains("test", ignoreCase = true),
                    hasRestrictedAirspace = militaryType == "airfield" ||
                            militaryType == "base" ||
                            militaryType == "danger_area",
                    dataSource = "OpenStreetMap",
                    lastUpdated = System.currentTimeMillis()
                )

                militaryBases.add(militaryBase)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Overpass response: ${e.message}", e)
        }

        Log.d(TAG, "Parsed ${militaryBases.size} military bases from OSM")
        return militaryBases
    }

    /**
     * Determine military branch from available tags with improved detection
     *
     * @param tags JSON object containing tags from Overpass API response
     * @param militaryType Type of military installation
     *
     * Returns a string representing the military branch of the installation.
     */
    private fun determineBranch(tags: JSONObject, militaryType: String): String {
        // Check explicit military_service tag first
        if (tags.has("military_service")) {
            val service = tags.optString("military_service").lowercase()
            return when {
                service.contains("air_force") || service.contains("air force") -> "Air Force"
                service.contains("army") -> "Army"
                service.contains("navy") || service.contains("naval") -> "Navy"
                service.contains("marine") -> "Marines"
                service.contains("coast_guard") || service.contains("coast guard") -> "Coast Guard"
                service.contains("space_force") || service.contains("space force") -> "Space Force"
                else -> service.split("_").joinToString(" ") { it.capitalize() }
            }
        }

        // Otherwise, try to determine from name or type
        val name = tags.optString("name", "").lowercase()
        val operator = tags.optString("operator", "").lowercase()

        return when {
            name.contains("air force") || name.contains("afb") ||
                    operator.contains("air force") || militaryType == "airfield" -> "Air Force"

            name.contains("army") || operator.contains("army") ||
                    militaryType == "barracks" -> "Army"

            name.contains("navy") || name.contains("naval") ||
                    operator.contains("navy") || militaryType == "naval_base" -> "Navy"

            name.contains("marine") || name.contains("usmc") ||
                    operator.contains("marine") -> "Marines"

            name.contains("coast guard") || operator.contains("coast guard") -> "Coast Guard"

            name.contains("space force") || operator.contains("space force") -> "Space Force"

            else -> "Military"
        }
    }
}
