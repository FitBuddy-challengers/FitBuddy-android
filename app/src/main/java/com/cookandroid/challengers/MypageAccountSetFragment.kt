package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentMypageAccountSetBinding
import com.cookandroid.challengers.model.ProfileData
import com.cookandroid.challengers.util.UserPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import androidx.navigation.fragment.findNavController

class MypageAccountSetFragment : Fragment() {

    private var _binding: FragmentMypageAccountSetBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageAccountSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreference = UserPreference(requireContext())
        val email = userPreference.getUserEmail()

        // 이메일 불러오기
        RetrofitClient.profileApiService.getProfile(email)
            .enqueue(object : Callback<ProfileData> {
                override fun onResponse(call: Call<ProfileData>, response: Response<ProfileData>) {
                    if (response.isSuccessful) {
                        response.body()?.let { profile ->
                            binding.userEmail.text = profile.email
                        }
                    } else {
                        Toast.makeText(requireContext(), "이메일 불러오기 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileData>, t: Throwable) {
                    Log.e("MypageAccountSet", "프로필 요청 실패", t)
                    Toast.makeText(requireContext(), "통신 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // 뒤로 가기
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}