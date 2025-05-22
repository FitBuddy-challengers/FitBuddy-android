package com.cookandroid.challengers.api

import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ExerciseApi {
    @POST("/api/create-dummy-plan")
    fun createDummyPlan(@Body request: DummyPlanRequest): Call<DummyPlanResponse>
}