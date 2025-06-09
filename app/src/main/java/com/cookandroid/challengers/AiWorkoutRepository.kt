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

    suspend fun createPlan(date: String): Long? = withContext(Dispatchers.IO) {
        try {
            val response = scheduleApi.createSchedule(
                RetrofitClient.CreateScheduleRequest(
                    planId = 0,
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

    suspend fun addExercisesToSchedule(
        planId: Long,
        exerciseList: List<Triple<Int, Int?, Int?>>,
        date: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            exerciseList.forEachIndexed { index, (exerciseId, reps, _) ->
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

                val addResponse = scheduleApi.addExerciseToSchedule(
                    scheduleId,
                    RetrofitClient.AddExerciseRequest(
                        exerciseId = exerciseId,
                        setList = listOf(
                            RetrofitClient.SetData(
                                setNumber = 1,
                                reps = reps ?: 10,
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