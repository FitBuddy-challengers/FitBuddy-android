package com.cookandroid.challengers.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.AiExercise
import com.cookandroid.challengers.api.AiPlanRequest
import com.cookandroid.challengers.api.ExerciseApi
import com.cookandroid.challengers.api.RetrofitClient.aiRoutineApi
import com.cookandroid.challengers.model.ChatMessage
import com.cookandroid.challengers.model.UserInfo
import com.cookandroid.challengers.model.ScheduleInfo
import com.cookandroid.challengers.repository.AiWorkoutRepository
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
//    fun showWelcomeMessage() {
//        if (!hasWelcomed && ::userInfo.isInitialized) {
//            val time = getCurrentTime()
//            addMessage(ChatMessage.FromBot("안녕하세요. ${userInfo.name}님!\nAI Buddy와 함께 운동 루틴을 계획하시겠어요?", time))
//            hasWelcomed = true
//        }

//    }
// 상태별 메시지 표시
//fun showWelcomeMessage() {
//    if (hasWelcomed) return  // ✅ 중복 방지 플래그
//
//    val userName = if (::userInfo.isInitialized) userInfo.name else "운동러"
//    val time = getCurrentTime()
//
//    // ✅ 문자열 템플릿 + 접미사는 따로 문자열에 포함
//    addMessage(ChatMessage.FromBot("안녕하세요, ${userName}님!\nAI Buddy와 함께 운동 루틴을 계획해 볼까요?", time))
//
//    hasWelcomed = true
//}

    //api 연동으로 수정! 더 자연스럽고 힘내는 응원 멘트 생성

    fun showWelcomeMessage() {
        if (hasWelcomed) return // ✅ 중복 방지

        viewModelScope.launch {
            val userName = if (::userInfo.isInitialized) userInfo.name else "운동러"
            val time = getCurrentTime()

            // ✅ 서버에서 인사 문구 요청
            val message = workoutRepository.fetchWelcomeMessage(userName)
                ?: "안녕하세요, ${userName}님! 오늘도 운동 화이팅🔥"

            addMessage(ChatMessage.FromBot(message, time))
            hasWelcomed = true
        }
    }

    fun showResultMessage(planText: String, isSecondTry: Boolean) {
        if (!hasShownResult) {
            val time = getCurrentTime()

            addMessage(ChatMessage.FromBot("${getUserName()}님을 위한 운동 스케줄이 준비되었어요!", time))

            //  ‘•’ 제거한 포맷
            val formatted = planText.lines()
                .filter { it.isNotBlank() }
                .joinToString("\n") { it.trim() }

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

//    fun onDaysSelected(days: List<String>) {
//        scheduleInfo.days_of_week = days
//        _state.value = AichatState.AskFocusArea //문제! 바로 다음 상태로 넘어감.
//
//    }
    fun onDaysSelected(daysInput: List<String>) {
        val normalizedDays = daysInput.map { normalizeDayInput(it) }
        scheduleInfo.days_of_week = normalizedDays

//        val dayText = normalizedDays.joinToString(", ")
//        addMessage(
//            ChatMessage.FromBot(
//                "좋아요! ${dayText}에 운동 루틴을 잡을게요 💪",
//                getCurrentTime()
//            )
//        )

        // ✅ 1초 후 자연스럽게 다음 질문 표시 -> 바로 나오면 어색함.
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000L)
            _state.value = AichatState.AskFocusArea
        }
    }


    fun onFocusAreaEntered(area: String) {
        scheduleInfo.focus_area = area
        _state.value = AichatState.Generating
        generateWorkoutPlan()
    }

    private fun generateWorkoutPlan() {
        Log.d("AiChatVM", "🧾 GPT로부터 받은 planText:\n$lastGeneratedPlan")
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


            // 1️⃣ GPT 텍스트 파싱
            val parsedList = parseRoutineToTriples(lastGeneratedPlan)
            Log.d("AiChatVM", "✅ 파싱된 운동 리스트: ${parsedList.size}개")
            parsedList.forEach {
                Log.d("AiChatVM", "🧩 파싱: ${it.first} - ${it.second ?: "-"}회 x ${it.third ?: "-"}세트")
            }

            // 2️⃣ 서버에 있는 운동 전체 목록 조회
            val allExercisesResponse = exerciseApi.getAllExercises()
            if (!allExercisesResponse.isSuccessful) {
                Log.e("ChatDebug", "❌ 운동 목록 조회 실패")
                _state.value = AichatState.Rejected
                return@launch
            }

            val exerciseMap = allExercisesResponse.body()?.associateBy { it.name } ?: emptyMap()
            Log.d("AiChatVM", "📦 매핑 시작: parsedList = ${parsedList.size}개, exerciseMap = ${exerciseMap.size}개")
            parsedList.forEach { (name, _, _) ->
                val exists = exerciseMap.containsKey(name)
                Log.d("AiChatVM", "🔍 $name → 매핑 ${if (exists) "성공" else "실패"}")
            }

            // 3️⃣ reps/sets → AiExercise 로 변환 (is_time_type 기준으로 seconds or reps 분기)
            val aiExercises = parsedList.mapNotNull { (name, reps, sets) ->
                val exercise = exerciseMap[name] ?: return@mapNotNull null
                if (exercise.isTimeType == true) {
                    // 시간 기반 운동
                    val seconds = reps ?: 30 // "30회" → "30초"로 간주
                    AiExercise(name = name, seconds = seconds)
                } else {
                    // 반복 기반 운동
                    AiExercise(name = name, reps = reps, sets = sets)
                }
            }
            // ✅ 여기 로그 추가!!!
            Log.d("AiChatVM", "✅ 전송할 루틴: ${aiExercises.size}개")
            aiExercises.forEach {
                Log.d("AiChatVM", "📝 ${it.name} - ${it.sets ?: "-"}세트 x ${it.reps ?: it.seconds ?: "-"}")
            }

            // 4️⃣ 서버에 전송
            submitAiRoutine(userId, scheduleInfo.start_date, aiExercises)
        }
    }
