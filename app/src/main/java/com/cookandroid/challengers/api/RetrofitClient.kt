package com.cookandroid.challengers.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.cookandroid.challengers.BuildConfig // ★ 명시적으로 추가!
import com.cookandroid.challengers.auth.login.LoginService
import com.cookandroid.challengers.network.dto.PlanDto
import com.cookandroid.challengers.network.dto.ScheduleDto
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

//    data class AddExerciseRequest(
//        @SerializedName("exercise_id") val exerciseId: Int,
//        @SerializedName("set_list") val setList: List<SetData>
//    )
//
//    data class SetData(
//        @SerializedName("set_number") val setNumber: Int,
//        val reps: Int? = null,
//        val weight: Int? = null,
//        val seconds: Int? = null
//    )

    data class AddExerciseResponse(
        val message: String
    )

    val scheduleApi: ScheduleApi by lazy {
        retrofit.create(ScheduleApi::class.java)
    }

    data class  CreateScheduleRequest(
        @SerializedName("planId") val planId: Int,
        @SerializedName("date") val date: String,
        @SerializedName("exerciseOrder") val exerciseOrder: Int,
        @SerializedName("exercise_id") val exerciseId: Int
    )

    data class CreateScheduleResponse(
        val message: String,
        @SerializedName("scheduleId") val scheduleId: Int
    )

    data class ScheduleIdResponse(val scheduleId: Long)

    data class TodayPlanResponse(
        val plan: PlanDto,
        val schedules: List<ScheduleDto>
    )

//    data class PlanDto(
//        val id: Int,
//        @SerializedName("date") val date: String
//    )
//
//    data class ScheduleDto(
//        @SerializedName("id") val id: Int,
//        @SerializedName("exerciseId") val exerciseId: Int
//        // 필요 시 더 많은 필드 추가 가능
//    )

    data class ExerciseUpdateRequest(
        val exercise_id: Int,
        val set_list: List<SetDto>
    )

    data class SetDto(
        val set_number: Int,
        val reps: Int? = null,
        val weight: Int? = null,
        val seconds: Int? = null
    )
    data class ServerResponse(
        val message: String
    )

    data class ChangeExerciseRequest(
        val planId: Long,
        val oldExerciseId: Long,
        val newExerciseId: Long
    )

    data class ChangeExerciseResponse(
        val success: Boolean,
        val scheduleIds: List<Long>
    )

//    data class ChangeExerciseServerRequest(
//        val newExerciseId: Long
//    )

    //val scheduleApi: ScheduleApi = retrofit.create(ScheduleApi::class.java)

    // ✅ 운동 변경용 요청 DTO
    data class ChangeExerciseServerRequest(
        @SerializedName("newExerciseId")
        val newExerciseId: Long
    )

    // ✅ 운동 추가 요청 DTO
    data class AddExerciseRequest(
        val exerciseId: Int,
        val setList: List<SetData>
    )

    // ✅ 세트 정보 DTO
    data class SetData(
        val setNumber: Int,
        val reps: Int? = null,
        val weight: Int? = null,
        val seconds: Int? = null
    )

    data class TimeSetUiModel(
        var setNumber: Int,
        var minutes: Int,
        var weight: Float
    )

    data class TimeSetDto(
        @SerializedName("set_number") val setNumber: Int,
        val seconds: Int,
        val weight: Float
    )

    data class RepsSetUiModel(
        var setNumber: Int,
        var reps: Int,
        var weight: Float
    )

    data class RepsSetDto(
        @SerializedName("set_number") val setNumber: Int,
        val reps: Int,
        val weight: Float
        , @SerializedName("is_completed") val isCompleted: Boolean = false
    )

    data class Exercise_Set(
        val setNumber: Int,
        val reps: Int,
        val weight: Int,
        val isCompleted: Boolean
    )

    data class CompleteSetRequest(
        val setNumber: Int
    )

    data class CompleteSetResponse(
        val message: String,
        val isWorkoutCompleted: Boolean
    )

    data class SetCompletionRequest(
        val scheduleId: Long,
        val setNumber: Int,
        val isCompleted: Boolean
    )






}

