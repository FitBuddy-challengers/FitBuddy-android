package com.cookandroid.challengers.repository

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
        try {
            val response = aiRoutineApi.generateRoutine(
                AiRoutineRequest(user_info = userInfo, schedule_info = scheduleInfo)
            ).execute()

            if (response.isSuccessful) {
                response.body()?.plan_text
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
}