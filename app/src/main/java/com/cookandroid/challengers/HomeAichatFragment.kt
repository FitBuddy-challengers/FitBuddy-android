package com.cookandroid.challengers

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentHomeAichatBinding
import com.cookandroid.challengers.repository.AiWorkoutRepository
import com.cookandroid.challengers.util.UserPreference
import com.cookandroid.challengers.viewmodel.AiChatViewModel
import com.cookandroid.challengers.viewmodel.AiChatViewModelFactory
import com.cookandroid.challengers.viewmodel.AichatState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.cookandroid.challengers.model.ChatMessage

class HomeAichatFragment : Fragment() {

    private var _binding: FragmentHomeAichatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter

    // ViewModel 초기화 (Factory 사용)
    private val viewModel: AiChatViewModel by viewModels {
        val userPref = UserPreference(requireContext())
        AiChatViewModelFactory(
            workoutRepository = AiWorkoutRepository(
                scheduleApi = RetrofitClient.scheduleApi,
                aiRoutineApi = RetrofitClient.aiRoutineApi
            ),
            exerciseApi = RetrofitClient.exerciseApi,
            userPreference = userPref
        )
    }

    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeAichatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupDateHeader()
        setupClickListeners()
        observeViewModel()

        observeMessages() //  메시지 옵저빙 함수 호출

        // 사용자 정보 불러오기
        viewModel.loadUserInfo()

    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter() // 개선된 어댑터는 내부에서 메시지를 관리함
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter


    }

    private fun setupDateHeader() {
        val today = SimpleDateFormat("yyyy.MM.dd E", Locale.getDefault()).format(Date())
        binding.tvDate.text = today
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chatInputLayout.setEndIconOnClickListener {
            if (viewModel.state.value is AichatState.Generating) return@setEndIconOnClickListener

            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                binding.etMessage.text = null
                val time = getCurrentTime()
                viewModel.addMessage(ChatMessage.FromUser(message, time))
                handleUserInput(message)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                Log.d("ChatDebug", "🔄 State changed to: $state")

                binding.chatInputLayout.isEnabled = state !is AichatState.Generating

                when (state) {
                    is AichatState.Welcome -> {
                        Log.d("ChatDebug", "👋 상태: Welcome → 인사 메시지 출력 시도")
                        viewModel.showWelcomeMessage()
                    }

                    is AichatState.AskDate -> {
                        Log.d("ChatDebug", "📅 상태: AskDate → 날짜 선택 요청 메시지 출력 + DatePicker 호출")
                        viewModel.handleStateMessage(state)
                        showDatePicker(isStart = true)
                    }

                    is AichatState.AskDays -> {
                        Log.d("ChatDebug", "📆 상태: AskDays → 요일 입력 메시지 출력")
                        viewModel.handleStateMessage(state)
                    }

                    is AichatState.AskFocusArea -> {
                        Log.d("ChatDebug", "💪 상태: AskFocusArea → 부위 입력 메시지 출력")
                        viewModel.handleStateMessage(state)
                    }

                    is AichatState.Generating -> {
                        Log.d("ChatDebug", "⚙️ 상태: Generating → GPT 요청 전송 전 메시지 출력")
                        viewModel.handleStateMessage(state)
                    }

                    is AichatState.ShowResult -> {
                        Log.d("ChatDebug", "📦 상태: ShowResult → GPT 응답 출력 준비 (2회 이상?: ${state.isSecondTry})")
                        viewModel.showResultMessage(state.planText, state.isSecondTry)
                    }

                    is AichatState.Done -> {
                        Log.d("ChatDebug", "✅ 상태: Done → 루틴 저장 완료, 홈으로 이동 예정")
                        viewModel.handleStateMessage(state)
                        // 잠깐 기다린 후 navigate
                        kotlinx.coroutines.delay(1000)
                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
                    }

                    is AichatState.Rejected -> {
                        Log.d("ChatDebug", "❌ 상태: Rejected → 사용자 거절 또는 실패, 홈으로 이동 예정")
                        viewModel.handleStateMessage(state)
                        kotlinx.coroutines.delay(1000)
                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
                    }
                }
            }
        }
    }





