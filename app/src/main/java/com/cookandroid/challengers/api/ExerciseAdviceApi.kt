package com.cookandroid.challengers.api


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ExerciseAdviceApi {

    @POST("api/exercise-advice")
    suspend fun getExerciseAdvice(
        @Body request: AdviceRequest
    ): Response<AdviceResponse>
}

data class AdviceRequest(
    val userId: Int
)

data class AdviceResponse(
    val advice: String
)