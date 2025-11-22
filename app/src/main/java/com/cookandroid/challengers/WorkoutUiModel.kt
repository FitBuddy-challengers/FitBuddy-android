package com.cookandroid.challengers


data class WorkoutUiModel(
    val scheduleId: Long,
    val name: String,
    val reps: Int? = null,        // 반복 운동
    val seconds: Int? = null,     // 시간 운동
    val sets: Int,
    val isCompleted: Boolean = false
)

//data class WorkoutUiModel(
//    val scheduleId: Long,
//    val name: String,
//    val reps: Int,
//    val sets: Int,
//    var isCompleted: Boolean = false
//)