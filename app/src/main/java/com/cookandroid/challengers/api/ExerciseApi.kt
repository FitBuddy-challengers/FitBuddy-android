package com.cookandroid.challengers.api

import com.cookandroid.challengers.data.ExerciseSetEntity
import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import com.cookandroid.challengers.network.dto.ExerciseDto
import com.cookandroid.challengers.network.dto.ExerciseSetDto

import com.cookandroid.challengers.network.dto.TodayPlanResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExerciseApi {
    @POST("/api/create-dummy-plan")
    fun createDummyPlan(@Body request: DummyPlanRequest): Call<DummyPlanResponse>

    // ✅ 전체 운동 목록 가져오기
    @GET("/api/exercises")
    suspend fun getAllExercises(): Response<List<ExerciseDto>>
}



interface ScheduleApi {

    @POST("api/schedule/{scheduleId}/exercise")
    suspend fun addExerciseToSchedule(
        @Path("scheduleId") scheduleId: Long,
        @Body request: RetrofitClient.AddExerciseRequest
    ): Response<RetrofitClient.AddExerciseResponse>
    @GET("/api/plan/today")
    suspend fun getTodayPlan(@Query("userId") userId: Int): Response<TodayPlanResponse>

    @POST("/api/schedule")
    suspend fun createSchedule(@Body request: RetrofitClient.CreateScheduleRequest): Response<RetrofitClient.CreateScheduleResponse>


    @PUT("/api/schedule/change-exercise")
    suspend fun changeExercise(
        @Body request: RetrofitClient.ChangeExerciseRequest
    ): Response<RetrofitClient.ChangeExerciseResponse>


    @GET("/api/schedule-id")
    suspend fun getScheduleId(
        @Query("planId") planId: Long,
        @Query("exerciseId") exerciseId: Long
    ): Response<RetrofitClient.ScheduleIdResponse>



    @DELETE("/api/schedule/{scheduleId}")
    suspend fun deleteExercise(
        @Path("scheduleId") scheduleId: Long
    ): Response<Void>

    @POST("/api/schedule/{scheduleId}/exercise")
    fun updateExercise(
        @Path("scheduleId") scheduleId: Int,
        @Body request: RetrofitClient.ExerciseUpdateRequest
    ): Call<RetrofitClient.ServerResponse>

    @POST("/api/schedule/set")
    fun insertSet(@Body set: ExerciseSetEntity): Call<Void>

    @POST("/api/schedule/{scheduleId}/change-exercise")
    suspend fun changeExerciseServer(
        @Path("scheduleId") scheduleId: Long,
        @Body request: RetrofitClient.ChangeExerciseServerRequest
    ): Response<RetrofitClient.ServerResponse>

    // ✅ 운동 세트 조회 API
    @GET("/api/schedule/{planId}/exercise/{exerciseId}/sets")
    suspend fun getSetsByPlanAndExercise(
        @Path("planId") planId: Long,
        @Path("exerciseId") exerciseId: Long
    ): Response<List<ExerciseSetDto>>

    @PATCH("/api/schedule/{scheduleId}/order")
    suspend fun updateExerciseOrder(
        @Path("scheduleId") scheduleId: Long,
        @Query("order") newOrder: Int
    ): Response<Void>


    @GET("/api/schedule/{scheduleId}/time-sets")
    fun getTimeSets(@Path("scheduleId") scheduleId: Long): Call<List<RetrofitClient.TimeSetDto>>

    @PATCH("/api/schedule/{scheduleId}/time-sets")
    fun updateTimeSets(
        @Path("scheduleId") scheduleId: Long,
        @Body sets: List<RetrofitClient.TimeSetDto>
    ): Call<Void>

    @GET("/api/schedule/{scheduleId}/reps-sets")
    fun getRepsSets(@Path("scheduleId") scheduleId: Long): Call<List<RetrofitClient.RepsSetDto>>

    @PATCH("/api/schedule/{scheduleId}/reps-sets")
    fun updateRepsSets(
        @Path("scheduleId") scheduleId: Long,
        @Body sets: List<RetrofitClient.RepsSetDto>
    ): Call<Void>

    @GET("/api/schedule/{scheduleId}/exercise-info")
    suspend fun getExerciseInfo(@Path("scheduleId") scheduleId: Long): Response<ExerciseDto>



}