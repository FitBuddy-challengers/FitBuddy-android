package com.cookandroid.challengers.model

data class AiRoutineRequest(
    val user_info: UserInfo,
    val schedule_info: ScheduleInfo
)

data class UserInfo(
    val name: String,
    val age_group: String,
    val gender: String,
    val height: Int,
    val weight: Int,
    val disease: String,
    val exercise_level: String,
    val preferred_exercises: List<String>,
    val exercise_equipment: List<String>
)

data class ScheduleInfo(
    var start_date: String,
    var end_date: String,
    var days_of_week: List<String>,
    var focus_area: String = ""
)