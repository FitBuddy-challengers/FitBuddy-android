package com.cookandroid.challengers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.ExerciseApi
import com.cookandroid.challengers.model.UserInfo
import com.cookandroid.challengers.model.ScheduleInfo
import com.cookandroid.challengers.repository.AiWorkoutRepository
import com.cookandroid.challengers.util.UserPreference
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

class AiChatViewModel(
    private val workoutRepository: AiWorkoutRepository,
    private val exerciseApi: ExerciseApi,
    private val userPreference: UserPreference
) : ViewModel() {

    private val _state = MutableStateFlow<AichatState>(AichatState.Welcome)
    val state: StateFlow<AichatState> = _state

    private var userId: Int = -1
    private lateinit var userInfo: UserInfo
    private val scheduleInfo = ScheduleInfo("", "", listOf(), "")

    private var lastGeneratedPlan: String = ""
    private var recommendationCount = 0
    private var createdPlanId: Long? = null

    fun getUserName(): String = if (::userInfo.isInitialized) userInfo.name else ""
    fun getFocusArea(): String = scheduleInfo.focus_area

    fun loadUserInfo() {
        userId = userPreference.getUserId()
        if (userId == -1) {
            _state.value = AichatState.Rejected
            return
        }

        viewModelScope.launch {
            try {
                val response = exerciseApi.getUserInfo(userId)
                if (response.isSuccessful) {
                    val fetchedUserInfo = response.body() ?: return@launch
                    userInfo = fetchedUserInfo
                    _state.value = AichatState.Welcome
                } else {
                    _state.value = AichatState.Rejected
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = AichatState.Rejected
            }
        }
    }

    fun onUserConfirmedStart() {
        _state.value = AichatState.AskDate
    }

    fun onUserDeclined() {
        _state.value = AichatState.Rejected
    }

    fun onDateSelected(start: String, end: String) {
        scheduleInfo.start_date = start
        scheduleInfo.end_date = end
        _state.value = AichatState.AskDays
    }

    fun onDaysSelected(days: List<String>) {
        scheduleInfo.days_of_week = days
        _state.value = AichatState.AskFocusArea
    }

    fun onFocusAreaEntered(area: String) {
        scheduleInfo.focus_area = area
        _state.value = AichatState.Generating
        generateWorkoutPlan()
    }

    private fun generateWorkoutPlan() {
        viewModelScope.launch {
            recommendationCount++

            val result = workoutRepository.getRoutineFromServer(userInfo, scheduleInfo)
            if (result == null) {
                _state.value = AichatState.Rejected
                return@launch
            }

            lastGeneratedPlan = result
            _state.value = AichatState.ShowResult(result, isSecondTry = recommendationCount >= 2)
        }
    }

    fun onAcceptPlan() {
        viewModelScope.launch {
            val parsedList = parseRoutineToTriples(lastGeneratedPlan)

            val allExercises = exerciseApi.getAllExercises()
            if (!allExercises.isSuccessful) {
                _state.value = AichatState.Rejected
                return@launch
            }

            val exerciseMap = allExercises.body()?.associateBy { it.name } ?: emptyMap()
            val convertedList = parsedList.mapNotNull { (name, reps, sets) ->
                val id = exerciseMap[name]?.id?.toInt() ?: return@mapNotNull null
                Triple(id, reps, sets)
            }

            if (createdPlanId == null) {
                createdPlanId = workoutRepository.createPlan(scheduleInfo.start_date)
                if (createdPlanId == null) {
                    _state.value = AichatState.Rejected
                    return@launch
                }
            }

            val routineDates = generateDates(scheduleInfo.start_date, scheduleInfo.end_date, scheduleInfo.days_of_week)

            for (date in routineDates) {
                val success = workoutRepository.addExercisesToSchedule(
                    planId = createdPlanId!!,
                    exerciseList = convertedList,
                    date = date
                )
                if (!success) {
                    _state.value = AichatState.Rejected
                    return@launch
                }
            }

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

    private fun generateDates(start: String, end: String, daysOfWeek: List<String>): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val startDate = sdf.parse(start)!!
        val endDate = sdf.parse(end)!!
        val cal = Calendar.getInstance()
        cal.time = startDate

        val result = mutableListOf<String>()
        val targetDays = daysOfWeek.map { dayStringToCalendarDay(it) }

        while (!cal.time.after(endDate)) {
            if (cal.get(Calendar.DAY_OF_WEEK) in targetDays) {
                result.add(sdf.format(cal.time))
            }
            cal.add(Calendar.DATE, 1)
        }
        return result
    }

    private fun dayStringToCalendarDay(day: String): Int {
        return when (day) {
            "일" -> Calendar.SUNDAY
            "월" -> Calendar.MONDAY
            "화" -> Calendar.TUESDAY
            "수" -> Calendar.WEDNESDAY
            "목" -> Calendar.THURSDAY
            "금" -> Calendar.FRIDAY
            "토" -> Calendar.SATURDAY
            else -> throw IllegalArgumentException("Invalid day: $day")
        }
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd E요일", Locale.KOREA)
        return sdf.format(Date())
    }
}