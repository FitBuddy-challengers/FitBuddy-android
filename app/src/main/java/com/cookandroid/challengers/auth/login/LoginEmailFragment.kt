package com.cookandroid.challengers.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.MainActivity
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding

class LoginEmailFragment : Fragment() {

    private lateinit var binding: FragmentLoginEmailBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLoginEmailBinding.inflate(inflater, container, false)

        // 뒤로 가기
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 이메일 로그인
        binding.btnEmailSignin.setOnClickListener {
            val email = binding.tilLoginAddress.editText?.text.toString()
            val password = binding.tilLoginPassword.editText?.text.toString()

            // 입력값 검증
            if (validateInput(email, password)) {
                // 로그인 처리
                if (isValidAccount(email, password)) {
                    // MainActivity 이동
                    navigateToMainActivity()
                } else {
                    // 유효하지 않은 계정
                    Toast.makeText(requireContext(), "유효하지 않은 계정이에요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 이메일 회원가입
        binding.btnEmailSignup.setOnClickListener {
            // SignupEmailFragment 이동
            findNavController().navigate(R.id.action_email_to_signupEmail)
        }

        return binding.root
    }

    private fun validateInput(email: String, password: String): Boolean {
        // email/pw 입력 확인
        if (email.isBlank()) {
            Toast.makeText(requireContext(), "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 해요.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun isValidAccount(email: String, password: String): Boolean {
        // email/pw 유효 확인
        return email == "test@example.com" && password == "password123"
    }

    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}