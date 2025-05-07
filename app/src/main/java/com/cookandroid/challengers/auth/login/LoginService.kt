package com.cookandroid.challengers.auth.login


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {
    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}