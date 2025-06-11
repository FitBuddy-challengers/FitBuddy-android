package com.cookandroid.challengers.api

import retrofit2.Call
import com.cookandroid.challengers.model.AiRoutineRequest
import com.cookandroid.challengers.model.AiRoutineResponse
import com.cookandroid.challengers.model.UserInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface AiRoutineApi {
    @Headers("Content-Type: application/json")
    @POST("api/generate-routine")
    fun generateRoutine(@Body request: AiRoutineRequest): Call<AiRoutineResponse>

    @GET("api/user-info/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: Int): Response<UserInfo>

    @POST("/api/plan/submit-ai")
    suspend fun submitAiPlan(@Body request: AiPlanRequest): Response<AiPlanResponse>

}

data class AiPlanRequest(
    val user_id: Int,
    val start_date: String,
    val end_date: String,
    val exercises: List<AiExercise>
)

data class AiExercise(
    val name: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val seconds: Int? = null
)
data class AiPlanResponse(
    val plan_id: Long,
    val schedules: List<ExerciseSchedule>,
    val reps: List<ExerciseReps>,
    val times: List<ExerciseTime>
)

// 예시로 세부 클래스도 정의 (DB 컬럼 기준)
data class ExerciseSchedule(
    val id: Int,
    val exercise_plan_id: Int,
    val date: String,
    val exercise_order: Int,
    val is_completed: Boolean,
    val is_dummy: Boolean,
    val exercise_id: Int
)

data class ExerciseReps(
    val id: Int,
    val schedule_id: Int,
    val exercise_id: Int,
    val set_number: Int,
    val weight: Int,
    val reps: Int,
    val is_completed: Boolean
)

data class ExerciseTime(
    val id: Int,
    val schedule_id: Int,
    val exercise_id: Int,
    val elapsed_time_millis: Long,
    val is_completed: Boolean,
    val set_number: Int,
    val weight: Int
)
