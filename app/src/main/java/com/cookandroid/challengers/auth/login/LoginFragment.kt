package com.cookandroid.challengers.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.MainActivity
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLoginBinding.inflate(inflater, container, false)

        // 카카오 로그인 - 추후 구현
        binding.btnLoginKakao.setOnClickListener {
            navigateToMainActivity()
        }

        // 네이버 로그인 - 추후 구현
        binding.btnLoginNaver.setOnClickListener {
            navigateToMainActivity()
        }

        // 이메일 로그인 버튼 클릭 시
        binding.btnLoginEmail.setOnClickListener {
            // LoginEmailFragment 이동
            findNavController().navigate(R.id.action_login_to_email)
        }

        return binding.root
    }

    private fun navigateToMainActivity() {
        // MainActivity 이동
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}