//    private fun observeViewModel() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewModel.state.collectLatest { state ->
//                Log.d("ChatDebug", "🪠 Current status: $state")
//
//                val time = getCurrentTime()
//                binding.chatInputLayout.isEnabled = state !is AichatState.Generating
//
//                when (state) {
//                    is AichatState.Welcome -> {
//                        // ⚠️ 중복 방지: Welcome 메시지는 1회만 출력
//                        if (!viewModel.hasWelcomed) {
//                            viewModel.addMessage(ChatMessage.FromBot("안녕하세요. ${viewModel.getUserName()}님!\nAI Buddy와 함께 운동 루틴을 계획하시겠어요?", time))
//                            viewModel.hasWelcomed = true // ✅ 출력 완료 플래그
//                        }
//                    }
//
//                    is AichatState.AskDate -> {
//                        viewModel.addMessage(ChatMessage.FromBot("운동 시작일과 마감일을 지정해 주세요!", time))
//                        showDatePicker(isStart = true)
//                    }
//
//                    is AichatState.AskDays -> {
//                        viewModel.addMessage(ChatMessage.FromBot("어떤 요일에 운동하실 건가요?\n예: 월 수 금", time))
//                    }
//
//                    is AichatState.AskFocusArea -> {
//                        viewModel.addMessage(ChatMessage.FromBot("특별히 강화하고 싶은 부위가 있나요?\n예: 하체 / 복근 / 가슴", time))
//                    }
//
//                    is AichatState.Generating -> {
//                        viewModel.addMessage(ChatMessage.FromBot("${viewModel.getUserName()}님의 루틴을 생성하고 있어요. 잠시만 기다려 주세요...", time))
//                    }
//
//                    is AichatState.ShowResult -> {
//                        // ⚠️ 중복 방지: 결과 메시지는 1회만 출력
//                        if (!viewModel.hasShownResult) {
//                            viewModel.addMessage(ChatMessage.FromBot("${viewModel.getUserName()}님을 위한 운동 스케줄이 준비되었어요!", time))
//
//                            val formatted = state.planText.lines()
//                                .joinToString("\n") { "• $it" }
//
//                            viewModel.addMessage(ChatMessage.FromBot(formatted, time))
//                            viewModel.addMessage(ChatMessage.FromBot("이대로 할게요 / 다시 추천해 주세요", time))
//
//                            viewModel.hasShownResult = true // ✅ 출력 완료 플래그
//                        }
//                    }
//
//                    is AichatState.Done -> {
//                        viewModel.addMessage(ChatMessage.FromBot("좋아요. 생성된 루틴으로 즐거운 운동을 시작해 보세요!", time))
//                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
//                    }
//
//                    is AichatState.Rejected -> {
//                        viewModel.addMessage(ChatMessage.FromBot("추천한 루틴이 마음에 들지 않으셨나요?\n직접 추가해 보세요.", time))
//                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
//                    }
//                }
//            }
//        }
//    }
// 메시지 리스트 옵저버 함수 추가
    private fun observeMessages() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { messages: List<ChatMessage> ->
                Log.d("ChatDebug", "📩 메시지 옵저빙됨: ${messages.size}개") // 로그 추가
                adapter.setMessages(messages)
                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }



    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun handleUserInput(input: String) {
        if (viewModel.state.value is AichatState.Generating) return

        when (viewModel.state.value) {
            is AichatState.Welcome -> {
                val inputLower = input.lowercase()
                val positiveKeywords = listOf("네", "예", "좋아", "yes", "y", "응", "ㅇㅋ", "그래", "시작")
                val negativeKeywords = listOf("아니", "no", "싫어", "안 해", "거절")

                when {
                    positiveKeywords.any { inputLower.contains(it) } -> viewModel.onUserConfirmedStart()
                    negativeKeywords.any { inputLower.contains(it) } -> viewModel.onUserDeclined()
                    else -> {
                        val time = getCurrentTime()
                        viewModel.addMessage(
                            ChatMessage.FromBot("운동 루틴을 시작하시겠어요?\n예: 네 / 예 / 좋아요", time)
                        )
                    }
                }
            }

//            is AichatState.AskDays -> {
//                val days = input.split(" ", ",", "\n").mapNotNull {
//                    it.trim().takeIf { it.isNotEmpty() }
//                }
//                if (days.isNotEmpty()) {
//                    viewModel.onDaysSelected(days)
//                } else {
//                    val time = getCurrentTime()
//                    viewModel.addMessage(ChatMessage.FromBot("요일을 입력해 주세요. 예: 월 수 금", time))
//                }
//            }
            is AichatState.AskDays -> {
                val dayMapping = mapOf(
                    "mon" to "월", "monday" to "월",
                    "tue" to "화", "tuesday" to "화",
                    "wed" to "수", "wednesday" to "수",
                    "thu" to "목", "thursday" to "목",
                    "fri" to "금", "friday" to "금",
                    "sat" to "토", "saturday" to "토",
                    "sun" to "일", "sunday" to "일"
                )

                val days = input.split(" ", ",", "\n").mapNotNull { raw ->
                    val trimmed = raw.trim().lowercase()
                    when {
                        trimmed in dayMapping -> dayMapping[trimmed]
                        trimmed in listOf("월", "화", "수", "목", "금", "토", "일") -> trimmed
                        else -> null
                    }
                }

                if (days.isNotEmpty()) {
                    viewModel.onDaysSelected(days)

                    val time = getCurrentTime()
                    val koreanDays = days.joinToString(" ")
                    viewModel.addMessage(
                        ChatMessage.FromBot("좋아요! ${koreanDays} 요일에 운동하겠습니다 💪", time)
                    )
                } else {
                    val time = getCurrentTime()
                    viewModel.addMessage(ChatMessage.FromBot("요일을 입력해 주세요. 예: 월 수 금 또는 Mon Wed Fri", time))
                }
            }

            is AichatState.AskFocusArea -> {
                if (input.isNotBlank()) {
                    viewModel.onFocusAreaEntered(input)
                }
            }

            is AichatState.ShowResult -> {
                val inputLower = input.lowercase()
                val acceptKeywords = listOf("이대로", "좋아", "승인", "ok", "ㅇㅋ", "예", "네", "할게요")
                val rejectKeywords = listOf("다시", "재생성", "별로", "싫어", "다른")

                when {
                    acceptKeywords.any { inputLower.contains(it) } -> viewModel.onAcceptPlan()
                    rejectKeywords.any { inputLower.contains(it) } -> viewModel.onRejectPlan()
                    else -> {
                        val time = getCurrentTime()
                        viewModel.addMessage(
                            ChatMessage.FromBot("'이대로 할게요' 또는 '다시 추천해 주세요' 중 하나를 입력해 주세요!", time)
                        )
                    }
                }
            }

            else -> Unit
        }
//        when (viewModel.state.value) {
//            is AichatState.Welcome -> {
//                if (input.contains("네", ignoreCase = true) ||
//                    input.contains("예", ignoreCase = true) ||
//                    input.contains("좋아", ignoreCase = true)) {
//                    viewModel.onUserConfirmedStart()
//                } else {
//                    viewModel.onUserDeclined()
//                }
//            }
//            is AichatState.AskDays -> {
//                val days = input.split(" ", ",", "\n").mapNotNull {
//                    it.trim().takeIf { it.isNotEmpty() }
//                }
//                if (days.isNotEmpty()) {
//                    viewModel.onDaysSelected(days)
//                } else {
//                    val time = getCurrentTime()
//                    viewModel.addMessage(ChatMessage.FromBot("요일을 입력해 주세요. 예: 월 수 금", time))
//                }
//            }
//            is AichatState.AskFocusArea -> {
//                if (input.isNotBlank()) {
//                    viewModel.onFocusAreaEntered(input)
//                }
//            }
//            is AichatState.ShowResult -> {
//                when {
//                    input.contains("이대로", ignoreCase = true) ||
//                            input.contains("좋아", ignoreCase = true) ||
//                            input.contains("승인", ignoreCase = true) -> {
//                        viewModel.onAcceptPlan()
//                    }
//                    input.contains("다시", ignoreCase = true) ||
//                            input.contains("재생성", ignoreCase = true) -> {
//                        viewModel.onRejectPlan()
//                    }
//                    else -> {
//                        val time = getCurrentTime()
//                        viewModel.addMessage(
//                            ChatMessage.FromBot("'이대로 할게요' 또는 '다시 추천해 주세요' 중 하나를 입력해 주세요!", time)
//                        )
//                    }
//                }
//            }
//            else -> Unit
//        }
    }

