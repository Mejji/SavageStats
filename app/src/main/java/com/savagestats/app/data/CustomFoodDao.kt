package com.savagestats.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomFoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: CustomFoodItem)

    /**
     * Case-insensitive LIKE search across custom food names.
     * The ViewModel wraps the user query with '%' wildcards before calling.
     */
    @Query("SELECT * FROM custom_food_items WHERE name LIKE :query ORDER BY name ASC LIMIT 15")
    suspend fun searchFoods(query: String): List<CustomFoodItem>

    @Query("SELECT * FROM custom_food_items ORDER BY name ASC")
    suspend fun getAllFoods(): List<CustomFoodItem>

    @Query("DELETE FROM custom_food_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
