package com.savagestats.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DailyLog::class, Mission::class, CustomFoodItem::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SavageDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao
    abstract fun missionDao(): MissionDao
    abstract fun customFoodDao(): CustomFoodDao

    companion object {
        @Volatile
        private var INSTANCE: SavageDatabase? = null

        fun getDatabase(context: Context): SavageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SavageDatabase::class.java,
                    "savage_stats_database"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
