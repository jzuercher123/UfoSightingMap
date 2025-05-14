package com.ufomap.ufosightingmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.ufomap.ufosightingmap.ui.UFOSightingsNavGraph
import com.ufomap.ufosightingmap.ui.theme.UfoSightingMapTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.File
import timber.log.Timber

/**
 * Main entry point for the UFO Sighting Map application.
 * Responsible for:
 * - Setting up OSMDroid configuration
 * - Requesting location permissions
 * - Initializing the Compose UI with navigation
 */
class MainActivity : ComponentActivity() {


    // Permission request handler for location access
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Timber.tag("Permission").i("Location permission granted")
            } else {
                Timber.tag("Permission").w("Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().isMapViewHardwareAccelerated = true
        Configuration.getInstance().tileDownloadThreads = 4
        Configuration.getInstance().tileDownloadMaxQueueSize = 32
        Configuration.getInstance().tileFileSystemThreads = 4
        // Create directories for the tile cache
        val osmConfig = Configuration.getInstance()
        val basePath = File(applicationContext.cacheDir.absolutePath, "osmdroid")
        basePath.mkdirs()
        val tileCache = File(basePath, "tiles")
        tileCache.mkdirs()

// Set explicit cache paths and permissions
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = tileCache

// Set a specific user agent
        osmConfig.userAgentValue = applicationContext.packageName + "/" + BuildConfig.VERSION_NAME

// Then proceed with your existing configuration
        osmConfig.load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        // Configure OSMDroid
        Configuration.getInstance().apply {
            load(
                applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )
            // Set user agent to app package name
            userAgentValue = applicationContext.packageName
        }

        Timber.tag("MainActivity")
            .i("OSMDroid configuration loaded. User Agent: ${Configuration.getInstance().userAgentValue}")

        // Request location permission for user location features
        requestLocationPermission()

        // Set up the Compose UI with navigation
        setContent {
            UfoSightingMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create and remember navigation controller
                    val navController = rememberNavController()

                    // Set up navigation graph
                    UFOSightingsNavGraph(navController)
                }
            }
        }
    }

    /**
     * Request location permission if not already granted
     */
    private fun requestLocationPermission() {
        when {
            // Permission already granted
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.tag("Permission").i("Location permission already granted")
            }

            // Show rationale if needed
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Timber.tag("Permission").w("Showing rationale for location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Request permission
            else -> {
                Timber.tag("Permission").i("Requesting location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("MainActivity").d("onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").d("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("MainActivity").d("onDestroy")
    }
}