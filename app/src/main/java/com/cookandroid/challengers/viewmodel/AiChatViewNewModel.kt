package com.cookandroid.challengers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.screen.ChatUiMessage
import com.cookandroid.challengers.api.AiRoutineApi
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.model.ScheduleInfo
import com.cookandroid.challengers.model.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AiChatViewNewModel (
    private val api: AiRoutineApi,

) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages

    private val _state = MutableStateFlow(AiChatState.WELCOME)
    val state: StateFlow<AiChatState> = _state

    // 임시 저장값(날짜, 요일 등)
    var startDate: String = ""
    var endDate: String = ""
    var selectedDays: List<String> = emptyList()
    var focusArea: String = ""

    //상담 채팅 개수 추가
    private var consultCount = 0
    private var isConsultingActive = false

    // user id (임시로 1번)
    private val userId = 1

    // 시간 포맷
    private fun nowTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())


    // ==============================
    // 1) WELCOME API 호출
    // ==============================
    fun loadWelcomeMessage() {
        viewModelScope.launch {
            try {
                val response = api.getWelcomeMessage(mapOf("req" to "welcome"))
                if (response.isSuccessful) {
                    val msg = response.body()?.message ?: "안녕하세요!🔥"

                    // GPT가 2줄을 모두 만들어주므로 그대로 한 번만 추가
                    addBotMessage(msg, options = listOf("운동 스케줄 생성", "운동 상담"))

                    _state.value = AiChatState.SELECT_MAIN_OPTION
                }
            } catch (e: Exception) {
                addBotMessage("서버 연결에 문제가 있어요. 잠시 후 다시 시도해주세요.")
            }
        }
    }


    // ==============================
    // 2) 사용자 입력 처리
    // ==============================
    fun onUserSend(text: String) {
        addUserMessage(text)

        when (_state.value) {

            AiChatState.SELECT_MAIN_OPTION -> handleMainOption(text)

            AiChatState.CONSULTING_CHOICE -> handleConsultingChoice(text)

            AiChatState.ASK_OVERWRITE -> handleOverwriteOption(text)

            AiChatState.ASK_DAYS -> handleDaySelection(text)

            AiChatState.ASK_FOCUS -> handleFocusSelection(text)

            AiChatState.CONSULTING -> handleConsulting(text)

            AiChatState.SHOW_RECOMMENDED -> {
                when {
                    text.contains("다시") -> recommendExercise()
                    text.contains("종료") -> _state.value = AiChatState.EXIT
                    else -> addBotMessage("‘다시 생성하기’ 또는 ‘상담을 종료할게요’를 눌러주세요!")
                }
            }

            AiChatState.EXIT -> {
                // NavController.popBackStack() 연결 필요
            }

            else -> Unit
        }
    }


    // ==============================
    // 메인 옵션 선택 처리
    // ==============================
    private fun handleMainOption(text: String) {
        when {
            text.contains("스케줄") -> {
                checkExistingPlan()
            }
            text.contains("상담") -> {
                startConsulting()
            }
            else -> addBotMessage("아래 버튼에서 선택해주세요!")
        }
    }


    // ==============================
    // 3) 기존 플랜 여부 체크
    // ==============================
    private fun checkExistingPlan() {
        viewModelScope.launch {
            addBotMessage("운동 스케줄을 확인 중이에요…🔥")

            val today = "2025-01-01"  // 임시
            val nextWeek = "2025-01-07"

            try {
                val response = api.checkExistingPlan(
                    userId,
                    today,
                    nextWeek
                )

                if (response.isSuccessful) {
                    val exists = response.body()?.exists ?: false

                    if (exists) {
                        addBotMessage(
                            "기존 운동 루틴이 저장되어 있어요.\n새로 만들까요, 그대로 사용할까요?",
                            options = listOf("새롭게 생성할게요", "기존 루틴 사용할게요")
                        )
                        _state.value = AiChatState.ASK_OVERWRITE
                    } else {
                        goAskDate()
                    }
                }

            } catch (e: Exception) {
                addBotMessage("스케줄 확인 중 오류가 발생했어요!")
            }
        }
    }

    //운동 상담 채팅 3번 단위 마다 나타날 수 있도록 함.
    private fun handleConsultingChoice(text: String) {
        when {
            text.contains("이어가기") -> {
                consultCount = 0
                addBotMessage("좋아요! 계속 상담해볼까요? 어떤 운동 고민이 있으신가요?")
                _state.value = AiChatState.CONSULTING
            }

            text.contains("종료") -> {
                addBotMessage("상담을 종료했어요! 운동 화이팅🔥")
                _state.value = AiChatState.EXIT
            }
        }
    }
    //부위 선택 -> 추천 api 호출로 변경
    private fun recommendExercise() {
        viewModelScope.launch {
            addBotMessage("추천 운동을 생성하는 중이에요…🔥")

            try {
                val request = RetrofitClient.RecommendRequest(
                    userId = userId,
                    startDate = startDate,
                    endDate = endDate,
                    days = selectedDays,
                    focusArea = focusArea
                )

                val response = api.recommend(request)

                if (response.isSuccessful) {
                    val text = response.body()?.recommendation ?: "추천을 불러오지 못했어요!"

                    addBotMessage(text)
                    addBotMessage(
                        "이 추천이 마음에 드시나요?",
                        options = listOf("다시 생성하기", "상담을 종료할게요")
                    )

                    _state.value = AiChatState.SHOW_RECOMMENDED
                } else {
                    addBotMessage("추천 운동을 가져오는 중 오류가 발생했어요!")
                }

            } catch (e: Exception) {
                addBotMessage("서버와 연결할 수 없어요. 다시 시도해주세요 😢")
            }
        }
    }




    // ==============================
    // 덮어쓰기 선택 처리
    // ==============================
    private fun handleOverwriteOption(text: String) {
        when {
            text.contains("새롭게") -> {
                goAskDate()
            }
            text.contains("기존") -> {
                addBotMessage(
                    "좋아요! 혹시 더 상담하고 싶은 내용이 있을까요?",
                    options = listOf("운동 상담을 원해요", "상담을 종료할게요")
                )
                _state.value = AiChatState.SELECT_MAIN_OPTION
            }
        }
    }


    // ==============================
    // 날짜 선택 → 요일 선택 요청
    // ==============================
    fun onDateSelected(start: String, end: String) {
        startDate = start
        endDate = end

        addBotMessage(
            "좋아요! 그럼 이번 루틴은 어떤 요일에 운동하고 싶으세요?",
            options = listOf("월", "화", "수", "목", "금", "토", "일")
        )
        _state.value = AiChatState.ASK_DAYS
    }

    private fun handleDaySelection(text: String) {
        selectedDays = listOf(text)

        addBotMessage(
            "이번 루틴은 어떤 부위를 집중하고 싶나요?",
            options = listOf("상체", "하체", "복근", "전신")
        )
        _state.value = AiChatState.ASK_FOCUS
    }


    private fun handleFocusSelection(text: String) {
        focusArea = text
        recommendExercise()   // ← generateRoutine() 대신 이걸 호출!
    }


    // ==============================
    // GPT 운동 루틴 생성 API
    // ==============================
    private fun generateRoutine() {
        viewModelScope.launch {
            addBotMessage("운동 루틴을 생성하는 중이에요…🔥")

            val user = UserInfo(
                name = "홍길동",
                age_group = "20대",
                gender = "여성",
                height = 160,
                weight = 50,
                disease = "",
                exercise_level = "초급",
                preferred_exercises = listOf("스쿼트"),
                exercise_equipment = listOf("덤벨")
            )

            val schedule = ScheduleInfo(
                start_date = startDate,
                end_date = endDate,
                days_of_week = selectedDays,
                focus_area = focusArea
            )

            val request = com.cookandroid.challengers.model.AiRoutineRequest(
                user_info = user,
                schedule_info = schedule
            )

            val response = api.generateRoutine(request)

            if (response.isSuccessful) {
                val result = response.body()!!

                addBotMessage(result.routine_text)
                addBotMessage(
                    "이 루틴이 마음에 드시나요?",
                    options = listOf("다시 생성하기", "상담을 종료할게요")
                )
                _state.value = AiChatState.SHOW_RECOMMENDED
            }
        }
    }


    // ==============================
    // 운동 상담 모드
    // ==============================
    private fun startConsulting() {
        consultCount = 0
        isConsultingActive = true

        addBotMessage(
            "운동 상담 모드로 전환했어요! 어떤 운동 고민이 있으신가요?"
        )
        _state.value = AiChatState.CONSULTING
    }

    private fun handleConsulting(text: String) {
        viewModelScope.launch {
            try {
                // 1) 사용자 질문 → 서버로 전송
                val response = api.consult(mapOf("message" to text))

                if (response.isSuccessful) {
                    val answer = response.body()?.answer ?: "답변을 불러오지 못했어요!"

                    // 2) 상담 카운트 증가
                    consultCount++

                    // 3) 상담 횟수 < 3회 → 그냥 Bot 출력
                    if (consultCount < 3) {
                        addBotMessage(answer)
                        return@launch
                    }

                    // 4) 상담 3회 완료 → 4번째에는 선택 버튼 표시
                    addBotMessage(
                        answer + "\n\n상담을 이어가시겠어요?",
                        options = listOf("상담 이어가기", "상담 종료하기")
                    )

                    // 상태를 선택모드로 변경
                    _state.value = AiChatState.CONSULTING_CHOICE
                }
            } catch (e: Exception) {
                addBotMessage("상담 중 오류가 발생했어요! 잠시 후 다시 시도해주세요.")
            }
        }
    }


    // ==============================
    // 메시지 유틸
    // ==============================
    private fun addBotMessage(text: String, options: List<String> = emptyList()) {
        val new = ChatUiMessage.Bot(text, nowTime(), options)
        _messages.value = _messages.value + new
    }

    private fun addUserMessage(text: String) {
        val new = ChatUiMessage.User(text, nowTime())
        _messages.value = _messages.value + new
    }

    private fun goAskDate() {
        addBotMessage("새롭게 운동 루틴을 만들어볼까요?\n먼저 시작일과 종료일을 알려주세요!")

        viewModelScope.launch {
            delay(300)  // 메시지가 먼저 보이고
            _state.value = AiChatState.ASK_DATE
        }
    }
}

enum class AiChatState {
    WELCOME,
    SELECT_MAIN_OPTION,
    CHECK_EXISTING_PLAN,
    ASK_OVERWRITE,
    ASK_DATE,
    ASK_DAYS,
    ASK_FOCUS,
    SHOW_RECOMMENDED,
    CONSULTING,
    CONSULTING_CHOICE,     // 3회 후 상담 이어가기/종료
    EXIT
}