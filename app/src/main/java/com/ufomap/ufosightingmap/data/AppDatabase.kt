package com.ufomap.ufosightingmap.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ufomap.ufosightingmap.data.correlation.dao.AstronomicalEventDao
import com.ufomap.ufosightingmap.data.correlation.dao.MilitaryBaseDao
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherEventDao
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.utils.DateTypeConverter

/**
 * Main database class for the application.
 * Contains tables for UFO sightings and correlation data sources.
 */
@Database(
    entities = [
        Sighting::class,
        MilitaryBase::class,
        AstronomicalEvent::class,
        WeatherEvent::class,
        PopulationData::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // Original DAO
    abstract fun sightingDao(): SightingDao

    // Correlation DAOs
    abstract fun militaryBaseDao(): MilitaryBaseDao
    abstract fun astronomicalEventDao(): AstronomicalEventDao
    abstract fun weatherEventDao(): WeatherEventDao
    abstract fun populationDataDao(): PopulationDataDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ufo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}