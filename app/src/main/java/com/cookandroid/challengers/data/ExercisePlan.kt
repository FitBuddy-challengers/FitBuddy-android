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
    val id: Long = 0, // 자동생성되는 id
    val plannedDate: Long // 날짜를 나타냄(특정 날짜의 자정시각)
)
