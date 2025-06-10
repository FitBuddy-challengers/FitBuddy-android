package com.cookandroid.challengers.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.RetrofitClient.WeightRecordDto
import com.cookandroid.challengers.api.RetrofitClient.WeightRecordRequest
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordWeightViewModel(application: Application) : AndroidViewModel(application) {

    private val _allRecords = MutableLiveData<List<WeightRecordDto>>()
    val allRecords: LiveData<List<WeightRecordDto>> = _allRecords

    private val userPreference = UserPreference(application)

    init {
        // ViewModel이 생성될 때 모든 기록을 불러옵니다.
        loadAllWeightRecords()
    }

    private fun loadAllWeightRecords() {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            Log.e("RecordWeightVM", "Invalid User ID. Cannot fetch records.")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.recordApi.getWeightRecords(userId)
                if (response.isSuccessful) {
                    _allRecords.value = response.body() ?: emptyList()
                    Log.d("RecordWeightVM", "Successfully fetched ${response.body()?.size ?: 0} records.")
                } else {
                    Log.e("RecordWeightVM", "Failed to fetch records: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("RecordWeightVM", "Error fetching records", e)
            }
        }
    }

    fun addOrUpdateWeightRecord(
        date: LocalDate,
        weight: Double,
        bodyFat: Double?,
        muscle: Double?,
        onResult: (isSuccess: Boolean) -> Unit
    ) {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            onResult(false)
            return
        }

        val request = WeightRecordRequest(
            userId = userId,
            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE), // "YYYY-MM-DD"
            weight = weight,
            bodyFatPercentage = bodyFat,
            skeletalMuscleMass = muscle
        )

        viewModelScope.launch {
            try {
                val response = RetrofitClient.recordApi.addOrUpdateWeightRecord(request)
                if (response.isSuccessful) {
                    loadAllWeightRecords() // 저장 성공 후, 목록을 새로고침하여 차트에 즉시 반영
                    onResult(true)
                } else {
                    Log.e("RecordWeightVM", "Failed to save record: ${response.code()}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("RecordWeightVM", "Error saving record", e)
                onResult(false)
            }
        }
    }
}