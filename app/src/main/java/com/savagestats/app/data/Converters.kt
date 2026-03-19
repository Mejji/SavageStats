package com.savagestats.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromMissionTaskList(tasks: List<MissionTask>): String =
        gson.toJson(tasks)

    @TypeConverter
    fun toMissionTaskList(json: String): List<MissionTask> {
        val type = object : TypeToken<List<MissionTask>>() {}.type
        return gson.fromJson(json, type)
    }
}
