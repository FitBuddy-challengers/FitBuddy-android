package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_plans")
// 운동 계획: 날짜별 그룹핑
// 한 계획에는 여러 운동이 포함됩니다.
data class ExercisePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 운동 계획 ID (PK, 자동 생성)
    val plannedDate: Long = System.currentTimeMillis() // 계획한 날짜 (timestamp)
)