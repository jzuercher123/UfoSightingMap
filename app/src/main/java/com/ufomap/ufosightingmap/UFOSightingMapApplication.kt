package com.ufomap.ufosightingmap

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.SightingRepository
import com.ufomap.ufosightingmap.data.repositories.AstronomicalEventRepository
import com.ufomap.ufosightingmap.data.repositories.MilitaryBaseRepository
import com.ufomap.ufosightingmap.data.repositories.PopulationDataRepository
import com.ufomap.ufosightingmap.data.repositories.WeatherEventRepository
import com.ufomap.ufosightingmap.data.sync.DataSyncManager
import com.ufomap.ufosightingmap.data.sync.DataUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import timber.log.Timber
import java.io.File

/**
 * Main application class for UFO Sighting Map app.
 * Handles global initialization, logging setup, and database preparation.
 */
class UFOSightingMapApplication : Application() {
    // Application-level coroutine scope for tasks that should survive configuration changes
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Tag for logging
    private val TAG = "UFOSightingMapApp"

    // Add this property
    private lateinit var dataSyncManager: DataSyncManager

    override fun onCreate() {
        super.onCreate()

        // Initialize logging system
        setupLogging()

        // Pre-configure OSMDroid to ensure it's set up before any map views
        setupOSMDroid()

        // Initialize database and preload data
        initializeDatabase()

        // Initialize data synchronization
        initializeDataSync()
    }

    /**
     * Sets up logging configuration based on build type
     */
    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            // Plant Timber debug tree for better logging in debug builds
            Timber.plant(Timber.DebugTree())
            Timber.d("Logging initialized in debug mode")
        } else {
            // In production, we could configure crash reporting or a custom logging tree
            // but keep it simple for now
        }
    }

    /**
     * Setup OSMDroid configuration at application level
     */
    private fun setupOSMDroid() {
        // Setup OSMDroid configuration early
        try {
            Configuration.getInstance().apply {
                // Set user agent early to avoid initialization issues
                userAgentValue = packageName

                // Set explicit tile cache path
                osmdroidTileCache = File(cacheDir, "osmdroid")

                // Ensure base path is set
                osmdroidBasePath = getExternalFilesDir(null)

                // Enable debugging in debug builds
                isDebugMode = BuildConfig.DEBUG
                isDebugMapView = BuildConfig.DEBUG
                isDebugTileProviders = BuildConfig.DEBUG
                isDebugMapTileDownloader = BuildConfig.DEBUG
            }
            Timber.d("OSMDroid pre-configured in Application")
        } catch (e: Exception) {
            Timber.e(e, "Error pre-configuring OSMDroid")
        }
    }

    /**
     * Initializes the database and ensures sample data is loaded
     */
    private fun initializeDatabase() {
        Timber.d("Starting database initialization")
        applicationScope.launch {
            try {
                // Get database references
                val database = AppDatabase.getDatabase(applicationContext)
                val sightingDao = database.sightingDao()
                val repository = SightingRepository(sightingDao, applicationContext)

                // Verify that assets are accessible
                verifyAssets()

                // Check if database is empty and needs sample data
                val count = sightingDao.count()
                Timber.d("Database contains $count sightings")

                if (count == 0) {
                    Timber.d("Database empty, loading sample data from JSON assets")
                    repository.forceReloadData()
                    Timber.d("Data loaded, now contains ${sightingDao.count()} sightings")
                } else {
                    Timber.d("Database already contains data, no loading needed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing database")
                Log.e(TAG, "Failed to initialize database: ${e.message}", e)
            }
        }
    }

    /**
     * Verifies that required asset files exist and are accessible
     */
    private fun verifyAssets() {
        try {
            // List all assets in the root directory
            val assetFiles = assets.list("")
            Timber.d("Available assets: ${assetFiles?.joinToString()}")

            // Check specifically for sightings.json
            if (assetFiles?.contains("sightings.json") == true) {
                val fileSize = assets.open("sightings.json").available()
                Timber.d("Found sightings.json (${fileSize} bytes)")

                // Validate file content by reading a sample
                val sample = assets.open("sightings.json").bufferedReader().use {
                    it.readText().take(100)
                }
                Timber.d("JSON content starts with: $sample...")
            } else {
                Timber.e("CRITICAL: sightings.json not found in assets folder!")
                Log.e(TAG, "CRITICAL: sightings.json not found in assets folder!")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error verifying assets")
            Log.e(TAG, "Failed to verify assets: ${e.message}", e)
        }
    }

    private fun initializeDataSync() {
        Timber.d("Initializing data synchronization system")

        // Initialize repositories if needed
        val database = AppDatabase.getDatabase(applicationContext)
        val weatherRepo = WeatherEventRepository(database.weatherEventDao(), applicationContext)
        val astronomicalRepo = AstronomicalEventRepository(database.astronomicalEventDao(), applicationContext)
        val populationRepo = PopulationDataRepository(database.populationDataDao(), applicationContext)
        val militaryRepo = MilitaryBaseRepository(database.militaryBaseDao(), applicationContext)

        // Schedule background updates
        DataUpdateWorker.scheduleUpdates(applicationContext)

        // Initialize sync manager
        dataSyncManager = DataSyncManager(
            context = applicationContext,
            weatherRepo = weatherRepo,
            astronomicalRepo = astronomicalRepo,
            populationRepo = populationRepo,
            militaryRepo = militaryRepo,
            scope = applicationScope
        )

        // Perform initial sync if needed
        applicationScope.launch {
            dataSyncManager.syncDataIfNeeded()
        }

        Timber.d("Data synchronization system initialized")
    }
}
