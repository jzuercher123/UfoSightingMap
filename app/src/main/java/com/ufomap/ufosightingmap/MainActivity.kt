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
                Log.i("Permission", "Location permission granted")
            } else {
                Log.w("Permission", "Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid - UPDATED CONFIGURATION
        val ctx = applicationContext
        Configuration.getInstance().apply {
            // Load defaults
            load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

            // Set explicit cache location
            osmdroidTileCache = File(ctx.cacheDir, "osmdroid")

            // Ensure base path is set
            osmdroidBasePath = ctx.getExternalFilesDir(null)

            // Set user agent to app package name
            userAgentValue = ctx.packageName

            // Enable extra debugging
            isDebugMode = true
            isDebugMapView = true
            isDebugTileProviders = true
            isDebugMapTileDownloader = true
        }

        Log.i("MainActivity", "OSMDroid configuration loaded. User Agent: ${Configuration.getInstance().userAgentValue}")
        Log.i("MainActivity", "OSMDroid cache location: ${Configuration.getInstance().osmdroidTileCache}")

        // Request location permissions
        requestLocationPermission()

        // Also request storage permissions if needed
        requestStoragePermissions()

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
                Log.i("Permission", "Location permission already granted")
            }

            // Show rationale if needed
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.w("Permission", "Showing rationale for location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Request permission
            else -> {
                Log.i("Permission", "Requesting location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Request storage permissions for tile caching
     */
    private fun requestStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.i("Permission", "Storage permission granted: $isGranted")
                }.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
    }
}