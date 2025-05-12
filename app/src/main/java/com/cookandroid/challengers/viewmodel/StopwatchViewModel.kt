package com.cookandroid.challengers.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StopwatchViewModel : ViewModel() {
    private val _elapsedTime = MutableLiveData<Long>(0L)
    val elapsedTime: LiveData<Long> = _elapsedTime

    private val _isRunning = MutableLiveData<Boolean>(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private var startTime: Long = 0L
    private var timerJob: Job? = null

    // 추가: 이전 시간을 받아서 시작하는 함수
    fun startStopwatch(previousTime: Long = 0L) {
        if (!_isRunning.value!!) {
            _isRunning.value = true
            startTime = System.currentTimeMillis() - previousTime
            startTimer()
        }
    }

    // 추가: 시간을 설정하는 함수
    fun setElapsedTime(time: Long) {
        _elapsedTime.value = time
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_isRunning.value!!) {
                val currentTime = System.currentTimeMillis()
                _elapsedTime.value = currentTime - startTime
                delay(100) // 0.1초마다 업데이트
            }
        }
    }

    fun pauseStopwatch() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun stopStopwatch() {
        _isRunning.value = false
        timerJob?.cancel()
        _elapsedTime.value = 0L
    }

    fun formatElapsedTime(elapsedTime: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel() // ViewModel이 파괴될 때 타이머 정리
    }
}