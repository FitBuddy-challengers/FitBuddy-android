package com.cookandroid.challengers.api

import com.cookandroid.challengers.api.dto.ConsultResponse
import retrofit2.Call
import com.cookandroid.challengers.model.AiRoutineRequest
import com.cookandroid.challengers.model.UserInfo
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AiRoutineApi {
    @Headers("Content-Type: application/json")
    @POST("api/generate-routine")
    suspend fun generateRoutine(
        @Body request: AiRoutineRequest
    ): Response<AiRoutineResponse>

    @GET("api/user-info/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: Int): Response<UserInfo>

    @Headers("Content-Type: application/json")
    /*
    * 여기 수정했습니다! 기존 서버로부터 json 형식으로 가져와서 이제 ai로 부터 받은 루틴이 db에 저장되지 않은 문제를
    * 잡아보았습니다:) -윤지-
    * */
    @POST("api/plan/submit-ai")
    suspend fun submitAi(
        @Body request: SubmitAiRequest
    ): Response<SubmitAiResponse>

    //운동 상담 api 추가
    @Headers("Content-Type: application/json")
    @POST("api/chat/consult")
    suspend fun consult(
        @Body body: Map<String, String>
    ): Response<ConsultResponse>

//    data class ConsultResponse(
//        val answer: String
//    )
    @Headers("Content-Type: application/json")
    @POST("api/recommend-exercise")
    suspend fun recommend(
        @Body request: RetrofitClient.RecommendRequest
    ): Response<RecommendResponse>

//    @Headers("Content-Type: application/json")
//    @POST("api/chat/recommend")
//    suspend fun recommend(
//        @Body body: Map<String, Any>
//    ): Response<RecommendResponse>

    data class RecommendResponse(
        val routine_text: String,
        val exercises: List<AiExercise>
    )

    // ✅ 추가된 부분
    @Headers("Content-Type: application/json")
    @POST("api/chat/welcome")
    suspend fun getWelcomeMessage(@Body body: Map<String, String>): Response<WelcomeResponse>

    @GET("api/check-plan")
    suspend fun checkExistingPlan(
        @Query("user_id") userId: Int,
        @Query("date") date: String
    ): Response<CheckPlanResponse>

    data class CheckPlanResponse(
        val exists: Boolean,
        val plan_id: Long? = null
    )


    @DELETE("api/dummy-plan")
    suspend fun deleteDummyPlan(
        @Query("user_id") userId: Int
    ): Response<DeleteDummyResponse>

    data class DeleteDummyResponse(
        val success: Boolean
    )





    data class ExistingPlanResponse(
        val exists: Boolean,
        val plan_id: Long?
    )

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

data class WelcomeResponse(
    val message: String
)

data class SubmitAiRequest(
    @SerializedName("user_id") val userId: Int,
    val plans: List<PlanData>   // or 너희가 서버에서 요구하는 필드
)

data class PlanData(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("days") val days: List<String>,
    @SerializedName("focus_area") val focusArea: String,
    @SerializedName("exercises") val exercises: List<AiExercise>
)

data class SubmitAiResponse(
    val routine_text: String,
    val saved_plan_id: Long,
    val schedules: List<ScheduleResponse>,
    val reps_sets: List<RepsSetResponse>,
    val time_sets: List<TimeSetResponse>
)

data class ScheduleResponse(
    @SerializedName("id") val scheduleId: Long,
    @SerializedName("exercise_id") val exerciseId: Long,
    @SerializedName("exercise_order") val exerciseOrder: Int
)

data class RepsSetResponse(
    val schedule_id: Long,
    val set_number: Int,
    val reps: Int,
    val weight: Int?
)

data class TimeSetResponse(
    val schedule_id: Long,
    val set_number: Int,
    val elapsed_time_millis: Long
)
