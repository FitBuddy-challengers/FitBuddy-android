package com.cookandroid.challengers.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RecommendExerciseApi {
    @POST("recommend-exercise")
    suspend fun getExerciseRecommendation(@Body request: RetrofitClient.RecommendExerciseRequest): Response<RetrofitClient.RecommendExerciseResponse>
}