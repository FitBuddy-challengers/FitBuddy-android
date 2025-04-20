package com.cookandroid.challengers.auth.signup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentSignupPwBinding
import com.cookandroid.challengers.R

class SignupPwFragment : Fragment() {

    private var _binding: FragmentSignupPwBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passwordEditText = binding.tilLoginPassword.editText
        val confirmPasswordEditText = binding.tilLoginPassword2.editText

        // pw TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 유효성 검사
                validatePasswords()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        passwordEditText?.addTextChangedListener(textWatcher)
        confirmPasswordEditText?.addTextChangedListener(textWatcher)

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_signUpPassword_to_signUpOtp)
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validatePasswords() {
        val password = binding.tilLoginPassword.editText?.text.toString()
        val confirmPassword = binding.tilLoginPassword2.editText?.text.toString()

        val isValid = isValidPassword(password) && password == confirmPassword

        // 버튼 활성화/비활성화
        binding.btnNext.apply {
            isEnabled = isValid
            setBackgroundResource(
                if (isValid) R.drawable.btn_next_blue
                else R.drawable.btn_next_gray
            )
        }
    }

    // 비밀번호 유효성 체크
    private fun isValidPassword(password: String): Boolean {
        val regex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}\$")
        return regex.matches(password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}