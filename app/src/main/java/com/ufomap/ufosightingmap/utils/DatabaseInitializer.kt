package com.ufomap.ufosightingmap.utils

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner // ADDED IMPORT
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.SightingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DatabaseInitializer(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("DatabaseInitializer worker started.")
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = SightingRepository(database.sightingDao(), applicationContext)

            // Use a CoroutineScope tied to the application's lifecycle if possible,
            // or a custom scope for background work.
            // For one-off initialization, Dispatchers.IO is fine.
            // For observing lifecycle, you'd typically do this in Application class or similar.
            repository.initializeDatabaseIfNeeded(CoroutineScope(Dispatchers.IO))
            Timber.d("Database initialization check complete.")
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error during database initialization worker.")
            return Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "DatabaseInitializerWorker"

        fun schedule(context: Context) {
            // Ensure this is only scheduled once or uses a unique name with REPLACE policy
            // ProcessLifecycleOwner.get().lifecycleScope.launch { // CORRECTED: Use ProcessLifecycleOwner.get().lifecycleScope
            // The above line is not ideal for scheduling WorkManager tasks.
            // Scheduling should typically happen at application start.
            // The companion object itself doesn't have a lifecycle owner.
            // Let's assume scheduling happens from Application.onCreate() or similar.

            val workRequest = PeriodicWorkRequestBuilder<DatabaseInitializer>(1, TimeUnit.DAYS)
                // Add constraints if needed, e.g. .setConstraints(Constraints.Builder()...build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Or REPLACE if you want it to run if already scheduled
                workRequest
            )
            Timber.d("DatabaseInitializer worker scheduled.")
        }

        // Example of how to observe application lifecycle if needed elsewhere
        fun observeApplicationLifecycle(context: Context) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    Timber.d("Application came to foreground.")
                    // You could trigger some logic here if needed
                    // For instance, re-check database initialization if it's not a worker
                }
            })
        }
    }
}