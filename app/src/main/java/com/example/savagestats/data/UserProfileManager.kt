package com.example.savagestats.data

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
    val age: Int = 0,
    val weight: Float = 0f,   // kg
    val height: Float = 0f,   // cm
    val goal: String = ""     // e.g. "Build Muscle", "Athletic Conditioning", "Lose Fat"
)

class UserProfileManager(private val context: Context) {

    private fun sanitizeFloat(value: Float): Float =
        if (value.isFinite()) value else 0f

    private object Keys {
        val USER_AGE = intPreferencesKey("user_age")
        val USER_WEIGHT = floatPreferencesKey("user_weight")
        val USER_HEIGHT = floatPreferencesKey("user_height")
        val USER_GOAL = stringPreferencesKey("user_goal")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            age = prefs[Keys.USER_AGE] ?: 0,
            weight = prefs[Keys.USER_WEIGHT] ?: 0f,
            height = prefs[Keys.USER_HEIGHT] ?: 0f,
            goal = prefs[Keys.USER_GOAL] ?: ""
        )
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

    suspend fun updateProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_AGE] = profile.age
            prefs[Keys.USER_WEIGHT] = sanitizeFloat(profile.weight)
            prefs[Keys.USER_HEIGHT] = sanitizeFloat(profile.height)
            prefs[Keys.USER_GOAL] = profile.goal
        }
    }
}
