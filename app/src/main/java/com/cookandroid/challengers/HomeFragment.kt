package com.cookandroid.challengers


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentHomeBinding
import com.cookandroid.challengers.util.UserPreference
import com.cookandroid.challengers.api.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Response
import okhttp3.ResponseBody
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookandroid.challengers.data.StoreItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dateAdapter: DateAdapter
    private lateinit var workoutAdapter: WorkoutAdapter

    private val weekDates = mutableListOf<WeekDate>()
    private var selectedPosition = 0
    private var planId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = UserPreference(requireContext()).getUserId()
        Log.d("출석", "👉 현재 userId = $userId")

        //  출석 인증 로직
        if (userId != -1 && !AttendanceUtil.hasCheckedAttendanceToday(requireContext())) {
            Log.d("출석", "🟡 출석 미기록 상태, markAttendance 실행")
            markAttendance(userId)
        } else {
            Log.d("출석", "🔵 이미 출석 기록됨 또는 userId 무효")
        }

        //  날짜 로직 수정 시작 ✨
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val displayFormatter = DateTimeFormatter.ofPattern("d")

        val formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
        binding.tvDay.text = formattedDate

        //  정확하게 이번 주 '일요일'부터 시작 (한국식)
        val dayOfWeek = today.dayOfWeek.value // 월=1, ... 일=7
        val daysFromSunday = if (dayOfWeek == 7) 0 else dayOfWeek
        val startOfWeek = today.minusDays(daysFromSunday.toLong())

        //  날짜 리스트 구성
        weekDates.clear()
        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            weekDates.add(
                WeekDate(
                    date = date.format(displayFormatter),     // 예: "6"
                    fullDate = date.format(formatter),        // 예: "2025-06-06"
                    isToday = date == today                   //  오늘 강조됨
                )
            )
        }
        Log.d("HomeFragment", "📆 주간 날짜 수: ${weekDates.size}")

        //  리사이클러뷰에 레이아웃 매니저 반드시 설정!
        binding.rvDate.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        //  어댑터 연결 및 날짜 클릭 이벤트 처리
        dateAdapter = DateAdapter(weekDates) { selected ->
            selectedPosition = selected

            val selectedDate = LocalDate.parse(weekDates[selected].fullDate, formatter)
            val displayText = "${selectedDate.year}년 ${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일"
            binding.tvDay.text = displayText
        }
        binding.rvDate.adapter = dateAdapter

        loadTodayWorkoutPlan()

        binding.btnStartWorkout.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_exerciseFragment)
        }

        binding.btnAiChat.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_homeAichatFragment)
        }

        binding.btnNoti.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_mypageNotiSetFragment)
        }

        binding.btnMypage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_homeMypageFragment)
        }
    }

    private fun markAttendance(userId: Int) {
        RetrofitClient.challengeApi.markAttendance(userId)
            .enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("출석 처리", "✅ 오늘 출석 성공!")
                        AttendanceUtil.markAttendanceToday(requireContext())
                        parentFragmentManager.setFragmentResult("attendance_done", Bundle())
                    } else {
                        Log.w("출석 실패", "⚠️ 서버 응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                    Log.e("출석 실패", "❌ 네트워크 오류: ${t.message}", t)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        applyCharacterFromPreference()
    }

    private fun loadTodayWorkoutPlan() {
        val userId = UserPreference(requireContext()).getUserId()
        if (userId == -1) {
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId)
                if (response.isSuccessful) {
                    val data = response.body()
                    planId = data?.plan?.id?.toLong() ?: -1L
                    val scheduleList = data?.schedules ?: emptyList()

                    Log.d("HomeFragment", "📦 planId: $planId, 스케줄 수: ${scheduleList.size}")

                    val workoutItems = mutableListOf<WorkoutUiModel>()

                    for (schedule in scheduleList) {
                        try {
                            val call = RetrofitClient.scheduleApi.getRepsSets(schedule.schedule_id.toLong())
                            val repsResponse = call.execute() // execute()는 IO 스레드 안에서 호출 중이므로 OK

                            if (repsResponse.isSuccessful) {
                                val repsSets = repsResponse.body() ?: emptyList()
                                if (repsSets.isNotEmpty()) {
                                    val reps = repsSets.first().reps
                                    val sets = repsSets.size
                                    val name = schedule.exercise_name
                                    val isCompleted = repsSets.all { it.isCompleted }

                                    workoutItems.add(
                                        WorkoutUiModel(
                                            scheduleId = schedule.schedule_id.toLong(),
                                            name = name,
                                            reps = reps,
                                            sets = sets,
                                            isCompleted = isCompleted
                                        )
                                    )
                                }
                            } else {
                                Log.w("HomeFragment", "❗ reps 불러오기 실패: scheduleId=${schedule.schedule_id}")
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "🔥 reps 요청 중 예외 발생: scheduleId=${schedule.schedule_id}", e)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        // ✅ 운동 리스트 반영
                        workoutAdapter = WorkoutAdapter { item, isChecked ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val request = RetrofitClient.SetCompletionRequest(
                                        scheduleId = item.scheduleId,
                                        setNumber = 1,
                                        isCompleted = isChecked
                                    )

                                    val updateResponse = RetrofitClient.scheduleApi
                                        .updateRepsSetCompletion(request)
                                        .execute()

                                    if (updateResponse.isSuccessful) {
                                        Log.d("HomeFragment", "✅ 체크 상태 서버 반영 완료: ${item.name} → $isChecked")

                                        val currentList = workoutAdapter.currentList.toMutableList()
                                        val index = currentList.indexOfFirst { it.scheduleId == item.scheduleId }
                                        if (index != -1) {
                                            currentList[index] = item.copy(isCompleted = isChecked)
                                            withContext(Dispatchers.Main) {
                                                workoutAdapter.submitList(currentList)

                                                // ✅ 체크 변경 후에도 게이지 반영
                                                updateProgressGauge(currentList)
                                            }
                                        }
                                    } else {
                                        Log.w("HomeFragment", "❗ 체크 서버 반영 실패: ${updateResponse.code()}")
                                    }

                                } catch (e: Exception) {
                                    Log.e("HomeFragment", "❌ 체크 상태 반영 중 오류", e)
                                }
                            }
                        }

                        binding.rvWorkout.adapter = workoutAdapter
                        workoutAdapter.submitList(workoutItems)

                        // ✅ 운동 달성률에 따른 게이지 설정
                        updateProgressGauge(workoutItems)
                    }

                } else {
                    Log.e("HomeFragment", "❌ 계획 불러오기 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "🚨 네트워크 오류", e)
            }
        }
    }

    private fun updateProgressGauge(items: List<WorkoutUiModel>) {
        val total = items.size
        val completed = items.count { it.isCompleted }

        val progressPercent = if (total > 0) (completed * 100) / total else 0

        Log.d("HomeFragment", "📊 운동 진행률: $completed/$total ($progressPercent%)")

        binding.uiGauge.progress = progressPercent
    }

    private fun applyCharacterFromPreference() {
        if (!isAdded || _binding == null) return

        val equippedMapIds = UserPreference(requireContext()).getEquippedItemIds()

        // ID 맵을 ProductItem 맵으로 변환
        val equippedItems = mutableMapOf<String, ProductItem?>()
        equippedMapIds.forEach { (category, itemId) ->
            equippedItems[category] = itemId?.let { StoreItemData.findItemById(it) }
        }

        // 기본 캐릭터 이미지 설정
        val baseCharacterItem = equippedItems["character"]
        binding.baseCharacterImageView.setImageResource(
            baseCharacterItem?.imageResId ?: R.drawable.char_graycat
        )

        // 토끼 캐릭터 Y 오프셋 조정
        if (baseCharacterItem?.id == 2) {
            binding.baseCharacterImageView.translationY = -44f
        } else {
            binding.baseCharacterImageView.translationY = 0f
        }

        // 아이템 가져오기
        val costumeItem = equippedItems["costume"]
        val onepieceItem = equippedItems["onepiece"]
        val topItem = equippedItems["top"]
        val pantsItem = equippedItems["pants"]
        val accItem = equippedItems["acc"]
        val hairAccItem = equippedItems["hairAcc"]
        val glassesItem = equippedItems["glasses"]

        // 2. 코스튬 처리
        if (costumeItem != null) {
            binding.costumeItemImageView.setImageResource(costumeItem.imageResId)
            binding.costumeItemImageView.visibility = View.VISIBLE
            binding.topItemImageView.visibility = View.GONE
            binding.pantsItemImageView.visibility = View.GONE
            binding.onepieceItemImageView.visibility = View.GONE
        } else {
            binding.costumeItemImageView.visibility = View.GONE
            // 코스튬 미착용 시 원피스 vs 상/하의 로직 실행
            if (onepieceItem != null) {
                binding.onepieceItemImageView.setImageResource(onepieceItem.imageResId)
                binding.onepieceItemImageView.visibility = View.VISIBLE
                binding.topItemImageView.visibility = View.GONE
                binding.pantsItemImageView.visibility = View.GONE
            } else {
                binding.onepieceItemImageView.visibility = View.GONE
                binding.topItemImageView.visibility = if (topItem != null) {
                    binding.topItemImageView.setImageResource(topItem.imageResId)
                    View.VISIBLE
                } else View.GONE
                binding.pantsItemImageView.visibility = if (pantsItem != null) {
                    binding.pantsItemImageView.setImageResource(pantsItem.imageResId)
                    View.VISIBLE
                } else View.GONE
            }
        }

        // 3. 일반 액세서리 처리 (acc)
        binding.accItemImageView.visibility = if (accItem != null) {
            binding.accItemImageView.setImageResource(accItem.imageResId)
            View.VISIBLE
        } else View.GONE

        // 4. 헤어 액세서리
        binding.hairAccItemImageView.visibility = if (hairAccItem != null) {
            binding.hairAccItemImageView.setImageResource(hairAccItem.imageResId)
            View.VISIBLE
        } else View.GONE

        // 5. 안경
        binding.glassesItemImageView.visibility = if (glassesItem != null) {
            binding.glassesItemImageView.setImageResource(glassesItem.imageResId)
            View.VISIBLE
        } else View.GONE
    }

}

// ✅ 하루에 한 번 출석 여부를 저장하고 확인하는 유틸
object AttendanceUtil {

    private const val PREF_NAME = "AttendancePrefs"
    private const val KEY_LAST_ATTENDANCE_DATE = "last_attendance_date"

    fun hasCheckedAttendanceToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_ATTENDANCE_DATE, null)
        val today = getTodayDate()
        Log.d("출석", "📌 hasCheckedAttendanceToday: lastDate=$lastDate, today=$today")
        return lastDate == today
    }

    fun markAttendanceToday(context: Context) {
        val today = getTodayDate()
        Log.d("출석", "✅ markAttendanceToday: 저장됨 → $today")
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_ATTENDANCE_DATE, today).apply()
    }

    private fun getTodayDate(): String {
        return LocalDate.now().toString() // 예: "2025-06-03"
    }
}