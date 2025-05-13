package com.ufomap.ufosightingmap.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.SightingRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Utility class to manage database initialization.
 * This centralizes database setup operations and ensures they happen early in the app lifecycle.
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    private var isInitialized = false

    /**
     * Initializes the database with sample data if needed.
     * This should be called once during app startup.
     */
    fun initializeDatabase(context: Context) {
        if (isInitialized) return

        Timber.d("Starting database initialization")

        val database = AppDatabase.getDatabase(context)
        val sightingDao = database.sightingDao()
        val repository = SightingRepository(sightingDao, context)

        // Use the app lifecycle scope for initialization
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                // Check if database has data
                val count = sightingDao.count()
                Timber.d("Database contains $count sightings")

                if (count == 0) {
                    // Verify JSON asset exists
                    val assetFiles = context.assets.list("")
                    if (assetFiles?.contains("sightings.json") == true) {
                        val size = context.assets.open("sightings.json").available()
                        Timber.d("Found sightings.json ($size bytes), loading data")

                        // Force load data
                        repository.forceReloadData()

                        // Verify data was loaded
                        val newCount = sightingDao.count()
                        Timber.d("Database now contains $newCount sightings")
                    } else {
                        Timber.e("ERROR: sightings.json not found in assets folder!")
                    }
                } else {
                    Timber.d("Database already contains data, skipping initialization")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during database initialization")
            }

            isInitialized = true
        }
    }
}