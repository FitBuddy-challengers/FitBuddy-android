package com.cookandroid.challengers.auth.signup

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.SignUpRequest
import com.cookandroid.challengers.api.SignUpResponse
import com.cookandroid.challengers.api.SignUpService
import com.cookandroid.challengers.databinding.FragmentSignupPwBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpPasswordFragment : Fragment() {

    private var _binding: FragmentSignupPwBinding? = null
    private val binding get() = _binding!!

    private val signUpService = RetrofitClient.retrofit.create(SignUpService::class.java)

    private lateinit var email: String // SignUpEmailFragment에서 넘겨받은 이메일

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SignUpEmailFragment에서 전달받은 이메일 꺼내오기
        email = arguments?.getString("email") ?: ""

        // "다음" 버튼 초기 설정: 비활성화 상태, 회색 배경
        binding.btnNext.isEnabled = false
        binding.btnNext.setBackgroundResource(R.drawable.btn_inactive_background)

        // 비밀번호, 비밀번호 확인 입력 감지하는 TextWatcher 등록
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNextButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.tilLoginPassword.editText?.addTextChangedListener(textWatcher)
        binding.tilLoginPassword2.editText?.addTextChangedListener(textWatcher)

        // "다음" 버튼 클릭 이벤트
        binding.btnNext.setOnClickListener {
            val password = binding.tilLoginPassword.editText?.text.toString()

            // 서버에 회원가입 요청 보내기
            signUp(email, password)
        }
    }

    // 비밀번호 조건에 따라 다음 버튼 활성화/비활성화하는 함수
    private fun updateNextButtonState() {
        val password = binding.tilLoginPassword.editText?.text.toString()
        val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

        if (password.length >= 8 && password == confirmPassword) {
            binding.btnNext.isEnabled = true
            binding.btnNext.setBackgroundResource(R.drawable.btn_next_blue) // 파란색 버튼 배경으로
        } else {
            binding.btnNext.isEnabled = false
            binding.btnNext.setBackgroundResource(R.drawable.btn_next_gray) // 회색 버튼 배경으로
        }
    }

    // 서버에 회원가입 요청 보내기
    private fun signUp(email: String, password: String) {
        val request = SignUpRequest(email = email, password = password)

        signUpService.signup(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SignUp", "회원가입 성공: ${body?.message}")

                    // ✨ 이메일을 Bundle로 넘기기
                    val bundle = Bundle()
                    bundle.putString("email", email)

                    findNavController().navigate(R.id.action_signUpPassword_to_signUpOtp, bundle)

                } else {
                    Log.d("SignUp", "회원가입 실패: 코드 ${response.code()}, 에러 메시지 ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "회원가입 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                Log.e("SignUp", "서버 연결 실패: ${t.message}")
                Toast.makeText(requireContext(), "서버 연결 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}