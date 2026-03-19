package com.savagestats.app.data.nutrition

import androidx.room.Dao
import androidx.room.Query

@Dao
interface FoodDao {

    /**
     * FTS4 full-text search with tokenized query support.
     * The ViewModel passes a pre-formatted FTS query string (e.g., "fried* chicken*")
     * which matches documents containing ALL tokens in any order.
     */
    @Query("""
        SELECT food_items.* FROM food_items 
        JOIN food_items_fts ON food_items.id = food_items_fts.rowid 
        WHERE food_items_fts MATCH :ftsQuery 
        LIMIT 15
    """)
    suspend fun searchFoods(ftsQuery: String): List<FoodItem>

    /**
     * Rebuild the FTS index. Call once after createFromAsset()
     * if the pre-packaged DB doesn't include the FTS table data.
     */
    @Query("INSERT INTO food_items_fts(food_items_fts) VALUES('rebuild')")
    suspend fun rebuildFts()

    /**
     * Get total count of food items for debugging.
     */
    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodCount(): Int

    /**
     * Get sample foods for debugging - shows what's actually in the DB.
     */
    @Query("SELECT * FROM food_items LIMIT 5")
    suspend fun getSampleFoods(): List<FoodItem>
}
