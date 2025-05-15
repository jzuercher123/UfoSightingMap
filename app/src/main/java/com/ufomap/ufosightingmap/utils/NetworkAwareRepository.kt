package com.ufomap.ufosightingmap.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Repository wrapper that is aware of network connectivity and can adapt its behavior accordingly.
 * Fetches from remote sources when network is available, or falls back to local data when offline.
 */
class NetworkAwareRepository<T>(
    private val context: Context,
    private val fetchRemote: suspend () -> List<T>,
    private val saveLocal: suspend (List<T>) -> Unit,
    private val getLocal: suspend () -> List<T>,
    private val tag: String = "NetworkAwareRepository"
) {
    /**
     * Get data with network awareness
     * If network is available and forceRefresh is true, fetch from remote
     * Otherwise, use local data
     *
     * @param forceRefresh Whether to force a refresh from remote source
     * @return List of data items
     */
    suspend fun getData(forceRefresh: Boolean = false): List<T> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = isNetworkAvailable(connectivityManager)

        return if (isConnected && forceRefresh) {
            try {
                Timber.tag(tag).d("Fetching data from remote source")
                fetchRemote().also {
                    Timber.tag(tag).d("Saving ${it.size} items to local storage")
                    saveLocal(it)
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error fetching remote data, falling back to local")
                getLocal()
            }
        } else {
            if (!isConnected) {
                Timber.tag(tag).d("No network connection, using local data")
            } else if (!forceRefresh) {
                Timber.tag(tag).d("Using local data (no force refresh)")
            }
            getLocal()
        }
    }

    /**
     * Observe network connectivity as a Flow
     *
     * @return Flow of Boolean indicating if network is available
     */
    fun observeNetworkConnectivity(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.tag(tag).d("Network became available")
                trySend(true)
            }

            override fun onLost(network: Network) {
                Timber.tag(tag).d("Network connection lost")
                trySend(isNetworkAvailable(connectivityManager))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        val currentState = isNetworkAvailable(connectivityManager)
        trySend(currentState)

        awaitClose {
            Timber.tag(tag).d("Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * Check if network is currently available
     */
    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Wait until network is available and then fetch data
     *
     * @param timeoutMillis Maximum time to wait for network in milliseconds, 0 for no timeout
     * @return List of data items, or empty list if timeout occurred
     */
    suspend fun getDataWhenNetworkAvailable(timeoutMillis: Long = 0): List<T> {
        val flow = observeNetworkConnectivity()

        // If timeout is specified, add a timeout mechanism
        val isNetworkAvailable = if (timeoutMillis > 0) {
            try {
                kotlinx.coroutines.withTimeout(timeoutMillis) {
                    // Wait until network is available
                    flow.first { it }
                }
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Timber.tag(tag).w("Timeout waiting for network")
                false
            }
        } else {
            // No timeout, just wait until network is available
            flow.first { it }
        }

        return if (isNetworkAvailable) {
            getData(forceRefresh = true)
        } else {
            getLocal()
        }
    }
}