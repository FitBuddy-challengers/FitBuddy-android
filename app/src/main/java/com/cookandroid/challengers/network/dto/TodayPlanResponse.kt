package com.cookandroid.challengers.network.dto

data class TodayPlanResponse(
    val plan: PlanDto,
    val schedules: List<ScheduleDto>
)

data class PlanDto(
    val id: Int,
    val start_date: String,
    val end_date: String
    // 필요한 필드 추가
)

data class ScheduleDto(
    val id: Int,
    val exercise_id: Int,
    val exercise_order: Int,
    val is_completed: Boolean,
    val date: String,
    val exercise_plan_id: Int
)
