package com.savagestats.app.data.nutrition

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = FoodItem::class)
@Entity(tableName = "food_items_fts")
data class FoodItemFts(
    val name: String
)
