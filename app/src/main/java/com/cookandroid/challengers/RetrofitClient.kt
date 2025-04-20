package com.cookandroid.challengers


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.cookandroid.challengers.BuildConfig // ★ 명시적으로 추가!

// 서버와 애뮬레이터 연동을 위한 코드 절대 수정 XXX
object RetrofitClient {
    private val BASE_URL: String = BuildConfig.BASE_URL // ★ 타입 명시!

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}