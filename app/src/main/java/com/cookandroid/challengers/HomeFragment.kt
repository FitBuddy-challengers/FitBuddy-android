package com.cookandroid.challengers


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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = UserPreference(requireContext()).getUserId()
        if (userId != -1) {
            markAttendance(userId)
        }
    }

    private fun markAttendance(userId: Int) {
        lifecycleScope.launch {
            try {
                val response: Response<ResponseBody> = RetrofitClient.challengeApi.markAttendance(userId)
                if (response.isSuccessful) {
                    Log.d("출석 처리", "✅ 오늘 출석 성공!")
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