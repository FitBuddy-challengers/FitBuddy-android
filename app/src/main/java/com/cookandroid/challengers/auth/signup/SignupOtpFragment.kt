package com.cookandroid.challengers.auth.signup

import android.os.Bundle
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.auth.signup.SignUpDoneFragment
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

    private lateinit var email: String // 이전 프래그먼트(SignUpPasswordFragment)에서 넘어온 이메일

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignupOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 서버에 이메일로 OTP 발송 요청 보내는 함수
    private fun sendOtpEmail(email: String) {
        val request = mapOf(
            "email" to email
        )

        signUpService.sendOtp(request).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("OTP", "OTP 이메일 전송 성공")
                    Toast.makeText(requireContext(), "인증번호가 이메일로 발송되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("OTP", "OTP 이메일 전송 실패: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "OTP 전송 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("OTP", "서버 연결 실패: ${t.message}")
                Toast.makeText(requireContext(), "서버 연결 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✨ 이메일을 arguments로부터 가져오기
        email = arguments?.getString("email") ?: ""

        // ✨ 화면 열리자마자 서버에 OTP 이메일 발송 요청
        sendOtpEmail(email)

        // ✨ (추가) OTP EditText 입력 감지
        val otpWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNextButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        // ✨ 6개 EditText에 TextWatcher 등록
        binding.otp1.addTextChangedListener(otpWatcher)
        binding.otp2.addTextChangedListener(otpWatcher)
        binding.otp3.addTextChangedListener(otpWatcher)
        binding.otp4.addTextChangedListener(otpWatcher)
        binding.otp5.addTextChangedListener(otpWatcher)
        binding.otp6.addTextChangedListener(otpWatcher)


        // 다음 버튼 클릭
        binding.btnNext.setOnClickListener {
            val otp = getEnteredOtp()

            if (otp.length != 6) {
                Toast.makeText(requireContext(), "6자리 인증번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(email, otp)
        }

        // 뒤로 가기 버튼 클릭 시
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ✅ (추가) 버튼 상태 업데이트 함수
    private fun updateNextButtonState() {
        val otp = getEnteredOtp()
        val isComplete = otp.length == 6

        binding.btnNext.isEnabled = isComplete
        val backgroundRes = if (isComplete) R.drawable.btn_next_blue else R.drawable.btn_next_gray
        binding.btnNext.setBackgroundResource(backgroundRes)
    }

    // EditText 6개에서 입력한 OTP 가져오기
    private fun getEnteredOtp(): String {
        return (binding.otp1.text.toString() +
                binding.otp2.text.toString() +
                binding.otp3.text.toString() +
                binding.otp4.text.toString() +
                binding.otp5.text.toString() +
                binding.otp6.text.toString()).trim()
    }

    // 서버로 인증 요청
    // 서버로 인증 요청
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

                    // ✅ SharedPreferences에 이메일 저장
                    val prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
                    prefs.edit().putString("email", email).apply()

                    // 회원가입 완료 화면으로 이동
                    findNavController().navigate(R.id.action_signUpOtp_to_signUpDone)
                } else {
                    Log.d("OTP", "인증 실패: ${response.errorBody()?.string()}")
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