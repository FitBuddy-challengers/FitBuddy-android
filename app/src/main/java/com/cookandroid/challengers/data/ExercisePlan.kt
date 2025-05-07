package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_plans") // 운동계획 (날짜별)
data class ExercisePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 운동 계획 ID (PK, 자동 생성)
    val plannedDate: Long = System.currentTimeMillis() // 계획한 날짜 (timestamp)
)