//    private fun handleUserInput(input: String) {
//        if (viewModel.state.value is AichatState.Generating) return
//
//        when (viewModel.state.value) {
//            is AichatState.Welcome -> {
//                if (input.contains("네", ignoreCase = true)) viewModel.onUserConfirmedStart()
//                else viewModel.onRejectPlan()
//            }
//            is AichatState.AskDays -> {
//                val days = input.split(" ", ",").mapNotNull {
//                    it.trim().takeIf { it.isNotEmpty() }
//                }
//                viewModel.onDaysSelected(days)
//            }
//            is AichatState.AskFocusArea -> {
//                viewModel.onFocusAreaEntered(input)
//            }
//            is AichatState.ShowResult -> {
//                when {
//                    input.contains("이대로", ignoreCase = true) -> viewModel.onAcceptPlan()
//                    input.contains("다시", ignoreCase = true) -> viewModel.onFocusAreaEntered(viewModel.getFocusArea())
//                    else -> viewModel.addMessage(
//                        ChatMessage.FromBot("‘이대로 할게요’ 또는 ‘다시 추천해 주세요’ 중 하나를 입력해 주시면 도와드릴게요", getCurrentTime())
//                    )
//                }
//            }
//            else -> Unit
//        }
//    }

    // HomeAichatFragment에서 날짜 선택 처리
    private fun showDatePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

                if (isStart) {
                    startDate = selectedDate
                    val time = getCurrentTime()
                    viewModel.addMessage(ChatMessage.FromUser("시작일: $selectedDate", time))

                    // 종료일 선택
                    showDatePicker(isStart = false)
                } else {
                    endDate = selectedDate
                    val time = getCurrentTime()
                    viewModel.addMessage(ChatMessage.FromUser("종료일: $selectedDate", time))

                    viewModel.onDateSelected(startDate, endDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (isStart) {
                datePicker.minDate = System.currentTimeMillis() // 오늘 이후만 선택 가능
            } else {
                // 시작일 이후만 선택 가능
                val startCal = Calendar.getInstance()
                startCal.time = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(startDate)!!
                datePicker.minDate = startCal.timeInMillis
            }
        }.show()
    }

//    private fun showDatePicker(isStart: Boolean) {
//        val cal = Calendar.getInstance()
//
//        val datePicker = DatePickerDialog(
//            requireContext(),
//            { _, year, month, day ->
//                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
//                if (isStart) {
//                    startDate = formatted
//                    showDatePicker(false)
//                } else {
//                    endDate = formatted
//                    viewModel.onDateSelected(startDate, endDate)
//                }
//            },
//            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
//        )
//
//        if (isStart) {
//            datePicker.datePicker.minDate = System.currentTimeMillis()
//        } else {
//            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//            val minDate = sdf.parse(startDate)?.time ?: System.currentTimeMillis()
//            datePicker.datePicker.minDate = minDate
//        }
//        datePicker.show()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}