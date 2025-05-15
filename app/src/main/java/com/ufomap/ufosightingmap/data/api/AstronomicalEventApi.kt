class AstronomicalEventRepository(
    private val dao: AstronomicalEventDao,
    private val api: AstronomicalEventApi,
    private val scope: CoroutineScope
) {
    private val _events = MutableStateFlow<List<AstronomicalEvent>>(emptyList())
    val events: StateFlow<List<AstronomicalEvent>> = _events

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        // Initial load from database
        scope.launch {
            dao.getAllEvents().collect { dbEvents ->
                _events.value = dbEvents
            }
        }
    }

    suspend fun refreshData(forceRefresh: Boolean = false) {
        // Don't update if already updating
        if (_updateState.value is UpdateState.Updating && !forceRefresh) return

        _updateState.value = UpdateState.Updating
        try {
            val lastUpdate = getLastUpdateTime()
            // Only fetch if data is stale
            if (forceRefresh || System.currentTimeMillis() - lastUpdate > STALE_THRESHOLD) {
                val newEvents = api.getAstronomicalEvents()
                dao.insertAll(newEvents)
                saveLastUpdateTime()
            }
            _updateState.value = UpdateState.Success
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Updating : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}