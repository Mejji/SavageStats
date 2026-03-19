package com.savagestats.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

data class UserProfile(
    val name: String = "",    // user's display name
    val age: Int = 0,
    val weight: Float = 0f,   // kg
    val height: Float = 0f,   // cm
    val goal: String = "",    // e.g. "Build Muscle", "Athletic Conditioning", "Lose Fat"
    val targetWeight: Float = 0f,  // kg
    val savageXp: Int = 0,
)

class UserProfileManager(private val context: Context) {

    private fun sanitizeFloat(value: Float): Float =
        if (value.isFinite()) value else 0f

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_WEIGHT = floatPreferencesKey("user_weight")
        val USER_HEIGHT = floatPreferencesKey("user_height")
        val USER_GOAL = stringPreferencesKey("user_goal")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight")
        val SAVAGE_XP = intPreferencesKey("savage_xp")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.USER_NAME] ?: "",
            age = prefs[Keys.USER_AGE] ?: 0,
            weight = prefs[Keys.USER_WEIGHT] ?: 0f,
            height = prefs[Keys.USER_HEIGHT] ?: 0f,
            goal = prefs[Keys.USER_GOAL] ?: "",
            targetWeight = prefs[Keys.TARGET_WEIGHT] ?: 0f,
            savageXp = prefs[Keys.SAVAGE_XP] ?: 0,
        )
    }

    val isProfileComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.USER_NAME] ?: ""
        val age = prefs[Keys.USER_AGE] ?: 0
        val weight = prefs[Keys.USER_WEIGHT] ?: 0f
        val height = prefs[Keys.USER_HEIGHT] ?: 0f
        val goal = prefs[Keys.USER_GOAL] ?: ""
        name.isNotBlank() && age > 0 && weight > 0f && height > 0f && goal.isNotBlank()
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
        }
    }

    suspend fun updateAge(age: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_AGE] = age
        }
    }

    suspend fun updateWeight(weight: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_WEIGHT] = sanitizeFloat(weight)
        }
    }

    suspend fun updateHeight(height: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_HEIGHT] = sanitizeFloat(height)
        }
    }

    suspend fun updateGoal(goal: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_GOAL] = goal
        }
    }

    suspend fun updateTargetWeight(targetWeight: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TARGET_WEIGHT] = sanitizeFloat(targetWeight)
        }
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVAGE_XP] ?: 0
            prefs[Keys.SAVAGE_XP] = current + amount
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = profile.name
            prefs[Keys.USER_AGE] = profile.age
            prefs[Keys.USER_WEIGHT] = sanitizeFloat(profile.weight)
            prefs[Keys.USER_HEIGHT] = sanitizeFloat(profile.height)
            prefs[Keys.USER_GOAL] = profile.goal
            prefs[Keys.TARGET_WEIGHT] = sanitizeFloat(profile.targetWeight)
            prefs[Keys.SAVAGE_XP] = profile.savageXp
        }
    }
}

/** Calculate the user's Savage Rank based on XP and weight progress. */
fun calculateSavageRank(currentWeight: Float, targetWeight: Float, xp: Int): String {
    val weightGoalMet = targetWeight > 0f && currentWeight > 0f &&
            kotlin.math.abs(currentWeight - targetWeight) <= 2f // within 2kg tolerance

    return when {
        weightGoalMet && xp >= 1000 -> "Savage God"
        xp >= 600 -> "Local Threat"
        xp >= 300 -> "Iron Novice"
        xp >= 100 -> "Couch Predator"
        else -> "Uncooked Noodle"
    }
}

/** XP thresholds for each rank, used for progress bar calculation. */
fun xpForNextRank(xp: Int): Pair<Int, Int> {
    return when {
        xp >= 1000 -> 1000 to 1000 // max rank
        xp >= 600 -> 600 to 1000
        xp >= 300 -> 300 to 600
        xp >= 100 -> 100 to 300
        else -> 0 to 100
    }
}
