package com.cookandroid.challengers.network.dto

data class TodayPlanResponse(
    val plan: PlanDto,
    val schedules: List<ScheduleDto>
)

data class PlanDto(
    val id: Int,
    val start_date: String,
    val end_date: String,
    val day: List<Int>?,
    val day_pattern: List<String>?,
    val completed_days: List<String>?,
    val is_dummy: Boolean
)

data class ScheduleDto(
    val schedule_id: Int, // s.id
    val exercise_id: Int,
    val date: String,
    val exercise_order: Int,
    val is_completed: Boolean,
    val is_dummy: Boolean,

    val exercise_name: String,
    val part: String,
    val equip: String,
    val image_path: String?,
    val start_position: List<String>?,
    val exercise_motion: List<String>?,
    val breathing: List<String>?,
    val caution: List<String>?,
    val mets: Double,
    val is_time_type: Boolean,
    val is_noise: Boolean
)

//data class ScheduleDto(
//    val id: Int,
//    val exercise_id: Int,
//    val exercise: ExerciseDto
//)

data class ExerciseDto(
    val name: String,
    val part: String,
    val equip: String,
    val image_path: String?,
    val start_position: List<String>?,
    val exercise_motion: List<String>?,
    val breathing: List<String>?,
    val caution: List<String>?,
    val mets: Double,
    val is_time_type: Boolean,
    val is_noise: Boolean
)
