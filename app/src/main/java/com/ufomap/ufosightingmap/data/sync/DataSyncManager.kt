package com.ufomap.ufosightingmap.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ufomap.ufosightingmap.data.repositories.AstronomicalEventRepository
import com.ufomap.ufosightingmap.data.repositories.MilitaryBaseRepository
import com.ufomap.ufosightingmap.data.repositories.PopulationDataRepository
import com.ufomap.ufosightingmap.data.repositories.WeatherEventRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central manager for coordinating data synchronization across the app
 */
class DataSyncManager(
    private val context: Context,
    private val weatherRepo: WeatherEventRepository,
    private val astronomicalRepo: AstronomicalEventRepository,
    private val populationRepo: PopulationDataRepository,
    private val militaryRepo: MilitaryBaseRepository,
    private val scope: CoroutineScope
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Track if any sync is in progress
    private val isSyncing = AtomicBoolean(false)

    // Network availability flow
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        // Monitor network connectivity
        setupNetworkMonitoring()

        // Start initial sync
        scope.launch {
            delay(1000) // Small delay to allow app to initialize
            val isConnected = _isNetworkAvailable.value
            if (isConnected) {
                syncDataIfNeeded()
            } else {
                Timber.d("Initial sync skipped due to no network connectivity")
            }
        }
    }

    /**
     * Setup network connectivity monitoring
     */
    private fun setupNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check initial state
        _isNetworkAvailable.value = isNetworkConnected(connectivityManager)

        // Register for network callbacks
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
                Timber.d("Network became available")

                // Try to sync data when network becomes available
                scope.launch {
                    syncDataIfNeeded()
                }
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = isNetworkConnected(connectivityManager)
                Timber.d("Network lost, remaining connections: ${_isNetworkAvailable.value}")
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Check if device has a network connection
     */
    private fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Synchronize data if not already syncing
     * Optionally limit to specific data types
     *
     * @param dataTypes Set of data types to sync, defaults to all
     */
    suspend fun syncDataIfNeeded(dataTypes: Set<DataType> = DataType.values().toSet()) {
        if (!_isNetworkAvailable.value) {
            Timber.d("Sync skipped: No network connectivity")
            _syncState.value = SyncState.Error("No network connectivity")
            return
        }

        if (!isSyncing.compareAndSet(false, true)) {
            Timber.d("Sync already in progress, skipping")
            return
        }

        try {
            _syncState.value = SyncState.Syncing
            Timber.d("Starting data synchronization for types: $dataTypes")

            // Execute syncs in parallel but with supervision
            supervisorScope {
                val syncJobs = dataTypes.map { dataType ->
                    async {
                        try {
                            when (dataType) {
                                DataType.WEATHER -> weatherRepo.refreshData()
                                DataType.ASTRONOMICAL_EVENTS -> astronomicalRepo.refreshData()
                                DataType.POPULATION -> populationRepo.refreshData()
                                DataType.MILITARY_BASES -> militaryRepo.refreshData()
                            }
                            Timber.d("Successfully synced $dataType")
                        } catch (e: Exception) {
                            Timber.e(e, "Error syncing $dataType")
                            throw e
                        }
                    }
                }

                // Wait for all syncs to complete
                syncJobs.awaitAll()
            }

            _syncState.value = SyncState.Success
            Timber.d("All data synchronized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error during data synchronization")
            _syncState.value = SyncState.Error(e.message ?: "Unknown error during sync")
        } finally {
            isSyncing.set(false)
        }
    }

    /**
     * Force synchronization of all data regardless of cache status
     */
    suspend fun forceSyncAll() {
        Timber.d("Forcing full data synchronization")
        weatherRepo.invalidateCache()
        astronomicalRepo.invalidateCache()
        populationRepo.invalidateCache()
        militaryRepo.invalidateCache()

        syncDataIfNeeded()
    }

    /**
     * States representing the current synchronization status
     */
    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Error(val message: String) : SyncState()
    }
}