package com.cookandroid.challengers.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.RetrofitClient.PhotoChallengeItem
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class ChallengePhotoViewModel : ViewModel() {
    // 7일(일~토)의 사진 데이터를 담는 리스트. 사진이 없으면 null.
    private val _weeklyPhotos = MutableLiveData<List<PhotoChallengeItem?>>(List(7) { null })
    val weeklyPhotos: LiveData<List<PhotoChallengeItem?>> get() = _weeklyPhotos

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchWeeklyPhotos(userId: Int) {
        if (userId == -1) {
            _error.value = "유효하지 않은 사용자 ID입니다."
            return
        }

        viewModelScope.launch {
            try {
                // 이번 주 일요일 날짜 계산
                val today = LocalDate.now()
                val daysFromSunday = if (today.dayOfWeek == DayOfWeek.SUNDAY) 0 else today.dayOfWeek.value
                val startDate = today.minusDays(daysFromSunday.toLong())
                val startDateString = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val response = RetrofitClient.challengeApi.getWeeklyPhotos(userId, startDateString)
                if (response.isSuccessful) {
                    val photosFromServer = response.body() ?: emptyList()

                    // 서버에서 받은 데이터를 7일짜리 리스트로 변환
                    val weeklyList = MutableList<PhotoChallengeItem?>(7) { null }
                    photosFromServer.forEach { photo ->
                        val photoDate = LocalDate.parse(photo.date)
                        val dayIndex = photoDate.dayOfWeek.value % 7 // 일요일=0, 월요일=1...
                        if (dayIndex in 0..6) {
                            weeklyList[dayIndex] = photo
                        }
                    }
                    _weeklyPhotos.postValue(weeklyList)
                } else {
                    val errorMsg = "주간 사진 로딩 실패 (코드: ${response.code()})"
                    _error.postValue(errorMsg)
                    Log.e("PhotoVM", errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "오류가 발생했습니다: ${e.message}"
                _error.postValue(errorMsg)
                Log.e("PhotoVM", "Error fetching weekly photos", e)
            }
        }
    }
}

