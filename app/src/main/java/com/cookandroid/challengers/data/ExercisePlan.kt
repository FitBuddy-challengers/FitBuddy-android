package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_plans",
    indices = [Index(value = ["plannedDate"], unique = true)]
)
data class ExercisePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plannedDate: Long
)
