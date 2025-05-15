package com.ufomap.ufosightingmap.data.sync

import android.content.Context
import androidx.work.*
import com.ufomap.ufosightingmap.data.AppDatabase
import com.ufomap.ufosightingmap.data.repositories.AstronomicalEventRepository
import com.ufomap.ufosightingmap.data.repositories.MilitaryBaseRepository
import com.ufomap.ufosightingmap.data.repositories.PopulationDataRepository
import com.ufomap.ufosightingmap.data.repositories.WeatherEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for handling background data updates
 */
class DataUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dataTypeStr = inputData.getString("DATA_TYPE") ?: return@withContext Result.failure()
        val dataType = try {
            DataType.valueOf(dataTypeStr)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid data type: $dataTypeStr")
            return@withContext Result.failure()
        }

        Timber.d("Starting background update for $dataType")

        try {
            // Get database access
            val database = AppDatabase.getDatabase(applicationContext)

            // Select appropriate repository based on data type
            when (dataType) {
                DataType.WEATHER -> {
                    val repo = WeatherEventRepository(database.weatherEventDao(), applicationContext)
                    repo.refreshData()
                }
                DataType.ASTRONOMICAL_EVENTS -> {
                    val repo = AstronomicalEventRepository(database.astronomicalEventDao(), applicationContext)
                    repo.refreshData()
                }
                DataType.POPULATION -> {
                    val repo = PopulationDataRepository(database.populationDataDao(), applicationContext)
                    repo.refreshData()
                }
                DataType.MILITARY_BASES -> {
                    val repo = MilitaryBaseRepository(database.militaryBaseDao(), applicationContext)
                    repo.refreshData()
                }
            }

            Timber.d("Successfully updated $dataType data")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error updating $dataType data")
            // Retry up to 3 times on failure
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "DataUpdateWorker"

        /**
         * Schedule regular updates for all data types
         */
        fun scheduleUpdates(context: Context) {
            Timber.d("Scheduling regular data updates")
            scheduleUpdate(context, DataType.WEATHER, UpdateFrequency.REAL_TIME)
            scheduleUpdate(context, DataType.ASTRONOMICAL_EVENTS, UpdateFrequency.FREQUENT)
            scheduleUpdate(context, DataType.POPULATION, UpdateFrequency.DAILY)
            scheduleUpdate(context, DataType.MILITARY_BASES, UpdateFrequency.WEEKLY)
        }

        /**
         * Schedule a periodic work request for a specific data type
         */
        private fun scheduleUpdate(context: Context, dataType: DataType, frequency: UpdateFrequency) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val updateWork = PeriodicWorkRequestBuilder<DataUpdateWorker>(
                frequency.intervalMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf("DATA_TYPE" to dataType.name))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_${dataType.name.lowercase()}",
                ExistingPeriodicWorkPolicy.UPDATE,
                updateWork
            )

            Timber.d("Scheduled ${dataType.name} updates with frequency: ${frequency.name}")
        }
    }
}