package com.cookandroid.challengers.api

import com.cookandroid.challengers.api.RetrofitClient.PhotoChallengeItem
import com.cookandroid.challengers.api.RetrofitClient.UploadPhotoResponse
import com.cookandroid.challengers.api.RetrofitClient.WorkoutRecordDto
import com.cookandroid.challengers.data.ExerciseSetEntity
import com.cookandroid.challengers.model.UserInfo
import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import com.cookandroid.challengers.network.dto.ExerciseDto
import com.cookandroid.challengers.network.dto.ExerciseSetDto

import com.cookandroid.challengers.network.dto.TodayPlanResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface ExerciseApi {
    @POST("/api/create-dummy-plan")
    fun createDummyPlan(@Body request: DummyPlanRequest): Call<DummyPlanResponse>

    // ✅ 전체 운동 목록 가져오기
    @GET("/api/exercises")
    suspend fun getAllExercises(): Response<List<ExerciseDto>>

    // ✅ 특정 운동의 즐겨찾기 상태 변경
    @PATCH("/api/exercises/{exerciseId}/favorite")
    suspend fun toggleExerciseFavorite(
        @Path("exerciseId") exerciseId: Long,
        @Body request: RetrofitClient.ToggleFavoriteRequest
    ): Response<RetrofitClient.ExerciseStateUpdateResponse>

    // 숨김 상태 변경
    @PATCH("/api/exercises/{exerciseId}/hidden")
    suspend fun toggleExerciseHidden(
        @Path("exerciseId") exerciseId: Long,
        @Body request: RetrofitClient.ToggleHiddenRequest
    ): Response<RetrofitClient.ExerciseStateUpdateResponse>

    @POST("/api/today-plan")
    fun getTodayPlan(
        @Body request: RetrofitClient.TodayPlanRequest
    ): Call<TodayPlanResponse>

    // 유저 id
    @GET("/api/user-info/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: Int): Response<UserInfo>

}


interface ScheduleApi {

    @POST("api/schedule/{scheduleId}/exercise")
    suspend fun addExerciseToSchedule(
        @Path("scheduleId") scheduleId: Int,
        @Body request: RetrofitClient.AddExerciseRequest
    ): Response<RetrofitClient.AddExerciseResponse>
    @GET("/api/plan/today")
    suspend fun getTodayPlan(@Query("userId") userId: Int): Response<TodayPlanResponse>

    @POST("/api/schedule")
    suspend fun createSchedule(@Body request: RetrofitClient.CreateScheduleRequest): Response<RetrofitClient.CreateScheduleResponse>


    @POST("/api/schedule/{scheduleId}/change-exercise")
    suspend fun changeExercise(
        @Path("scheduleId") scheduleId: Long,
        @Body request: RetrofitClient.ChangeExerciseServerRequest
    ): Response<RetrofitClient.ChangeExerciseResponse>


    @GET("/api/schedule-id")
    suspend fun getScheduleId(
        @Query("planId") planId: Long,
        @Query("exerciseId") exerciseId: Long
    ): Response<RetrofitClient.ScheduleIdResponse>


    @DELETE("/api/schedule/{scheduleId}")
    suspend fun deleteExercise(
        @Path("scheduleId") scheduleId: Long
    ): Response<Void>

    @POST("/api/schedule/{scheduleId}/exercise")
    fun updateExercise(
        @Path("scheduleId") scheduleId: Int,
        @Body request: RetrofitClient.ExerciseUpdateRequest
    ): Call<RetrofitClient.ServerResponse>

    @POST("/api/schedule/set")
    fun insertSet(@Body set: ExerciseSetEntity): Call<Void>

    @GET("api/plans/{planId}/schedules")
    suspend fun getSchedulesForPlan(
        @Path("planId") planId: Long
    ): Response<List<RetrofitClient.SimpleScheduleItemDto>>

    // 특정 스케줄(운동)을 완료로 표시하는 API
    @PATCH("/api/schedule/{scheduleId}/complete")
    suspend fun markScheduleAsComplete(
        @Path("scheduleId") scheduleId: Long
    ): Response<Void> // 또는 성공 메시지를 담은 DTO

    @POST("/api/schedule/{scheduleId}/change-exercise")
    suspend fun changeExerciseServer(
        @Path("scheduleId") scheduleId: Long,
        @Body request: RetrofitClient.ChangeExerciseServerRequest
    ): Response<RetrofitClient.ServerResponse>

    // ✅ 운동 세트 조회 API
    @GET("/api/schedule/{planId}/exercise/{exerciseId}/sets")
    suspend fun getSetsByPlanAndExercise(
        @Path("planId") planId: Long,
        @Path("exerciseId") exerciseId: Long
    ): Response<List<ExerciseSetDto>>

    @PATCH("/api/schedule/{scheduleId}/order")
    suspend fun updateExerciseOrder(
        @Path("scheduleId") scheduleId: Long,
        @Query("order") newOrder: Int
    ): Response<Void>


    @GET("/api/schedule/{scheduleId}/time-sets")
    fun getTimeSets(@Path("scheduleId") scheduleId: Long): Call<List<RetrofitClient.TimeSetDto>>

    @PATCH("/api/schedule/{scheduleId}/time-sets")
    fun updateTimeSets(
        @Path("scheduleId") scheduleId: Long,
        @Body sets: List<RetrofitClient.TimeSetDto>
    ): Call<Void>

    @GET("/api/reps-sets/{scheduleId}")
    fun getRepsSets(@Path("scheduleId") scheduleId: Long): Call<List<RetrofitClient.RepsSetDto>>


    @PATCH("/api/schedule/{scheduleId}/reps-sets")
    fun updateRepsSets(
        @Path("scheduleId") scheduleId: Long,
        @Body sets: List<RetrofitClient.RepsSetDto>
    ): Call<Void>

    @GET("/api/schedule/{scheduleId}/exercise-info")
    suspend fun getExerciseInfo(@Path("scheduleId") scheduleId: Long): Response<ExerciseDto>

    @PATCH("/api/reps-sets/{scheduleId}/complete-set")
    fun completeSet(
        @Path("scheduleId") scheduleId: Long,
        @Body body: RetrofitClient.CompleteSetRequest
    ): Call<RetrofitClient.CompleteSetResponse>


    @PATCH("/api/sets/reps/complete")
    fun updateRepsSetCompletion(
        @Query("scheduleId") scheduleId: Long,
        @Query("setNumber") setNumber: Int,
        @Query("isCompleted") isCompleted: Boolean
    ): Call<Void>

    @PATCH("/api/sets/reps/complete")
    fun updateRepsSetCompletion(
        @Body body: RetrofitClient.SetCompletionRequest
    ): Call<Void>

    @PATCH("/api/sets/time/complete")
    fun updateTimeSetCompletion(
        @Body body: RetrofitClient.TimeSetCompletionRequest
    ): Call<Void>
}

interface ChallengeApi {
    @GET("/api/challenge-levels")
    suspend fun getAllLevels(): List<RetrofitClient.ChallengeLevelDto>

    @GET("/api/user-challenge-progress/{userId}")
    suspend fun getUserChallengeProgress(@Path("userId") userId: Int): RetrofitClient.ChallengeProgressResponse

//    @POST("/api/challenge/attendance/{userId}")
//    suspend fun markAttendance(@Path("userId") userId: Int): Response<ResponseBody>

    @POST("/api/challenge/attendance/{userId}")
    fun markAttendance(@Path("userId") userId: Int): Call<ResponseBody>

    // 월간 운동 기록을 가져오는 함수 추가
    @GET("api/challenge/monthly-records")
    suspend fun getMonthlyWorkoutRecords(
        @Query("userId") userId: Int,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<List<WorkoutRecordDto>> // WorkoutRecordDto는 {"date": "..."} 형태의 객체

    // 사진 인증 업로드
    @Multipart
    @POST("/api/challenge/upload-photo")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Part("user_id") userId: RequestBody,
        @Part("date") date: RequestBody
    ): Response<UploadPhotoResponse>

    // 주간 인증 사진 목록 조회
    @GET("/api/challenge/weekly-photos")
    suspend fun getWeeklyPhotos(
        @Query("userId") userId: Int,
        @Query("startDate") startDate: String // "YYYY-MM-DD" 형식의 일요일
    ): Response<List<PhotoChallengeItem>>

    @POST("/api/challenge/claim")
    suspend fun claimReward(@Body request: RetrofitClient.ClaimRewardRequest): Response<RetrofitClient.ClaimRewardResponse>
}


interface RecordApi {
    @GET("/api/records/monthly-completion")
    suspend fun getMonthlyCompletion(
        @Query("userId") userId: Int,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<List<RetrofitClient.DateCompletionRateDto>>

    @GET("/api/records/daily")
    suspend fun getDailyRecords(
        @Query("userId") userId: Int,
        @Query("date") date: String // "YYYY-MM-DD"
    ): Response<List<RetrofitClient.ExerciseRecordItem>>

    @GET("/api/records/monthly-summary")
    suspend fun getMonthlySummary(@Query("userId") userId: Int
    ): Response<RetrofitClient.MonthlySummaryResponse>

    @GET("/api/records/radar-data")
    suspend fun getRadarData(
        @Query("userId") userId: Int,
        @Query("period") period: String
    ): Response<Map<String, Double>>

    @GET("/api/records/weight")
    suspend fun getWeightRecords(
        @Query("userId") userId: Int
    ): Response<List<RetrofitClient.WeightRecordDto>>

    @POST("/api/records/weight")
    suspend fun addOrUpdateWeightRecord(
        @Body record: RetrofitClient.WeightRecordRequest
    ): Response<Unit>
}

interface StoreApi {
    @POST("/api/store/purchase")
    suspend fun purchaseItem(
        @Body request: RetrofitClient.PurchaseRequest
    ): Response<RetrofitClient.PurchaseResponse>

    //  특정 사용자가 소유한 아이템 ID 목록을 가져오는 API
    @GET("/api/store/owned-items")
    suspend fun getOwnedItemIds(
        @Query("userId") userId: Int
    ): Response<List<Int>> // 예시: [1, 101, 102, 401] 형태의 응답을 기대
}

