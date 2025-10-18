package com.cookandroid.challengers.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//import com.cookandroid.challengers.BuildConfig // ★ 명시적으로 추가!
import com.cookandroid.challengers.auth.login.LoginService
import com.cookandroid.challengers.network.dto.PlanDto
import com.cookandroid.challengers.network.dto.ScheduleDto
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

// 서버와 애뮬레이터 연동을 위한 코드 절대 수정 XXX
object RetrofitClient {
    // private val BASE_URL: String = BuildConfig.BASE_URL // ★ 타입 명시!

    private const val BASE_URL = "https://fitbuddy-server-p6r6.onrender.com/" // ← 슬래시 추가!
    //private const val BASE_URL = "http://10.0.2.2:3000/"//<- 원래 내 pc
    //private const val BASE_URL = "http://172.19.27.182:3000/" //내 갤럭시//172.19.27.182

    //  먼저 gson 정의Add commentMore actions
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        //.addConverterFactory(GsonConverterFactory.create())Add commentMore actions
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val challengeApi: ChallengeApi = retrofit.create(ChallengeApi::class.java)

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

    // AI 루틴 api
    val aiRoutineApi: AiRoutineApi by lazy {
        retrofit.create(AiRoutineApi::class.java)
    }

    // AI 추천 api
    val recommendApi: RecommendExerciseApi by lazy {
        retrofit.create(RecommendExerciseApi::class.java)
    }

    // 기록 API 인터페이스
    val recordApi: RecordApi by lazy {
        retrofit.create(RecordApi::class.java)
    }

    // 상점 API 인터페이스
    val storeApi: StoreApi by lazy {
        retrofit.create(StoreApi::class.java)
    }

    data class WorkoutRecordDto(val date: String)


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

    data class CreateScheduleRequest(
        @SerializedName("planId") val planId: Int,
        @SerializedName("date") val date: String,
        @SerializedName("exerciseOrder") val exerciseOrder: Int,
        @SerializedName("exercise_id") val exerciseId: Int
    )

    data class CreateScheduleResponse(
        val message: String,
        @SerializedName("scheduleId") val scheduleId: Int
    )

    data class ScheduleIdResponse(
        @SerializedName("scheduleId") // **** 서버가 보내는 JSON 키 "scheduleId"와 매핑 ****
        val scheduleId: Long
    )

    // 특정 Plan에 속한 Schedule 아이템을 위한 DTO (exerciseId 포함)
    data class SimpleScheduleItemDto(
        @SerializedName("exercise_id") val exerciseId: Long, // 서버 JSON 키가 "exercise_id"라고 가정
        @SerializedName("schedule_id") val scheduleId: Long, // 스케줄 ID도 유용할 수 있음
        @SerializedName("exercise_order") val exerciseOrder: Int // 순서 정보
    )

    data class TodayPlanResponse(
        val plan: PlanDto,
        val schedules: List<ScheduleDto>
    )

