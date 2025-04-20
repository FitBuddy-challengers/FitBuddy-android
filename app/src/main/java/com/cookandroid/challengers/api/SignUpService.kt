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

    // 서버에 OTP 이메일 발송 요청
    @POST("/send-otp")
    fun sendOtp(
        @Body request: Map<String, String>  // {"email": ""}
    ): Call<Map<String, String>>

    // OTP 인증 확인
    @POST("/verify-otp")
    fun verifyOtp(
        @Body request: Map<String, String> // {"email": "", "otp": ""}
    ): Call<Map<String, String>>


}