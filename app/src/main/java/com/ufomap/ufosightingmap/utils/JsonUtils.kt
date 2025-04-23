package com.ufomap.ufosightingmap.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ufomap.ufosightingmap.data.Sighting
import java.io.IOException

/**
 * Loads a list of UFO sightings from a JSON file in the assets directory.
 *
 * @param context The Android context to access assets
 * @param fileName The name of the JSON file in the assets directory
 * @return List of Sighting objects, or null if loading failed
 */
fun loadSightingsFromJson(context: Context, fileName: String): List<Sighting>? {
    return try {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        Log.d("JsonUtils", "Successfully read JSON file: $fileName")

        val sightingType = object : TypeToken<List<Sighting>>() {}.type
        val sightings = Gson().fromJson<List<Sighting>>(jsonString, sightingType)

        Log.d("JsonUtils", "Parsed ${sightings.size} sightings from JSON")
        sightings
    } catch (e: IOException) {
        Log.e("JsonUtils", "Error reading JSON file: ${e.message}", e)
        null
    } catch (e: Exception) {
        Log.e("JsonUtils", "Error parsing JSON data: ${e.message}", e)
        null
    }
}