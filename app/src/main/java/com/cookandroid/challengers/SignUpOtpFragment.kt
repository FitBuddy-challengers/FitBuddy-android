package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.SignUpService
import com.cookandroid.challengers.databinding.FragmentSignupOtpBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpOtpFragment : Fragment() {

    private var _binding: FragmentSignupOtpBinding? = null
    private val binding get() = _binding!!

    private val signUpService = RetrofitClient.retrofit.create(SignUpService::class.java)

    private lateinit var email: String // 이전 프래그먼트(SignUpPasswordFragment)에서 넘겨받을 이메일

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignupOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✨ 이메일을 arguments로부터 받아오기
        email = arguments?.getString("email") ?: ""

        binding.btnNext.setOnClickListener {
            val enteredOtp = getEnteredOtp()

            if (enteredOtp.length != 6) {
                Toast.makeText(requireContext(), "6자리 인증번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(email, enteredOtp)
        }
    }

    private fun getEnteredOtp(): String {
        val otp1 = binding.otp1.text.toString()
        val otp2 = binding.otp2.text.toString()
        val otp3 = binding.otp3.text.toString()
        val otp4 = binding.otp4.text.toString()
        val otp5 = binding.otp5.text.toString()
        val otp6 = binding.otp6.text.toString()

        return otp1 + otp2 + otp3 + otp4 + otp5 + otp6
    }

    private fun verifyOtp(email: String, otp: String) {
        val request = mapOf(
            "email" to email,
            "otp" to otp
        )

        signUpService.verifyOtp(request).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(
                call: Call<Map<String, String>>,
                response: Response<Map<String, String>>
            ) {
                if (response.isSuccessful) {
                    Log.d("OTP", "인증 성공")
                    Toast.makeText(requireContext(), "인증에 성공했습니다!", Toast.LENGTH_SHORT).show()

                    // TODO: 인증 성공 후 다음 화면으로 이동 (예: 회원가입 완료 화면)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, SignUpDoneFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Log.d("OTP", "인증 실패")
                    Toast.makeText(requireContext(), "인증번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("OTP", "서버 연결 실패: ${t.message}")
                Toast.makeText(requireContext(), "서버 연결 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}