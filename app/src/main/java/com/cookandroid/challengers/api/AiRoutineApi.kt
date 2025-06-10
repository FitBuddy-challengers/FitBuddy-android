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
}