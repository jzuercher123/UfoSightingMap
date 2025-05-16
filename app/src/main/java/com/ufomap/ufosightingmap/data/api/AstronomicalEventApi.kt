import com.ufomap.ufosightingmap.data.correlation.dao.AstronomicalEventDao
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data access object for astronomical events
 *
 */
private fun AstronomicalEventApi.getAstronomicalEvents() {
    TODO("Not yet implemented")
}

/**
 * Data access object for astronomical events
 *
 */
annotation class AstronomicalEventApi


/**
 * Repository for astronomical events
 *
 * @param dao Data access object for astronomical events
 * @param api API for astronomical events
 * @param scope Coroutine scope for launching coroutines
 *
 * The repository is responsible for managing the data for astronomical events.
 * It fetches the data from the API and updates the database.
 *
 * The repository is also responsible for managing the state of the update process.
 * It is a sealed class that represents the state of the update process.
 *
 */
class AstronomicalEventRepository(
    private val dao: AstronomicalEventDao,
    private val api: AstronomicalEventApi,
    private val scope: CoroutineScope
) {
    private val _events = MutableStateFlow<List<AstronomicalEvent>>(emptyList())
    val events: StateFlow<List<AstronomicalEvent>> = _events.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        // Initial load from database
        scope.launch {
            dao.getAllEvents().collect { dbEvents ->
                _events.value = dbEvents
            }
        }
    }

    /**
     * Refresh the data from the API and update the database
     * @param forceRefresh If true, refresh even if data is not stale
     *
     * If forceRefresh is true, refresh even if data is not stale.
     * If forceRefresh is false, refresh only if data is stale.
     *
     * Data is stale if it has not been updated in the last 24 hours.
     *
     * If data is stale, fetch from API and update database.
     *
     * If data is not stale, do nothing.
     */
    suspend fun refreshData(forceRefresh: Boolean = false) {
        // Don't update if already updating
        if (_updateState.value is UpdateState.Updating && !forceRefresh) return

        _updateState.value = UpdateState.Updating
        try {
            val lastUpdate = getLastUpdateTime()
            val STALE_THRESHOLD = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago

            // Fetch from API
            // If force refresh or last update is stale, fetch from API
            if (forceRefresh || lastUpdate == null || lastUpdate < STALE_THRESHOLD) {
                val newEvents = api.getAstronomicalEvents()
                dao.insertAll(newEvents)
                saveLastUpdateTime()
            }
            _updateState.value = UpdateState.Success
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Save the last update time to the database
     */
    private fun saveLastUpdateTime() {
        TODO("Not yet implemented")
    }

    /**
     * Get the last update time from the database
     */
    private fun getLastUpdateTime(): Long? {
        TODO("Not yet implemented")
    }

    /**
     * Represents the state of the update process
     *
     * Idle: No update is in progress
     * Updating: Update is in progress
     * Success: Update was successful
     * Error: Update failed with an error
     */
    sealed class UpdateState {
        object Idle : UpdateState()
        object Updating : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}