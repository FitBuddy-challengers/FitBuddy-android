package com.cookandroid.challengers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

data class CalendarDecorateItem(val date: LocalDate?, val completionRate: Int)

// ★ ViewModel -> AndroidViewModel로 변경하고 Application을 생성자로 받음
class RecordCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val _exerciseRecords = MutableLiveData<List<RetrofitClient.ExerciseRecordItem>>()
    val exerciseRecords: LiveData<List<RetrofitClient.ExerciseRecordItem>> = _exerciseRecords

    private val _decorateDates = MutableLiveData<List<CalendarDecorateItem>>()
    val decorateDates: LiveData<List<CalendarDecorateItem>> = _decorateDates

    private val userPreference = UserPreference(application)

    fun loadExerciseRecords(date: LocalDate) {
        viewModelScope.launch {
            // ★ SharedPreferences에서 userId 가져오기
            val userId = userPreference.getUserId()
            if (userId == -1) {
                Log.e("RecordViewModel", "Invalid user ID, cannot load daily records.")
                _exerciseRecords.value = emptyList() // 데이터가 없음을 UI에 알림
                return@launch // 유효하지 않은 ID이므로 API 호출 중단
            }

            try {
                // 날짜를 "YYYY-MM-DD" 형식의 문자열로 변환
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val response = RetrofitClient.recordApi.getDailyRecords(userId, dateString)

                if (response.isSuccessful) {
                    _exerciseRecords.value = response.body() ?: emptyList()
                    Log.d("RecordViewModel", "서버에서 일별 기록 로드 성공 (userId: $userId): ${response.body()?.size ?: 0}개")
                } else {
                    _exerciseRecords.value = emptyList()
                    Log.e("RecordViewModel", "일별 기록 로드 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _exerciseRecords.value = emptyList()
                Log.e("RecordViewModel", "일별 기록 로드 중 오류", e)
            }
        }
    }

    fun loadAndCalculateCompletionRates(year: Int, month: Int) {
        viewModelScope.launch {
            // ★ SharedPreferences에서 userId 가져오기
            val userId = userPreference.getUserId()
            if (userId == -1) {
                Log.e("RecordViewModel", "Invalid user ID, cannot load monthly completion rates.")
                _decorateDates.value = emptyList() // 데이터가 없음을 UI에 알림
                return@launch // 유효하지 않은 ID이므로 API 호출 중단
            }

            try {
                val response = RetrofitClient.recordApi.getMonthlyCompletion(userId, year, month)
                if (response.isSuccessful) {
                    val rates = response.body() ?: emptyList()
                    val decorateList = rates.map { dto ->
                        // 서버에서 받은 "YYYY-MM-DD" 문자열을 LocalDate로 파싱
                        val localDate = LocalDate.parse(dto.date, DateTimeFormatter.ISO_LOCAL_DATE)
                        CalendarDecorateItem(localDate, dto.completionRate)
                    }
                    _decorateDates.value = decorateList
                    Log.d("RecordViewModel", "서버에서 월별 완료율 로드 성공 (userId: $userId): ${decorateList.size}개")
                } else {
                    _decorateDates.value = emptyList()
                    Log.e("RecordViewModel", "월별 완료율 로드 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _decorateDates.value = emptyList()
                Log.e("RecordViewModel", "월별 완료율 로드 중 오류", e)
            }
        }
    }
}