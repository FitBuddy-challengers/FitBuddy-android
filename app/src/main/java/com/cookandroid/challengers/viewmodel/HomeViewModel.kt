package com.cookandroid.challengers.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.WorkoutUiModel
import com.cookandroid.challengers.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val _workouts = MutableStateFlow<List<WorkoutUiModel>>(emptyList())
    val workouts: StateFlow<List<WorkoutUiModel>> get() = _workouts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // 캐시 저장소
    private var lastCachedWorkouts: List<WorkoutUiModel> = emptyList()

    fun loadTodayWorkoutPlan(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            // 캐시 먼저 보여주기
            if (lastCachedWorkouts.isNotEmpty()) {
                _workouts.value = lastCachedWorkouts
            }

            try {
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId)

                if (!response.isSuccessful) {
                    // ❗ 실패해도 UI는 업데이트해야 함
                    withContext(Dispatchers.Main) {
                        _workouts.value = lastCachedWorkouts
                    }
                    return@launch
                }

                val data = response.body()
                val scheduleList = data?.schedules ?: emptyList()

                val result = scheduleList.map { schedule ->
                    async {
                        val repsSets = RetrofitClient.scheduleApi
                            .getRepsSets(schedule.schedule_id.toLong())
                            .execute().body() ?: emptyList()

                        val timeSets = RetrofitClient.scheduleApi
                            .getTimeSets(schedule.schedule_id.toLong())
                            .execute().body() ?: emptyList()

                        when {
                            repsSets.isNotEmpty() ->
                                WorkoutUiModel(
                                    scheduleId = schedule.schedule_id.toLong(),
                                    name = schedule.exercise_name,
                                    reps = repsSets.first().reps,
                                    seconds = null,
                                    sets = repsSets.size,
                                    isCompleted = repsSets.all { it.isCompleted }
                                )
                            timeSets.isNotEmpty() ->
                                WorkoutUiModel(
                                    scheduleId = schedule.schedule_id.toLong(),
                                    name = schedule.exercise_name,
                                    reps = null,
                                    seconds = timeSets.first().seconds,
                                    sets = timeSets.size,
                                    isCompleted = timeSets.all { it.isCompleted }
                                )
                            else -> null
                        }
                    }
                }.mapNotNull { it.await() }

                withContext(Dispatchers.Main) {
                    _workouts.value = result
                    lastCachedWorkouts = result
                }

            } catch (e: Exception) {
                Log.e("HomeVM", " 네트워크 오류", e)
                // 실패해도 캐시라도 보여주기
                withContext(Dispatchers.Main) {
                    _workouts.value = lastCachedWorkouts
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    //캐시 여부 확인 메서드
    fun hasCache(): Boolean = lastCachedWorkouts.isNotEmpty()
}