package com.cookandroid.challengers.api

import com.cookandroid.challengers.data.ExerciseSetEntity
import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import com.cookandroid.challengers.network.dto.TodayPlanResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExerciseApi {
    @POST("/api/create-dummy-plan")
    fun createDummyPlan(@Body request: DummyPlanRequest): Call<DummyPlanResponse>
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



    @GET("/api/schedule-id")
    suspend fun getScheduleId(
        @Query("planId") planId: Long,
        @Query("exerciseId") exerciseId: Long
    ): Response<RetrofitClient.ScheduleIdResponse>

    @DELETE("/api/schedule/{scheduleId}/exercise/{exerciseId}")
    suspend fun deleteExercise(
        @Path("scheduleId") scheduleId: Long,
        @Path("exerciseId") exerciseId: Long
    ): Response<Void>

    @POST("/api/schedule/{scheduleId}/exercise")
    fun updateExercise(
        @Path("scheduleId") scheduleId: Int,
        @Body request: RetrofitClient.ExerciseUpdateRequest
    ): Call<RetrofitClient.ServerResponse>

    @POST("/api/schedule/set")
    fun insertSet(@Body set: ExerciseSetEntity): Call<Void>
}