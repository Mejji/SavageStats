package com.savagestats.app.data.nutrition

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Separate read-only Room database for verified USDA nutrition data.
 * Loaded from a pre-packaged asset file (database/savage_nutrition.db).
 * Kept separate from [com.savagestats.app.data.SavageDatabase] to avoid
 * destructive migrations wiping user logs/missions.
 */
@Database(
    entities = [FoodItem::class, FoodItemFts::class],
    version = 3,
    exportSchema = false
)
abstract class NutritionDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao

    companion object {
        private const val TAG = "NutritionDatabase"
        
        @Volatile
        private var INSTANCE: NutritionDatabase? = null

        fun getDatabase(context: Context): NutritionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutritionDatabase::class.java,
                    "savage_nutrition.db"
                )
                    .createFromAsset("database/savage_nutrition.db")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d(TAG, "Nutrition DB created from asset")
                // Rebuild FTS index after first creation from asset
                rebuildFtsIndex()
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d(TAG, "Nutrition DB opened")
            }

            private fun rebuildFtsIndex() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Wait for INSTANCE to be assigned
                        kotlinx.coroutines.delay(100)
                        val dao = INSTANCE?.foodDao() ?: return@launch
                        val foodCount = dao.getFoodCount()
                        Log.d(TAG, "Food items count: $foodCount")
                        
                        if (foodCount > 0) {
                            dao.rebuildFts()
                            Log.d(TAG, "FTS index rebuilt successfully")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "FTS rebuild error: ${e.message}", e)
                    }
                }
            }
        }
    }
}
