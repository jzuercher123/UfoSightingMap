package com.ufomap.ufosightingmap.data.sync

/**
 * Defines the different data types that can be synchronized
 */
enum class DataType {
    WEATHER,
    ASTRONOMICAL_EVENTS,
    POPULATION,
    MILITARY_BASES
}

/**
 * Defines update frequencies for different data types
 * Each frequency has an associated interval in milliseconds
 */
enum class UpdateFrequency(val intervalMillis: Long) {
    REAL_TIME(60_000),          // 1 minute
    FREQUENT(15 * 60_000),      // 15 minutes
    DAILY(24 * 60 * 60_000),    // Daily
    WEEKLY(7 * 24 * 60 * 60_000) // Weekly
}