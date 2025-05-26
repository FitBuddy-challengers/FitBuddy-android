package com.cookandroid.challengers.network.dto

import com.google.gson.annotations.SerializedName

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
    val is_noise: Boolean,

    val set_count: Int? = null,
    val display_detail: String? = null,
    val seconds: Int?
)

//data class ScheduleDto(
//    val id: Int,
//    val exercise_id: Int,
//    val exercise: ExerciseDto
//)

data class ExerciseDto(
    val id: Long, // ✅ 이 줄 추가
    val name: String,
    val part: String,
    val equip: String,
    val image_path: String?,
    val start_position: List<String>?,
    val exercise_motion: List<String>?,
    val breathing: List<String>?,
    val caution: List<String>?,
    val mets: Double,
    @SerializedName("is_time_type")
    val isTimeType: Boolean,
    val is_noise: Boolean
)

data class ExerciseSetDto(
    val setNumber: Int,
    val weight: Int,
    val reps: Int,
    @SerializedName("is_completed") val isCompleted: Boolean = false
)

data class AddExerciseRequest(
    val exerciseId: Int,
    val setList: List<SetData>
)

data class SetData(
    val setNumber: Int,
    val reps: Int? = null,
    val weight: Int? = null,
    val seconds: Int? = null
)

data class ServerExerciseSet(
    val setNumber: Int,
    val weight: Int = 0,
    val reps: Int = 0,
    val isCompleted: Boolean = false,
    val isHighlighted: Boolean = false
)

