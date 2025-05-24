package com.cookandroid.challengers

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookandroid.challengers.databinding.FragmentHomeAichatBinding
import com.cookandroid.challengers.viewmodel.AichatState
import com.cookandroid.challengers.viewmodel.AichatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// https://colab.research.google.com/drive/1Mv4WCf4Jto-wlpLK_m3xSF9npbMiu-yv?usp=sharing

class HomeAichatFragment : Fragment() {

    private var _binding: FragmentHomeAichatBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private val viewModel: AichatViewModel by activityViewModels()

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
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
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
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                binding.etMessage.text = null
                val time = getCurrentTime()
                addMessage(ChatMessage.FromUser(message, time))
                handleUserInput(message)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                val time = getCurrentTime()
                when (state) {
                    is AichatState.Welcome -> {
                        addMessage(ChatMessage.FromBot("안녕하세요. ${viewModel.getUserName()}님!\nAI Buddy와 함께 운동 루틴을 계획하시겠어요?", time))
                    }
                    is AichatState.AskDate -> {
                        addMessage(ChatMessage.FromBot("운동 시작일과 마감일을 지정해 주세요!", time))
                        showDatePicker(isStart = true)
                    }
                    is AichatState.AskDays -> {
                        addMessage(ChatMessage.FromBot("어떤 요일에 운동하실 건가요?\n예: 월 수 금", time))
                    }
                    is AichatState.AskFocusArea -> {
                        addMessage(ChatMessage.FromBot("특별히 강화하고 싶은 부위가 있나요?", time))
                    }
                    is AichatState.Generating -> {
                        addMessage(ChatMessage.FromBot("${viewModel.getUserName()}님의 루틴을 생성하고 있어요. 잠시만 기다려 주세요...", time))
                    }
                    is AichatState.ShowResult -> {
                        addMessage(ChatMessage.FromBot("${viewModel.getUserName()}님을 위한 운동 스케줄이 준비되었어요!", time))
                        addMessage(ChatMessage.FromBot(state.planText, time))
                        addMessage(ChatMessage.FromBot("이대로 할게요 / 다시 추천해 주세요", time))
                    }
                    is AichatState.Done -> {
                        addMessage(ChatMessage.FromBot("좋아요. 생성된 루틴으로 즐거운 운동을 시작해 보세요!", time))
                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
                    }
                    is AichatState.Rejected -> {
                        addMessage(ChatMessage.FromBot("추천한 루틴이 마음에 들지 않으셨나요?\n직접 추가해 보세요.", time))
                        findNavController().navigate(R.id.action_homeAichatFragment_to_homeFragment)
                    }
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun handleUserInput(input: String) {
        when (val state = viewModel.state.value) {
            is AichatState.Welcome -> {
                if (input.contains("네", ignoreCase = true)) viewModel.onUserConfirmedStart()
                else viewModel.onRejectPlan()
            }

            is AichatState.AskDays -> {
                val days = input.split(" ", ",").mapNotNull {
                    it.trim().takeIf { it.isNotEmpty() }
                }
                viewModel.onDaysSelected(days)
            }

            is AichatState.AskFocusArea -> {
                viewModel.onFocusAreaEntered(input)
            }

            is AichatState.ShowResult -> {
                when {
                    input.contains("이대로", ignoreCase = true) -> viewModel.onAcceptPlan()
                    input.contains("다시", ignoreCase = true) -> viewModel.onFocusAreaEntered(viewModel.getFocusArea())
                    else -> addMessage(ChatMessage.FromBot("‘이대로 할게요’ 또는 ‘다시 추천해 주세요’ 중 하나를 입력해 주시면 도와드릴게요", getCurrentTime()))
                }
            }
            else -> Unit
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
    }

    private fun showDatePicker(isStart: Boolean) {
        val cal = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                if (isStart) {
                    startDate = formatted
                    showDatePicker(false)
                } else {
                    endDate = formatted
                    viewModel.onDateSelected(startDate, endDate)
                }
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )

        if (isStart) {
            datePicker.datePicker.minDate = System.currentTimeMillis()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val minDate = sdf.parse(startDate)?.time ?: System.currentTimeMillis()
            datePicker.datePicker.minDate = minDate
        }
        datePicker.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}