//            val parsedList = parseRoutineToTriples(lastGeneratedPlan)
//
//            val allExercises = exerciseApi.getAllExercises()
//            if (!allExercises.isSuccessful) {
//                _state.value = AichatState.Rejected
//                return@launch
//            }
//
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
//            is AichatState.AskDays -> {
//                addMessage(ChatMessage.FromBot("어떤 요일에 운동하실 건가요?\n예: 월 수 금", time))
//            }
            is AichatState.AskDays -> {
                // 자연스럽고 코치 톤으로 변경
                addMessage(
                    ChatMessage.FromBot(
                        "💪 이번 주 운동 스케줄을 정해볼까요?\n운동할 요일을 지정해주세요!\n예: 월 수 금 또는 Mon Wed Fri",
                        time
                    )
                )
                return // 자동 전환 방지
            }
            is AichatState.AskFocusArea -> {
                addMessage(ChatMessage.FromBot("이번엔 집중해서 단련하고 싶은 부위가 있을까요?\n" +
                        "\uD83D\uDCAA 예: 하체 / 복근 / 가슴", time))
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

        val koreanRegex = Regex("""\d+\.\s*(.+?)[\:\-\s]\s*(\d+)[회초]?\s*(\d+)?세트""")
        val englishRegex = Regex("""\d+\.\s*(.+?)\s*-\s*(\d+)\s*sets\s*(of)?\s*(\d+)\s*(reps|times)?""", RegexOption.IGNORE_CASE)

        planText.lines().forEach { line ->
            val korMatch = koreanRegex.find(line)
            if (korMatch != null) {
                val name = korMatch.groupValues[1].trim()
                val reps = korMatch.groupValues[2].toIntOrNull()
                val sets = korMatch.groupValues.getOrNull(3)?.toIntOrNull()
                result.add(Triple(name, reps, sets))
                return@forEach
            }

            val engMatch = englishRegex.find(line)
            if (engMatch != null) {
                val name = engMatch.groupValues[1].trim()
                val sets = engMatch.groupValues[2].toIntOrNull()
                val reps = engMatch.groupValues[4].toIntOrNull()
                result.add(Triple(name, reps, sets))
            }
        }

        return result
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

    private fun normalizeDayInput(input: String): String {
        return when (input.trim().lowercase()) {
            "월", "월요일", "mon", "monday" -> "월"
            "화", "화요일", "tue", "tuesday" -> "화"
            "수", "수요일", "wed", "wednesday" -> "수"
            "목", "목요일", "thu", "thursday" -> "목"
            "금", "금요일", "fri", "friday" -> "금"
            "토", "토요일", "sat", "saturday" -> "토"
            "일", "일요일", "sun", "sunday" -> "일"
            else -> input
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
    fun submitAiRoutine(userId: Int, startDate: String, exercises: List<AiExercise>) {
        viewModelScope.launch {
            val request = AiPlanRequest(
                user_id = userId,
                start_date = startDate,
                end_date = startDate,
                exercises = exercises
            )
            val result = workoutRepository.submitAiPlan(request)
            if (result != null) {
                Log.d("AiChatVM", "🎉 루틴 저장 완료: planId=${result.plan_id}")
                _state.value = AichatState.Done
                // ✅ 필요 시 result.schedules, result.reps 등 사용 가능
            } else {
                Log.e("AiChatVM", "💥 루틴 저장 실패")
                _state.value = AichatState.Rejected
            }
        }
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