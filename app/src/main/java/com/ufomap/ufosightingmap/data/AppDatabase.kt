package com.ufomap.ufosightingmap.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ufomap.ufosightingmap.data.correlation.dao.AstronomicalEventDao
import com.ufomap.ufosightingmap.data.correlation.dao.MilitaryBaseDao
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao
import com.ufomap.ufosightingmap.data.correlation.dao.WeatherEventDao
import com.ufomap.ufosightingmap.data.correlation.models.AstronomicalEvent
import com.ufomap.ufosightingmap.data.correlation.models.MilitaryBase
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData
import com.ufomap.ufosightingmap.data.correlation.models.WeatherEvent
import com.ufomap.ufosightingmap.utils.DateTypeConverter

@Database(
    entities = [
        Sighting::class,
        MilitaryBase::class,
        AstronomicalEvent::class,
        WeatherEvent::class,
        PopulationData::class
    ],
    version = 3,
    exportSchema = true
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

        // Define migrations
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration code...
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration code...
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ufo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Use proper migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}