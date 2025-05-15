package com.ufomap.ufosightingmap.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repository that optimizes memory usage when processing large datasets
 */
class MemoryOptimizedRepository<T>(
    private val scope: CoroutineScope,
    private val tag: String = "MemoryOptimizedRepo"
) {
    private val _data = MutableStateFlow<List<T>>(emptyList())
    val data: StateFlow<List<T>> = _data.asStateFlow()

    private var fetchJob: Job? = null
    private val isActive = AtomicBoolean(false)

    /**
     * Start observing data from the source
     *
     * @param sourceFlow Flow that provides the data
     */
    fun startObserving(sourceFlow: Flow<List<T>>) {
        if (isActive.getAndSet(true)) {
            Timber.tag(tag).d("Already observing, skipping")
            return
        }

        fetchJob = scope.launch {
            Timber.tag(tag).d("Starting data observation")
            sourceFlow
                .catch { e ->
                    Timber.tag(tag).e(e, "Error in data flow")
                }
                .collect { items ->
                    Timber.tag(tag).d("Received ${items.size} items")
                    _data.value = items
                }
        }
    }

    /**
     * Stop observing the data source
     */
    fun stopObserving() {
        Timber.tag(tag).d("Stopping data observation")
        fetchJob?.cancel()
        fetchJob = null
        isActive.set(false)
    }

    /**
     * Process a batch of items with memory-efficient chunking
     *
     * @param items List of items to process
     * @param batchSize Size of each batch
     * @param processor Function to process each batch
     */
    suspend fun processBatch(
        items: List<T>,
        batchSize: Int = 100,
        processor: suspend (List<T>) -> Unit
    ) {
        Timber.tag(tag).d("Processing ${items.size} items in batches of $batchSize")

        items.chunked(batchSize).forEachIndexed { index, batch ->
            Timber.tag(tag).d("Processing batch ${index + 1}/${(items.size + batchSize - 1) / batchSize}")
            processor(batch)
            // Allow other operations to proceed between batches
            delay(10)

            // Manually trigger garbage collection on every 5th batch
            if (index % 5 == 4) {
                Timber.tag(tag).d("Suggesting garbage collection")
                System.gc()
            }
        }
    }

    /**
     * Use weak references to prevent memory leaks
     */
    inner class WeakProcessor<R>(processor: (T) -> R) {
        private val weakRef = WeakReference(processor)

        fun process(item: T): R? {
            return weakRef.get()?.invoke(item)
        }
    }
}