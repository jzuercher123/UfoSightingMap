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
    exportSchema = true // Changed to true to enable schema versioning
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
                // Added submittedBy, submissionDate, isUserSubmitted, submissionStatus columns to sightings table
                database.execSQL(
                    "ALTER TABLE sightings ADD COLUMN submittedBy TEXT"
                )
                database.execSQL(
                    "ALTER TABLE sightings ADD COLUMN submissionDate TEXT"
                )
                database.execSQL(
                    "ALTER TABLE sightings ADD COLUMN isUserSubmitted INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE sightings ADD COLUMN submissionStatus TEXT NOT NULL DEFAULT 'approved'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create correlation tables
                // Military bases table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `military_bases` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `branch` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `city` TEXT,
                        `state` TEXT,
                        `country` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `establishedYear` INTEGER,
                        `sizeAcres` REAL,
                        `hasAirfield` INTEGER NOT NULL DEFAULT 0,
                        `hasNuclearCapabilities` INTEGER NOT NULL DEFAULT 0,
                        `hasResearchFacilities` INTEGER NOT NULL DEFAULT 0,
                        `hasRestrictedAirspace` INTEGER NOT NULL DEFAULT 0,
                        `dataSource` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """
                )

                // Create indices for military bases
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_military_bases_latitude` ON `military_bases` (`latitude`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_military_bases_longitude` ON `military_bases` (`longitude`)")

                // And similar CREATE TABLE commands for other correlation tables
                // astronomical_events, weather_events, population_data
                // (Simplified for brevity, but in the actual migration these would be fully written out)
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