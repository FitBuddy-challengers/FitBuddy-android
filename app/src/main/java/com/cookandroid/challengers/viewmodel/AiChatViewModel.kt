package com.cookandroid.challengers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.AiWorkoutRepository
import com.cookandroid.challengers.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class AichatState {
    object Welcome : AichatState()
    object AskDate : AichatState()
    object AskDays : AichatState()
    object AskFocusArea : AichatState()
    object Generating : AichatState()
    data class ShowResult(val planText: String, val isSecondTry: Boolean = false) : AichatState()
    object Done : AichatState()
    object Rejected : AichatState()
}

data class UserInfo(
    val name: String,
    val ageGroup: String,
    val gender: String,
    val height: Int,
    val weight: Int,
    val disease: String,
    val exerciseLevel: String,
    val preferredExercises: List<String>,
    val equipment: List<String>,
    var preferredFocusArea: String = ""
)

data class ScheduleInfo(
    var startDate: String,
    var endDate: String,
    var daysOfWeek: List<String>
)

class AichatViewModel(
    private val userInfo: UserInfo,
    private val scheduleInfo: ScheduleInfo,
    private val repository: AiWorkoutRepository,
    private val getWorkoutSchedule: suspend (UserInfo, ScheduleInfo) -> String
) : ViewModel() {

    private val _state = MutableStateFlow<AichatState>(AichatState.Welcome)
    val state: StateFlow<AichatState> = _state

    private var lastGeneratedPlan: String = ""
    private var recommendationCount = 0

    fun getUserName(): String = userInfo.name
    fun getFocusArea(): String = userInfo.preferredFocusArea

    fun onUserConfirmedStart() {
        _state.value = AichatState.AskDate
    }

    fun onUserDeclined() {
        _state.value = AichatState.Rejected
    }

    fun onDateSelected(start: String, end: String) {
        scheduleInfo.startDate = start
        scheduleInfo.endDate = end
        _state.value = AichatState.AskDays
    }

    fun onDaysSelected(days: List<String>) {
        scheduleInfo.daysOfWeek = days
        _state.value = AichatState.AskFocusArea
    }

    fun onFocusAreaEntered(area: String) {
        userInfo.preferredFocusArea = area
        _state.value = AichatState.Generating
        generateWorkoutPlan()
    }

    private fun generateWorkoutPlan() {
        viewModelScope.launch {
            recommendationCount++
            val result = getWorkoutSchedule(userInfo, scheduleInfo)
            lastGeneratedPlan = result
            _state.value = AichatState.ShowResult(result, isSecondTry = recommendationCount >= 2)
        }
    }

    fun onAcceptPlan() {
        viewModelScope.launch {
            val parsedList = parseRoutineToTriples(lastGeneratedPlan)
            repository.createPlanWithDetails(System.currentTimeMillis(), parsedList)
            _state.value = AichatState.Done
        }
    }

    fun onRejectPlan() {
        if (recommendationCount < 2) {
            _state.value = AichatState.Generating
            generateWorkoutPlan()
        } else {
            _state.value = AichatState.Rejected
        }
    }

    private fun parseRoutineToTriples(planText: String): List<Triple<String, Int?, Int?>> {
        val result = mutableListOf<Triple<String, Int?, Int?>>()
        val regex = Regex("\"(.*?)\"\\s*\"?(\\d+)?\"?회?\\s*\"?(\\d+)?\"?세트?")

        planText.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val name = match.groupValues[1]
                val reps = match.groupValues[2].toIntOrNull()
                val sets = match.groupValues[3].toIntOrNull()
                result.add(Triple(name, reps, sets))
            }
        }
        return result
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd E요일", Locale.KOREA)
        return sdf.format(Date())
    }
}