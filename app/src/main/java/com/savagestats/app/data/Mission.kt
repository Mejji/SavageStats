package com.savagestats.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class MissionTask(
    val name: String,
    var isDone: Boolean = false,
)

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val tasks: List<MissionTask>,
    val expiresAt: Long,
    val isCompleted: Boolean = false,
)
