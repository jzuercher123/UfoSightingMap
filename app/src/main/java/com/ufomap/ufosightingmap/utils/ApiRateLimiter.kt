package com.ufomap.ufosightingmap.utils

import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Rate limiter that prevents making too many API calls in a short time period
 */
class ApiRateLimiter {
    private val requestTimestamps = ConcurrentHashMap<String, AtomicLong>()
    private val minIntervalMillis = ConcurrentHashMap<String, Long>()

    /**
     * Set a rate limit for a specific API key
     *
     * @param key Identifier for the API
     * @param intervalMillis Minimum time between requests in milliseconds
     */
    fun setRateLimit(key: String, intervalMillis: Long) {
        minIntervalMillis[key] = intervalMillis
        Timber.d("Rate limit set for $key: $intervalMillis ms")
    }

    /**
     * Execute a block with rate limiting applied
     * Will delay execution if needed to respect rate limits
     *
     * @param key Identifier for the API
     * @param block Suspend function to execute
     */
    suspend fun executeWithRateLimit(key: String, block: suspend () -> Unit) {
        val lastRequestTime = requestTimestamps.getOrPut(key) { AtomicLong(0) }.get()
        val interval = minIntervalMillis[key] ?: 0
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastRequestTime < interval) {
            val delayTime = interval - (currentTime - lastRequestTime)
            Timber.d("Rate limiting $key: Delaying for $delayTime ms")
            delay(delayTime)
        }

        try {
            block()
        } finally {
            requestTimestamps[key]?.set(System.currentTimeMillis())
        }
    }

    /**
     * Check if a request can be made without exceeding rate limits
     * Does not update timestamps
     *
     * @param key Identifier for the API
     * @return true if a request can be made, false if it would exceed rate limits
     */
    fun canRequest(key: String): Boolean {
        val lastRequestTime = requestTimestamps[key]?.get() ?: 0
        val interval = minIntervalMillis[key] ?: 0
        val currentTime = System.currentTimeMillis()

        return currentTime - lastRequestTime >= interval
    }
}