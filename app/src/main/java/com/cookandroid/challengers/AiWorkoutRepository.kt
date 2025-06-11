package com.cookandroid.challengers.repository

import android.util.Log
import com.cookandroid.challengers.api.AiPlanRequest
import com.cookandroid.challengers.api.AiPlanResponse
import com.cookandroid.challengers.api.AiRoutineApi
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.ScheduleApi
import com.cookandroid.challengers.model.AiRoutineRequest
import com.cookandroid.challengers.model.UserInfo
import com.cookandroid.challengers.model.ScheduleInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiWorkoutRepository(
    private val scheduleApi: ScheduleApi,
    private val aiRoutineApi: AiRoutineApi
) {
    /* AI 서버로부터 운동 루틴 텍스트 받아오기
     사용자 정보(UserInfo) + 스케줄 정보(ScheduleInfo) -> AI가 생성한 운동 루틴 텍스트 (String) */
    suspend fun getRoutineFromServer(
        userInfo: UserInfo,
        scheduleInfo: ScheduleInfo
    ): String? = withContext(Dispatchers.IO) {
        Log.d("ChatDebug", "✅ aiRoutineApi 준비 완료, generateRoutine 호출 전")
        try {
            Log.d("ChatDebug", "📤 Sending AI request with: $userInfo, $scheduleInfo")
            val response = aiRoutineApi.generateRoutine(
                AiRoutineRequest(user_info = userInfo, schedule_info = scheduleInfo)
            ).execute()

            if (response.isSuccessful) {
                val planText = response.body()?.plan_text  // ✅ 변수 선언
                Log.d("ChatDebug", "✅ AI 응답 성공: ${planText ?: "null"}")
                return@withContext planText

            } else {
                Log.e("ChatDebug", "❌ AI 응답 실패: ${response.code()} - ${response.errorBody()?.string()}")
                return@withContext null
            }
        } catch (e: Exception) {
           // e.printStackTrace()
            //null
            Log.e("ChatDebug", "❌ 예외 발생: ${e.message}", e)
            e.printStackTrace() // 🔧 실제 예외 스택 추적을 로그로 보기 위해 추가
            return@withContext null
        }
    }

    /* 운동 계획 생성, schedule id를 받아 반환 */
    suspend fun createPlan(date: String): Long? = withContext(Dispatchers.IO) {
        try {
            val response = scheduleApi.createSchedule(
                RetrofitClient.CreateScheduleRequest(
                    planId = 0, // 임시 id
                    date = date,
                    exerciseOrder = 0,
                    exerciseId = 0
                )
            )
            if (response.isSuccessful) {
                response.body()?.scheduleId?.toLong()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // !! 스케줄 저장 후 세트 정보 저장, 성공 시 true, 실패 시 false
    suspend fun addExercisesToSchedule(
        planId: Long,
        exerciseList: List<Triple<Int, Int?, Int?>>, // 운동 ID, reps, sets
        date: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            exerciseList.forEachIndexed { index, (exerciseId, reps, _) ->
                // 운동별 스케줄 생성
                val scheduleResponse = scheduleApi.createSchedule(
                    RetrofitClient.CreateScheduleRequest(
                        planId = planId.toInt(),
                        date = date,
                        exerciseOrder = index,
                        exerciseId = exerciseId
                    )
                )
                if (!scheduleResponse.isSuccessful) return@withContext false

                val scheduleId = scheduleResponse.body()?.scheduleId ?: return@withContext false

                // 스케줄에 세트 저장
                val addResponse = scheduleApi.addExerciseToSchedule(
                    scheduleId,
                    RetrofitClient.AddExerciseRequest(
                        exerciseId = exerciseId,
                        setList = listOf(
                            RetrofitClient.SetData(
                                setNumber = 1,
                                reps = reps ?: 10, // 기본값
                                weight = 0,
                                isCompleted = false
                            )
                        )
                    )
                )
                if (!addResponse.isSuccessful) return@withContext false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 데이터베이스 저장을 하기 위한 서버 통신 역할.
//    suspend fun submitAiPlan(request: AiPlanRequest): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val response = aiRoutineApi.submitAiPlan(request)
//            if (response.isSuccessful) {
//                Log.d("AiRepo", "✅ 루틴 저장 성공: ${response.body()?.plan_id}")
//                return@withContext true
//            } else {
//                Log.e("AiRepo", "❌ 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                return@withContext false
//            }
//        } catch (e: Exception) {
//            Log.e("AiRepo", "❌ 예외 발생: ${e.message}")
//            return@withContext false
//        }
//    }
    suspend fun submitAiPlan(request: AiPlanRequest): AiPlanResponse? = withContext(Dispatchers.IO) {
        try {
            val response = aiRoutineApi.submitAiPlan(request)
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("AiRepo", "✅ 루틴 저장 성공: ${body?.plan_id}")
                return@withContext body
            } else {
                Log.e("AiRepo", "❌ 실패: ${response.code()} - ${response.errorBody()?.string()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("AiRepo", "❌ 예외 발생: ${e.message}", e)
            return@withContext null
        }
    }
}