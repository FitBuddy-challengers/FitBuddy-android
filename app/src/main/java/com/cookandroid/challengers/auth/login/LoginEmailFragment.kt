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
import com.cookandroid.challengers.HomeFragment

import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding

class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    // ⭐ 개발용 마스터 계정 정보
    private val masterEmail = "master"
    private val masterPassword = "12341234"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔙 뒤로 가기 버튼 클릭
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 📧 이메일 로그인 버튼 클릭
        binding.btnEmailSignin.setOnClickListener {
            val email = binding.tilLoginAddress.editText?.text.toString()
            val password = binding.tilLoginPassword.editText?.text.toString()

            // 입력값 검증
            if (validateInput(email, password)) {
                if (isMasterAccount(email, password)) {
                    // ✅ 개발용 마스터 계정 로그인 성공
                    Toast.makeText(requireContext(), "개발자 모드 로그인 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity() // ⭐ MainActivity로 이동해서 하단바 정상 세팅
                } else if (isValidAccount(email, password)) {
                    // ✅ 임시 계정 로그인 성공
                    Toast.makeText(requireContext(), "로그인 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity() // ⭐ MainActivity로 이동해서 하단바 정상 세팅
                } else {
                    // ❌ 유효하지 않은 계정
                    Toast.makeText(requireContext(), "유효하지 않은 계정이에요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🆕 이메일 회원가입 버튼 클릭
        binding.btnEmailSignup.setOnClickListener {
            // 회원가입 화면(SignUpEmailFragment)으로 이동
            findNavController().navigate(R.id.action_loginEmail_to_signUpEmail)
        }
    }

    // ✏️ 이메일과 비밀번호 입력 검증 함수
    private fun validateInput(email: String, password: String): Boolean {
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

    // ✏️ 개발용 마스터 계정 체크 함수
    private fun isMasterAccount(email: String, password: String): Boolean {
        return email == masterEmail && password == masterPassword
    }

    // ✏️ 임시 계정 유효성 검사 함수 (나중에 서버 연동할 예정)
    private fun isValidAccount(email: String, password: String): Boolean {
        return email == "test@example.com" && password == "password123"
    }

    // ⭐ MainActivity로 이동 (하단바 정상 작동)
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("isMasterLogin", true) // ⭐ 마스터 로그인 플래그 추가
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
