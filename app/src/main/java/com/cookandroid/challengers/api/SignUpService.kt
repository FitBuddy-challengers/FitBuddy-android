package com.cookandroid.challengers.api


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 회원가입 요청 데이터
data class SignUpRequest(
    val email: String,
    val password: String
)

// 서버 응답 데이터
data class SignUpResponse(
    val message: String
)

interface SignUpService {

    // 회원가입
    @POST("/signup")
    fun signup(
        @Body request: SignUpRequest
    ): Call<SignUpResponse>

    // OTP 인증 확인
    @POST("/verify-otp")
    fun verifyOtp(
        @Body request: Map<String, String> // {"email": "", "otp": ""}
    ): Call<Map<String, String>>
}