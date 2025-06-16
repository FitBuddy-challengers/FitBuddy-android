package com.cookandroid.challengers.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.cookandroid.challengers.model.ProfileData  // ProfileData 위치에 맞게 패키지 경로 조정
import retrofit2.http.GET
import retrofit2.http.Query

interface ProfileApiService {
    @POST("update-profile")
    fun updateProfile(@Body profileData: ProfileData): Call<Void>

    @GET("get-profile")
    fun getProfile(@Query("email") email: String): Call<ProfileData>
}