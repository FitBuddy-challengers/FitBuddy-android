package com.cookandroid.challengers.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.cookandroid.challengers.model.ProfileData  // ProfileData 위치에 맞게 패키지 경로 조정

interface ProfileApiService {
    @POST("update-profile")
    fun updateProfile(@Body profileData: ProfileData): Call<Void>
}