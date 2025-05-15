enum class UpdateFrequency(val intervalMillis: Long) {
    REAL_TIME(60_000),          // 1 minute - for weather data
    FREQUENT(15 * 60_000),      // 15 minutes - for astronomical events
    DAILY(24 * 60 * 60_000),    // Daily - for population data
    WEEKLY(7 * 24 * 60 * 60_000) // Weekly - for military bases
}

class DataUpdateManager(private val context: Context) {
    fun scheduleUpdates() {
        scheduleUpdate(DataType.WEATHER, UpdateFrequency.REAL_TIME)
        scheduleUpdate(DataType.ASTRONOMICAL_EVENTS, UpdateFrequency.FREQUENT)
        scheduleUpdate(DataType.POPULATION, UpdateFrequency.DAILY)
        scheduleUpdate(DataType.MILITARY_BASES, UpdateFrequency.WEEKLY)
    }

    private fun scheduleUpdate(dataType: DataType, frequency: UpdateFrequency) {
        // Implementation using WorkManager
    }
}