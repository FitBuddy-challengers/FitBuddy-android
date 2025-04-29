package com.cookandroid.challengers.auth.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.text.TextWatcher
import android.text.Editable
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

    private lateinit var email: String
    private val signUpService = RetrofitClient.retrofit.create(SignUpService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = arguments?.getString("email") ?: ""

        // ✅ (추가) 비밀번호 입력할 때 버튼 색 변경 감지
        binding.tilLoginPassword.editText?.addTextChangedListener(passwordWatcher)
        binding.tilLoginPassword2.editText?.addTextChangedListener(passwordWatcher)

        binding.btnNext.setOnClickListener {
            val password = binding.tilLoginPassword.editText?.text.toString()
            val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

            if (password.length < 8) {
                Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 해요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않아요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 서버에 회원가입 정보 전송
            val request = SignUpRequest(email, password)
            signUpService.signup(request).enqueue(object : Callback<SignUpResponse> {
                override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "회원가입 정보 저장 완료", Toast.LENGTH_SHORT).show()

                        val bundle = Bundle().apply {
                            putString("email", email)
                        }

                        findNavController().navigate(R.id.action_signUpPassword_to_signUpOtp, bundle)
                    } else {
                        Toast.makeText(requireContext(), "회원가입 저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ✅ (추가) 비밀번호 입력할 때마다 버튼 상태를 업데이트하는 TextWatcher
    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val password = binding.tilLoginPassword.editText?.text.toString()
            val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

            val isValid = password.length >= 8 && password == confirmPassword

            binding.btnNext.isEnabled = isValid
            val backgroundRes = if (isValid) R.drawable.btn_next_blue else R.drawable.btn_next_gray
            binding.btnNext.setBackgroundResource(backgroundRes)
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



/*
class SignUpPasswordFragment : Fragment() {

    private var _binding: FragmentSignupPwBinding? = null
    private val binding get() = _binding!!

    private lateinit var email: String
    private val signUpService = RetrofitClient.retrofit.create(SignUpService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = arguments?.getString("email") ?: ""


        binding.btnNext.setOnClickListener {
            val password = binding.tilLoginPassword.editText?.text.toString()
            val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

            if (password.length < 8) {
                Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 해요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않아요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 서버에 회원가입 정보 전송 (임시 저장)
            val request = SignUpRequest(email, password)
            signUpService.signup(request).enqueue(object : Callback<SignUpResponse> {
                override fun onResponse(
                    call: Call<SignUpResponse>,
                    response: Response<SignUpResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "회원가입 정보 저장 완료", Toast.LENGTH_SHORT).show()

                        // ✅ 회원가입 성공 후 OTP 화면으로 이동
                        val bundle = Bundle().apply {
                            putString("email", email)
                        }

                        findNavController().navigate(R.id.action_signUpPassword_to_signUpOtp, bundle)
                    } else {
                        Toast.makeText(requireContext(), "회원가입 저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}*/