package com.ufomap.ufosightingmap.utils

import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Generic caching mechanism that avoids unnecessary data fetches
 * by caching results and only fetching new data when the cache is stale.
 */
class SmartCache<T>(
    private val maxAgeMillis: Long,
    private val fetchData: suspend () -> T
) {
    private var lastUpdateTime: Long = 0
    private val cachedData = AtomicReference<T?>(null)

    /**
     * Get data from cache if fresh, otherwise fetch new data
     */
    suspend fun getData(): T {
        val currentTime = System.currentTimeMillis()
        val cachedValue = cachedData.get()

        return if (currentTime - lastUpdateTime > maxAgeMillis || cachedValue == null) {
            Timber.d("SmartCache: Cache miss or stale data, fetching fresh data")
            fetchData().also {
                cachedData.set(it)
                lastUpdateTime = currentTime
            }
        } else {
            Timber.d("SmartCache: Using cached data")
            cachedValue
        }
    }

    /**
     * Force the cache to be refreshed on next getData() call
     */
    fun invalidate() {
        Timber.d("SmartCache: Invalidating cache")
        lastUpdateTime = 0
    }

    /**
     * Clear the cached data entirely
     */
    fun clear() {
        Timber.d("SmartCache: Clearing cache")
        cachedData.set(null)
        lastUpdateTime = 0
    }
}