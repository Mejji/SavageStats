package com.example.savagestats.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyLog::class], version = 2, exportSchema = false)
abstract class SavageDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao

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
