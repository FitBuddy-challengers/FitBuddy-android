package com.cookandroid.challengers.auth.login


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 카카오 로그인 버튼 클릭 (추후 구현)
        binding.btnLoginKakao.setOnClickListener {
            // TODO: 카카오 로그인 로직 추가
        }

        // 네이버 로그인 버튼 클릭 (추후 구현)
        binding.btnLoginNaver.setOnClickListener {
            // TODO: 네이버 로그인 로직 추가
        }

        // 이메일 로그인 버튼 클릭 -> LoginEmailFragment로 이동
        binding.btnLoginEmail.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_loginEmail)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}