package com.cookandroid.challengers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RecordDateViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreference = UserPreference(application)

    private val _summaryText = MutableLiveData<String>()
    val summaryText: LiveData<String> = _summaryText

    private val _radarData = MutableLiveData<Map<String, Float>>()
    val radarData: LiveData<Map<String, Float>> = _radarData

    private val _recommendationText = MutableLiveData<String>()
    val recommendationText: LiveData<String> = _recommendationText


    init {
        loadMonthlySummary() // ViewModel 생성 시 요약 정보 로드
        loadAIRecommendation()
    }


    private fun loadMonthlySummary() {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            _summaryText.value = "로그인이 필요합니다."
            return
        }

        viewModelScope.launch {
            try {
                // 두 API를 동시에 호출
                val userProgressDeferred = async(Dispatchers.IO) {
                    // 이 함수는 성공 시 DTO를 직접 반환하고, 실패 시 예외를 던집니다.
                    RetrofitClient.challengeApi.getUserChallengeProgress(userId)
                }
                val summaryDeferred = async(Dispatchers.IO) {
                    // 이 함수는 항상 Response 객체를 반환합니다.
                    RetrofitClient.recordApi.getMonthlySummary(userId)
                }

                // ★★★ 응답 처리 방식 변경 ★★★
                // getUserChallengeProgress의 결과는 DTO 그 자체입니다.
                val userProgress = userProgressDeferred.await()
                // getMonthlySummary의 결과는 Response 객체입니다.
                val summaryResponse = summaryDeferred.await()

                // userProgress는 예외가 발생하지 않았다면 항상 유효한 객체입니다.
                val userName = userProgress.nickname ?: "챌린저"

                // summaryResponse는 성공 여부를 확인해야 합니다.
                if (summaryResponse.isSuccessful) {
                    val summary = summaryResponse.body()
                    val text = if (summary?.mostFrequentPart != null && summary.mostFrequentExercise != null) {
                        val exerciseNameWithParticle = appendObjectParticle(summary.mostFrequentExercise.name ?: "운동")
                        "${userName}님은 ${summary.mostFrequentPart.part ?: "활동"} 왕!\n이번 달, ${exerciseNameWithParticle} 가장 많이 하셨어요"
                    } else {
                        "${userName}님, 이번 달 운동 기록이 없습니다."
                    }
                    _summaryText.value = text
                } else {
                    // 사용자 이름은 성공적으로 가져왔지만, 요약 정보 로드에 실패한 경우
                    _summaryText.value = "${userName}님, 이번 달 운동 기록을 불러오지 못했습니다."
                    Log.e("RecordDateViewModel", "Failed to load monthly summary: ${summaryResponse.code()}")
                }
            } catch (e: Exception) {
                // ★★★ 여기서 두 API 호출의 모든 예외(네트워크 오류, 4xx/5xx 에러 등)를 한 번에 처리합니다. ★★★
                Log.e("RecordDateViewModel", "Error loading data in loadMonthlySummary", e)
                _summaryText.value = "데이터를 불러오는 중 오류가 발생했습니다."
            }
        }
    }

    fun loadRadarData(period: String) {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            _radarData.value = emptyMap() // userId가 없을 경우에도 빈 맵 할당
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.recordApi.getRadarData(userId, period)
                if (response.isSuccessful) {
                    val mapOfDoubles = response.body()
                    val mapOfFloats = mapOfDoubles?.mapValues { entry ->
                        entry.value.toFloat()
                    }

                    // ★★★ 수정된 부분 ★★★
                    // mapOfFloats가 null이면 emptyMap()을 할당하여 null이 들어가지 않도록 보장
                    _radarData.value = mapOfFloats ?: emptyMap()

                } else {
                    // API 호출 실패 시에도 빈 맵 할당
                    _radarData.value = emptyMap()
                    Log.e("RecordDateViewModel", "Failed to load radar data: ${response.code()}")
                }
            } catch (e: Exception) {
                // 예외 발생 시에도 빈 맵 할당
                _radarData.value = emptyMap()
                Log.e("RecordDateViewModel", "Error loading radar data", e)
            }
        }
    }

    fun loadAIRecommendation() {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            _recommendationText.value = "로그인이 필요합니다."
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.recommendApi.getExerciseRecommendation(
                    RetrofitClient.RecommendExerciseRequest(userId)
                )

                if (response.isSuccessful) {
                    val result = response.body()?.recommendation ?: "추천이 비어있습니다."
                    _recommendationText.value = result
                } else {
                    Log.e("ViewModel", "추천 실패: ${response.code()}")
                    _recommendationText.value = "추천을 가져오지 못했습니다."
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "추천 호출 오류", e)
                _recommendationText.value = "AI 추천 실패: 네트워크 오류"
            }
        }
    }

    // 을/를 처리 함수
    private fun appendObjectParticle(word: String): String {
        if (word.isEmpty()) return word
        val lastChar = word.last()
        val hasFinalConsonant = (lastChar.code - 0xAC00) % 28 != 0
        return word + if (hasFinalConsonant) "을" else "를"
    }
}