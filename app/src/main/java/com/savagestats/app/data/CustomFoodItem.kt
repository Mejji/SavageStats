package com.savagestats.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.savagestats.app.data.nutrition.FoodItem

/**
 * User-created food entry stored in the main SavageDatabase.
 * Mirrors the USDA [com.savagestats.app.data.nutrition.FoodItem] schema
 * so custom foods can be merged into search results seamlessly.
 *
 * Uses [autoGenerate] PK starting from 1 — no collision risk with
 * the read-only USDA NutritionDatabase which lives in a separate DB file.
 */
@Entity(tableName = "custom_food_items")
data class CustomFoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sodium: Float = 0f
)

/**
 * Maps a [CustomFoodItem] to a [FoodItem] for unified search results.
 * Uses negative IDs to distinguish custom entries from USDA data,
 * and prefixes the name with ★ so users can tell them apart in the dropdown.
 */
fun CustomFoodItem.toFoodItem(): FoodItem = FoodItem(
    id = -id,
    name = "★ $name",
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    sodium = sodium
)
