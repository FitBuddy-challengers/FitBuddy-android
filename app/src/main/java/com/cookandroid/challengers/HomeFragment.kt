package com.cookandroid.challengers


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dateAdapter: DateAdapter
    private lateinit var workoutAdapter: WorkoutAdapter

//    private lateinit var planDao: ExercisePlanDao
//    private lateinit var planDetailDao: PlanDetailDao

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