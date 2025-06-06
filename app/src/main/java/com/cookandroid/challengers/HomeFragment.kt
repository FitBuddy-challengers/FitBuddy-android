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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dateAdapter: DateAdapter
    private lateinit var workoutAdapter: WorkoutAdapter

    private lateinit var planDao: ExercisePlanDao
    private lateinit var planDetailDao: PlanDetailDao

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
        Log.d("출석", "👉 현재 userId = $userId") // ✅ 디버깅용 로그

        // 출석 인증은 하루에 1번!!
        if (userId != -1 && !AttendanceUtil.hasCheckedAttendanceToday(requireContext())) {
            Log.d("출석", "🟡 출석 미기록 상태, markAttendance 실행")
            markAttendance(userId)
        } else {
            //Log.d("출석", "🔵 이미 출석 기록됨 또는 userId 무효")
        }
    }

    private fun markAttendance(userId: Int) {
        lifecycleScope.launch {
            try {
                val response: Response<ResponseBody> = RetrofitClient.challengeApi.markAttendance(userId)
                if (response.isSuccessful) {
                    Log.d("출석 처리", "✅ 오늘 출석 성공!")
                    AttendanceUtil.markAttendanceToday(requireContext()) // ✅ 출석 기록
                    parentFragmentManager.setFragmentResult("attendance_done", Bundle())
                } else {
                    Log.w("출석 실패", "⚠️ 서버 응답 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("출석 실패", "❌ 네트워크 오류: ${e.message}")
            }
        }
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