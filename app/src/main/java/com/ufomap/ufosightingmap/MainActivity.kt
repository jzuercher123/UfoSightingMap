package com.ufomap.ufosightingmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.ufomap.ufosightingmap.ui.MapScreen
import com.ufomap.ufosightingmap.ui.theme.UfoSightingMapTheme
import com.ufomap.ufosightingmap.viewmodel.MapViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels()

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

        // osmdroid Configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        // Setting a user agent - using packageName instead of BuildConfig.APPLICATION_ID
        Configuration.getInstance().userAgentValue = applicationContext.packageName
        Log.i("MainActivity", "osmdroid configuration loaded. User Agent: ${Configuration.getInstance().userAgentValue}")

        requestLocationPermission()

        // Set the Compose content for the activity
        setContent {
            UfoSightingMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(mapViewModel)
                }
            }
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("Permission", "Location permission already granted")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.w("Permission", "Showing rationale for location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                Log.i("Permission", "Requesting location permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}