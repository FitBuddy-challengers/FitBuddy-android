package com.cookandroid.challengers.auth.profile


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cookandroid.challengers.MainActivity
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentProfileDoneBinding
import com.cookandroid.challengers.model.ProfileData
import com.cookandroid.challengers.viewmodel.ProfileViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileDoneFragment : Fragment() {

    private var _binding: FragmentProfileDoneBinding? = null
    private val binding get() = _binding!!

    // ✅ ViewModel 가져오기
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileDoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener { ///
            // ✅ ProfileData 구성
            val profileData = ProfileData(
                email = viewModel.email,
                name = viewModel.name,
                age_group = viewModel.ageGroup,
                gender = viewModel.gender,
                height = viewModel.height,
                weight = viewModel.weight,
                diseases = viewModel.diseases,
                workout_level = viewModel.workoutLevel,
                preferred_workouts = viewModel.preferredWorkouts,
                equipment = viewModel.equipment
            )

            // ✅ 서버 전송
            RetrofitClient.profileApiService.updateProfile(profileData)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            // ✅ 로그인 상태 저장
                            val prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("isLoggedIn", true).apply()

                            // 홈 화면으로 이동
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(requireContext(), "서버 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(requireContext(), "통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