    data class TodayPlanRequest(
        @SerializedName("user_id") val userId: Int,
        val date: String
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
    data class AddExerciseRequest( // 세트 목록을 포함한 운동 추가 요청 (사용처 확인 필요)
        @SerializedName("exerciseId") val exerciseId: Int, // 서버 API 스펙에 따라 키 이름 확인
        @SerializedName("setList") val setList: List<SetData>
    )

    // ✅ 세트 정보 DTO
    data class SetData(
        @SerializedName("set_number") val setNumber: Int,
        val reps: Int? = null,
        val weight: Int? = null,
        val seconds: Int? = null,
        @SerializedName("is_completed") val isCompleted: Boolean? = false // 서버와 주고받는 필드라면 추가
    )

    //    data class TimeSetUiModel(
//        var setNumber: Int,
//        var minutes: Int,
//        var weight: Float
//    )
    data class TimeSetUiModel(
        var setNumber: Int,
        var hours: Int,
        var minutes: Int,
        var seconds: Int,
        var weight: Float // 무게 필드가 이 UI에 필요 없다면 제거 가능
    )


    data class TimeSetDto(
        @SerializedName("set_number") val setNumber: Int,
        val seconds: Int,
        val weight: Float,
        @SerializedName("is_completed") val isCompleted: Boolean = false // ★ 추가

    )

    data class RepsSetUiModel(
        var setNumber: Int,
        var reps: Int,
        var weight: Float
    )

    data class RepsSetDto(
        @SerializedName("set_number") val setNumber: Int,
        val reps: Int,
        val weight: Float,
        @SerializedName("is_completed") val isCompleted: Boolean = false
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
        @SerializedName("scheduleId") // JSON으로 전송될 때 "scheduleId" 키를 사용하도록 지정
        val scheduleId: Long,

        @SerializedName("setNumber")
        val setNumber: Int,

        @SerializedName("isCompleted")
        val isCompleted: Boolean,

        @SerializedName("time_seconds")
        val timeSeconds: Int? = null
    )

    // 시간 기반 세트 완료 요청용 DTO
    data class TimeSetCompletionRequest(
        @SerializedName("scheduleId")
        val scheduleId: Long,
        @SerializedName("setNumber")
        val setNumber: Int,
        @SerializedName("isCompleted")
        val isCompleted: Boolean,
        @SerializedName("elapsedTimeMillis")
        val elapsedTimeMillis: Long? = null
    )

    // 북마크 설정
    data class ToggleFavoriteRequest(
        @SerializedName("isFavorite") val isFavorite: Boolean
    )

    // 숨김 상태 관련 데이터 클래스, 운동 리스트, 운동 추가페이지, 숨겨진 운동 페이지에서 사용함
    data class ToggleHiddenRequest(
        @SerializedName("isHidden") val isHidden: Boolean
    )

    data class ExerciseStateUpdateResponse(
        @SerializedName("exerciseId") val exerciseId: Long,
        @SerializedName("isFavorite") val isFavorite: Boolean,   // 서버에서 항상 값을 보낸다고 가정 (non-null)
        @SerializedName("isHidden") val isHidden: Boolean,     // 서버에서 항상 값을 보낸다고 가정 (non-null)
        @SerializedName("message") val message: String? = null
    )

// 챌린지
data class ChallengeLevelDto(
    val level: Int,
    @SerializedName("required_attendance")
    val requiredAttendance: Int,
    @SerializedName("required_photo")
    val requiredPhoto: Int,
    @SerializedName("required_exercise")
    val requiredExercise: Int,

    // 🚨 보상 필드 추가 및 매핑
    @SerializedName("reward_attendance")
    val rewardAttendance: Int,
    @SerializedName("reward_photo")
    val rewardPhoto: Int,
    @SerializedName("reward_exercise")
    val rewardExercise: Int
)

    data class ChallengeProgressResponse(
        val level: Int?,
        val coin: Int?,
        val nickname: String?,
        val profileImage: String?,
        val required: Requirement?,
        @SerializedName("counts")
        val current: Requirement?,
        @SerializedName("progress_percent")
        val progress: Progress?,
        val reward: Reward?
    )

    data class Requirement(
        val attendance: Int,
        val photo: Int,
        val exercise: Int
    )

    data class Progress(
        val attendancePercent: Int,
        val photoPercent: Int,
        val exercisePercent: Int
    )

    data class ChallengeItemUiModel(
        val title: String,
        val progressPercent: Int,
        val reward: Int, // 보상 코인 (현재는 0으로 표시)
        val type: String // "attendance", "exercise", "photo" 등 챌린지 종류를 식별하기 위한 필드

    )

    data class Reward(
        @SerializedName("reward_attendance")
        val attendance: Int,
        @SerializedName("reward_photo")
        val photo: Int,
        @SerializedName("reward_exercise")
        val exercise: Int
    )

    // 보상 요청 시 서버에 보낼 데이터 클래스
    data class ClaimRewardRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("challengeType")
        val challengeType: String
    )


    // 보상 요청 후 서버에서 받을 응답 데이터 클래스
    data class ClaimRewardResponse(
        val success: Boolean,
        val message: String,
        val updatedCoin: Int,
        val updatedLevel: Int
    )

    data class UploadPhotoResponse(
        val success: Boolean,
        val message: String
    )

    data class PhotoChallengeItem(
        @SerializedName("image_url")
        val imageUrl: String,
        val date: String // "YYYY-MM-DD"
    )

    // 기록
    // 서버로부터 받아올 특정 날짜의 운동 기록 데이터 구조
    data class ExerciseRecordItem(
        @SerializedName("exercise_name")
        val exerciseName: String,
        val reps: Int?,
        val sets: Int?,
        val seconds: Int?,
        @SerializedName("is_completed")
        val isCompleted: Boolean,
        @SerializedName("is_time_type")
        val isTimeType: Boolean
    )

    // 서버로부터 받아올 월별 운동 완료율 데이터 구조
    data class DateCompletionRateDto(
        val date: String, // "YYYY-MM-DD" 형식의 문자열
        @SerializedName("completion_rate")
        val completionRate: Int
    )

    data class MonthlySummaryResponse(
        @SerializedName("mostFrequentPart")
        val mostFrequentPart: SummaryItem?,

        @SerializedName("mostFrequentExercise")
        val mostFrequentExercise: SummaryItem?
    )

    data class SummaryItem(
        val part: String?,
        val name: String?,
        val count: Int
    )

    data class WeightRecordDto(
        val id: Long,
        @SerializedName("user_id")
        val userId: Int,
        val date: String, // "YYYY-MM-DD"
        val weight: Double,
        @SerializedName("body_fat_percentage")
        val bodyFatPercentage: Double?,
        @SerializedName("skeletal_muscle_mass")
        val skeletalMuscleMass: Double?
    )

    // 서버로 보낼 때 사용할 DTO
    data class WeightRecordRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("date")
        val date: String, // "YYYY-MM-DD"
        @SerializedName("weight")
        val weight: Double,
        @SerializedName("bodyFatPercentage")
        val bodyFatPercentage: Double?,
        @SerializedName("skeletalMuscleMass")
        val skeletalMuscleMass: Double?
    )

    // 상점
    // 서버로 보낼 요청 DTO
    data class PurchaseRequest(
        val userId: Int,
        val itemId: Int
    )

    data class PurchaseResponse(
        val success: Boolean,
        val message: String,
        val updatedCoin: Int
    )

    // GPT 운동 추천 요청 DTO
    data class RecommendExerciseRequest(
        @SerializedName("userId") val userId: Int
    )

    // GPT 운동 추천 응답 DTO
    data class RecommendExerciseResponse(
        val recommendation: String
    )

}
