package com.cookandroid.challengers.data

// ❌ 제거: @Entity(tableName = "exercise_sets")
// 그냥 서버 전송용 data class로 사용
data class ExerciseSetEntity(
    val id: Long = 0,
    val scheduleId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int,
    val weight: Int
)
