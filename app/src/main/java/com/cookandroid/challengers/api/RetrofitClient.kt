package com.cookandroid.challengers.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.cookandroid.challengers.BuildConfig // ★ 명시적으로 추가!
import com.cookandroid.challengers.auth.login.LoginService
import com.google.gson.annotations.SerializedName

// 서버와 애뮬레이터 연동을 위한 코드 절대 수정 XXX
object RetrofitClient {
   // private val BASE_URL: String = BuildConfig.BASE_URL // ★ 타입 명시!
   private const val BASE_URL = "http://10.0.2.2:3000/"
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

    data class AddExerciseRequest(
        @SerializedName("exercise_id") val exerciseId: Int,
        @SerializedName("set_list") val setList: List<SetData>
    )

    data class SetData(
        @SerializedName("set_number") val setNumber: Int,
        val reps: Int? = null,
        val weight: Int? = null,
        val seconds: Int? = null
    )

    data class AddExerciseResponse(
        val message: String
    )

    val scheduleApi: ScheduleApi by lazy {
        retrofit.create(ScheduleApi::class.java)
    }

    data class CreateScheduleRequest(
        @SerializedName("planId") val planId: Int,
        @SerializedName("date") val date: String,
        @SerializedName("exerciseOrder") val exerciseOrder: Int
    )

    data class CreateScheduleResponse(
        val message: String,
        @SerializedName("scheduleId") val scheduleId: Int
    )


}