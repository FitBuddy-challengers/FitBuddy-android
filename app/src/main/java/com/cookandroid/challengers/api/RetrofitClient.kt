package com.cookandroid.challengers.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.cookandroid.challengers.BuildConfig // ★ 명시적으로 추가!
import com.cookandroid.challengers.auth.login.LoginService

// 서버와 애뮬레이터 연동을 위한 코드 절대 수정 XXX
object RetrofitClient {
    private val BASE_URL: String = BuildConfig.BASE_URL // ★ 타입 명시!

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val profileApiService: ProfileApiService by lazy {
        retrofit.create(ProfileApiService::class.java)
    }

    //로그인 API 인터페이스
    val loginService: LoginService by lazy {
        retrofit.create(LoginService::class.java)
    }

    
    //더미 스케줄을 담음(채윤지 05.22 추가)
    val exerciseApi: ExerciseApi by lazy {
        retrofit.create(ExerciseApi::class.java)
    }
}