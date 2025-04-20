package com.cookandroid.challengers


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.SignUpRequest
import com.cookandroid.challengers.api.SignUpService
import com.cookandroid.challengers.api.SignUpResponse
import com.cookandroid.challengers.databinding.FragmentSignupPwBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpPasswordFragment : Fragment() {

    private var _binding: FragmentSignupPwBinding? = null
    private val binding get() = _binding!!

    private val signUpService = RetrofitClient.retrofit.create(SignUpService::class.java)

    private lateinit var email: String // SignUpEmailFragment에서 넘겨받을 이메일
    // (지금은 임시로 처리하고, 나중에 진짜 넘겨줄거야!)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 여기서 이메일 꺼내오기
        email = arguments?.getString("email") ?: ""

                // "다음" 버튼 클릭
        binding.btnNext.setOnClickListener {
            val password = binding.tilLoginPassword.editText?.text.toString()
            val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

            if (password.length < 8) {
                Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signUp(email, password)
        }
    }

    private fun signUp(email: String, password: String) {
        val request = SignUpRequest(
            email = email,
            password = password
        )

        signUpService.signup(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SignUp", "회원가입 성공: ${body?.message}")

                    // ✅ 서버 통신 성공하면 바로 OTP 인증 화면으로 이동!
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, SignUpOtpFragment())
                        .addToBackStack(null)
                        .commit()

                } else {
                    Log.d("SignUp", "회원가입 실패: ${response.errorBody()?.string()}")
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