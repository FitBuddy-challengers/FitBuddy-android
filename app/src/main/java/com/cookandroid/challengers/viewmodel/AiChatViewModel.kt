package com.cookandroid.challengers.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.ExerciseApi
import com.cookandroid.challengers.model.ChatMessage
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

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> get() = _messages

    private var hasWelcomed = false
    private var hasShownResult = false

    private var userId: Int = -1
    private lateinit var userInfo: UserInfo
    private val scheduleInfo = ScheduleInfo("", "", listOf(), "")

    private var lastGeneratedPlan: String = ""
    private var recommendationCount = 0
    private var createdPlanId: Long? = null

    fun addMessage(message: ChatMessage) {
        Log.d("ChatDebug", "💬 Adding message: ${message.javaClass.simpleName}")
        _messages.value = _messages.value + message
    }

    fun getUserName(): String = if (::userInfo.isInitialized) userInfo.name else ""
    fun getFocusArea(): String = scheduleInfo.focus_area


    // 비동기 관련 오류 수정
    fun loadUserInfo() {
        userId = userPreference.getUserId()
        Log.d("ChatDebug", "🔍 Loading user info, userId: $userId")

        if (userId == -1) {
            Log.e("ChatDebug", "❌ Invalid userId")
            _state.value = AichatState.Rejected
            return
        }

        viewModelScope.launch {
            try {
                val response = exerciseApi.getUserInfo(userId)
                if (response.isSuccessful) {
                    val fetchedUserInfo = response.body()
                    if (fetchedUserInfo != null) {
                        userInfo = fetchedUserInfo
                        Log.d("ChatDebug", "✅ User info loaded: ${userInfo.name}")

                        // 사용자 정보 로드 완료 후 Welcome 상태로 전환
                        hasWelcomed = false // 플래그 리셋
                        _state.value = AichatState.Welcome
                    } else {
                        Log.e("ChatDebug", "❌ User info response body is null")
                        _state.value = AichatState.Rejected
                    }
                } else {
                    Log.e("ChatDebug", "❌ Failed to load user info: ${response.code()}")
                    _state.value = AichatState.Rejected
                }
            } catch (e: Exception) {
                Log.e("ChatDebug", "❌ Exception loading user info", e)
                _state.value = AichatState.Rejected
            }
        }
    }

    // 상태별 메시지 표시
    fun showWelcomeMessage() {
        if (!hasWelcomed && ::userInfo.isInitialized) {
            val time = getCurrentTime()
            addMessage(ChatMessage.FromBot("안녕하세요. ${userInfo.name}님!\nAI Buddy와 함께 운동 루틴을 계획하시겠어요?", time))
            hasWelcomed = true
        }
    }

    fun showResultMessage(planText: String, isSecondTry: Boolean) {
        if (!hasShownResult) {
            val time = getCurrentTime()
            addMessage(ChatMessage.FromBot("${getUserName()}님을 위한 운동 스케줄이 준비되었어요!", time))

            val formatted = planText.lines()
                .filter { it.isNotBlank() }
                .joinToString("\n") { "• $it" }

            addMessage(ChatMessage.FromBot(formatted, time))
            addMessage(ChatMessage.FromBot("이대로 할게요 / 다시 추천해 주세요", time))
            hasShownResult = true
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
        hasShownResult = false // 새로운 결과 생성 시 플래그 리셋

        viewModelScope.launch {
            recommendationCount++
            Log.d("ChatDebug", "🏋️ Generating workout plan, attempt: $recommendationCount")

            val result = workoutRepository.getRoutineFromServer(userInfo, scheduleInfo)
            if (result == null) {
                Log.e("ChatDebug", "❌ Failed to generate workout plan")
                _state.value = AichatState.Rejected
                return@launch
            }

            lastGeneratedPlan = result
            Log.d("ChatDebug", "✅ Workout plan generated")
            _state.value = AichatState.ShowResult(result, isSecondTry = recommendationCount >= 2)
        }
    }

    fun onAcceptPlan() {
        viewModelScope.launch {
            Log.d("ChatDebug", "✅ User accepted plan")
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
        Log.d("ChatDebug", "❌ User rejected plan, count: $recommendationCount")
        if (recommendationCount < 2) {
            // 다시 생성 시도
            _state.value = AichatState.Generating
            generateWorkoutPlan()
        } else {
            _state.value = AichatState.Rejected
        }
    }

    // 상태별 자동 메시지 표시
    fun handleStateMessage(state: AichatState) {
        val time = getCurrentTime()

        when (state) {
            is AichatState.AskDate -> {
                addMessage(ChatMessage.FromBot("운동 시작일과 마감일을 지정해 주세요!", time))
            }
            is AichatState.AskDays -> {
                addMessage(ChatMessage.FromBot("어떤 요일에 운동하실 건가요?\n예: 월 수 금", time))
            }
            is AichatState.AskFocusArea -> {
                addMessage(ChatMessage.FromBot("특별히 강화하고 싶은 부위가 있나요?\n예: 하체 / 복근 / 가슴", time))
            }
            is AichatState.Generating -> {
                addMessage(ChatMessage.FromBot("${getUserName()}님의 루틴을 생성하고 있어요. 잠시만 기다려 주세요...", time))
            }
            is AichatState.Done -> {
                addMessage(ChatMessage.FromBot("좋아요! 생성된 루틴으로 즐거운 운동을 시작해 보세요!", time))
            }
            is AichatState.Rejected -> {
                addMessage(ChatMessage.FromBot("추천한 루틴이 마음에 들지 않으셨나요?\n직접 추가해 보세요.", time))
            }
            else -> Unit
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

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}

//package com.cookandroid.challengers.viewmodel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.cookandroid.challengers.api.ExerciseApi
//import com.cookandroid.challengers.model.ChatMessage
//import com.cookandroid.challengers.model.UserInfo
//import com.cookandroid.challengers.model.ScheduleInfo
//import com.cookandroid.challengers.repository.AiWorkoutRepository
//import com.cookandroid.challengers.util.UserPreference
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.*
//
//sealed class AichatState {
//    // 단계별 대화 상태: 입력 요청 등
//    object Welcome : AichatState()
//    object AskDate : AichatState()
//    object AskDays : AichatState()
//    object AskFocusArea : AichatState()
//    object Generating : AichatState()
//    data class ShowResult(val planText: String, val isSecondTry: Boolean = false) : AichatState()
//    object Done : AichatState()
//    object Rejected : AichatState()
//}
//
//
//
//
//class AiChatViewModel(
//    private val workoutRepository: AiWorkoutRepository,
//    private val exerciseApi: ExerciseApi,
//    private val userPreference: UserPreference
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<AichatState>(AichatState.Welcome)
//    val state: StateFlow<AichatState> = _state
//
//    // 1. 메시지 리스트 상태 추가
//    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
//    val messages: StateFlow<List<ChatMessage>> get() = _messages
//
//    // 2. 메시지 추가 함수
//    fun addMessage(message: ChatMessage) {
//        _messages.value = _messages.value + message
//    }
//
//    var hasWelcomed = false // ⚠️ 첫 Welcome 메시지 출력 여부
//    var hasShownResult = false // ⚠️ 결과 메시지 중복 방지용
//
//    private var userId: Int = -1
//    private lateinit var userInfo: UserInfo
//    private val scheduleInfo = ScheduleInfo("", "", listOf(), "")
//
//    private var lastGeneratedPlan: String = ""
//    private var recommendationCount = 0
//    private var createdPlanId: Long? = null
//
//    fun getUserName(): String = if (::userInfo.isInitialized) userInfo.name else ""
//    fun getFocusArea(): String = scheduleInfo.focus_area
//
//    // 사용자 정보 로드 후 첫인사로 설정
//    fun loadUserInfo() {
//        userId = userPreference.getUserId()
//        if (userId == -1) {
//            _state.value = AichatState.Rejected
//            Log.d("Debug", "📛 userId: ${userPreference.getUserId()}")  // -1이라면 비정상
//            return
//        }
//
//        viewModelScope.launch {
//            try {
//                val response = exerciseApi.getUserInfo(userId)
//                if (response.isSuccessful) {
//                    val fetchedUserInfo = response.body() ?: return@launch
//                    userInfo = fetchedUserInfo
//                    _state.value = AichatState.Welcome
//                } else {
//                    _state.value = AichatState.Rejected
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                _state.value = AichatState.Rejected
//            }
//        }
//    }
//
//    fun onUserConfirmedStart() {
//        _state.value = AichatState.AskDate
//    }
//
//    fun onUserDeclined() {
//        _state.value = AichatState.Rejected
//    }
//
//    fun onDateSelected(start: String, end: String) {
//        scheduleInfo.start_date = start
//        scheduleInfo.end_date = end
//        _state.value = AichatState.AskDays
//    }
//
//    fun onDaysSelected(days: List<String>) {
//        scheduleInfo.days_of_week = days
//        _state.value = AichatState.AskFocusArea
//    }
//
//    fun onFocusAreaEntered(area: String) {
//        scheduleInfo.focus_area = area
//        _state.value = AichatState.Generating
//        generateWorkoutPlan()
//    }
//
//    // 루틴 생성 요청, 결과 반영
//    private fun generateWorkoutPlan() {
//        viewModelScope.launch {
//            recommendationCount++
//
//            val result = workoutRepository.getRoutineFromServer(userInfo, scheduleInfo)
//            if (result == null) {
//                _state.value = AichatState.Rejected
//                return@launch
//            }
//
//            lastGeneratedPlan = result
//            _state.value = AichatState.ShowResult(result, isSecondTry = recommendationCount >= 2)
//        }
//    }
//
//    // !! plan text 파싱 후 저장 시도, 리스트 변환
//    fun onAcceptPlan() {
//        viewModelScope.launch {
//            val parsedList = parseRoutineToTriples(lastGeneratedPlan)
//
//            // 전체 운동 정보
//            val allExercises = exerciseApi.getAllExercises()
//            if (!allExercises.isSuccessful) {
//                _state.value = AichatState.Rejected
//                return@launch
//            }
//
//            // 운동 이름 -> id 변환
//            val exerciseMap = allExercises.body()?.associateBy { it.name } ?: emptyMap()
//            val convertedList = parsedList.mapNotNull { (name, reps, sets) ->
//                val id = exerciseMap[name]?.id?.toInt() ?: return@mapNotNull null
//                Triple(id, reps, sets)
//            }
//
//            if (createdPlanId == null) {
//                createdPlanId = workoutRepository.createPlan(scheduleInfo.start_date)
//                if (createdPlanId == null) {
//                    _state.value = AichatState.Rejected
//                    return@launch
//                }
//            }
//
//            // 날짜 계산 후 루틴 저장
//            val routineDates = generateDates(scheduleInfo.start_date, scheduleInfo.end_date, scheduleInfo.days_of_week)
//
//            for (date in routineDates) {
//                val success = workoutRepository.addExercisesToSchedule(
//                    planId = createdPlanId!!,
//                    exerciseList = convertedList,
//                    date = date
//                )
//                if (!success) {
//                    _state.value = AichatState.Rejected
//                    return@launch
//                }
//            }
//
//            _state.value = AichatState.Done
//        }
//    }
//
//    // 사용자 거절 시
//    fun onRejectPlan() {
//        if (recommendationCount < 2) {
//            _state.value = AichatState.Generating
//            generateWorkoutPlan()
//        } else {
//            hasShownResult = false // 다시 추천할 때 ShowResult 다시 보여주기 위해 리셋
//            _state.value = AichatState.Rejected
//        }
//    }
//
//    // 스케줄 텍스트 파싱 후 분해
//    private fun parseRoutineToTriples(planText: String): List<Triple<String, Int?, Int?>> {
//        val result = mutableListOf<Triple<String, Int?, Int?>>()
//        val regex = Regex("\"(.*?)\"\\s*\"?(\\d+)?\"?회?\\s*\"?(\\d+)?\"?세트?")
//        planText.lines().forEach { line ->
//            val match = regex.find(line)
//            if (match != null) {
//                val name = match.groupValues[1]
//                val reps = match.groupValues[2].toIntOrNull()
//                val sets = match.groupValues[3].toIntOrNull()
//                result.add(Triple(name, reps, sets))
//            }
//        }
//        return result
//    }
//
//    // 날짜 리스트 생성(사용자 선택 바탕)
//    private fun generateDates(start: String, end: String, daysOfWeek: List<String>): List<String> {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
//        val startDate = sdf.parse(start)!!
//        val endDate = sdf.parse(end)!!
//        val cal = Calendar.getInstance()
//        cal.time = startDate
//
//        val result = mutableListOf<String>()
//        val targetDays = daysOfWeek.map { dayStringToCalendarDay(it) }
//
//        while (!cal.time.after(endDate)) {
//            if (cal.get(Calendar.DAY_OF_WEEK) in targetDays) {
//                result.add(sdf.format(cal.time))
//            }
//            cal.add(Calendar.DATE, 1)
//        }
//        return result
//    }
//
//    private fun dayStringToCalendarDay(day: String): Int {
//        return when (day) {
//            "일" -> Calendar.SUNDAY
//            "월" -> Calendar.MONDAY
//            "화" -> Calendar.TUESDAY
//            "수" -> Calendar.WEDNESDAY
//            "목" -> Calendar.THURSDAY
//            "금" -> Calendar.FRIDAY
//            "토" -> Calendar.SATURDAY
//            else -> throw IllegalArgumentException("Invalid day: $day")
//        }
//    }
//
//    fun getTodayString(): String {
//        val sdf = SimpleDateFormat("yyyy.MM.dd E요일", Locale.KOREA)
//        return sdf.format(Date())
//    }
//}