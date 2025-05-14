package com.ufomap.ufosightingmap.utils

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import java.io.File

object OSMDroidManager {
    private const val TAG = "OSMDroidManager"

    /**
     * Configures OSMDroid with appropriate paths and settings.
     * Always call this before initializing any MapView.
     */
    fun configure(context: Context) {
        Log.d(TAG, "Configuring OSMDroid")

        try {
            // Get the Configuration instance
            val config = Configuration.getInstance()

            // Load default preferences
            config.load(context, PreferenceManager.getDefaultSharedPreferences(context))

            // Set user agent to app package name
            config.userAgentValue = context.packageName

            // Configure map tile download cache path (internal storage for reliability)
            val osmdroidDir = File(context.filesDir, "osmdroid")
            if (!osmdroidDir.exists()) {
                val created = osmdroidDir.mkdirs()
                Log.d(TAG, "OSMDroid directory created: $created")
            }

            val customTileSourcePath = File(context.filesDir, "osmdroid/tiles")
            customTileSourcePath.mkdirs()
            config.osmdroidTileCache = customTileSourcePath

            // For later versions, ensure the cache exists
            val tileCache = config.osmdroidTileCache
            if (tileCache != null && !tileCache.exists()) {
                val created = tileCache.mkdirs()
                Log.d(TAG, "Tile cache directory created: $created")
            }

            // Log success
            Log.d(TAG, "OSMDroid configured successfully")
            Log.d(TAG, "Tile cache path: ${config.osmdroidTileCache?.absolutePath}")
            Log.d(TAG, "User agent: ${config.userAgentValue}")

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring OSMDroid", e)
        }
    }

    /**
     * Clears the tile cache to free up space
     */
    fun clearCache(context: Context) {
        try {
            val tileCache = Configuration.getInstance().osmdroidTileCache
            tileCache?.deleteRecursively()
            Log.d(TAG, "OSMDroid cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing OSMDroid cache", e)
        }
    }
}

annotation class OSMDroidTester
