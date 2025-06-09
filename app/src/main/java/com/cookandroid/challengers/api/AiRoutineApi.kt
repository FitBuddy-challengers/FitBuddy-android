package com.cookandroid.challengers.api

import retrofit2.Call
import com.cookandroid.challengers.model.AiRoutineRequest
import com.cookandroid.challengers.model.AiRoutineResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AiRoutineApi {
    @Headers("Content-Type: application/json")
    @POST("api/generate-routine")
    fun generateRoutine(@Body request: AiRoutineRequest): Call<AiRoutineResponse>
}