package com.cookandroid.challengers.auth.signup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentSignupEmailBinding

class SignupEmailFragment : Fragment(R.layout.fragment_signup_email) {

    private lateinit var binding: FragmentSignupEmailBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSignupEmailBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener {
            requireActivity()
        }

        // btnNext 색상 - TextWatcher
        binding.tilLoginAddress.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val email = charSequence.toString().trim()  // 텍스트 가공
                if (isValidEmail(email)) {
                    binding.btnNext.setBackgroundResource(R.drawable.btn_next_blue)
                    binding.btnNext.isEnabled = true  // blue - 활성화
                } else {
                    binding.btnNext.setBackgroundResource(R.drawable.btn_next_gray)
                    binding.btnNext.isEnabled = false  // gray - 비활성화
                }
            }

            override fun afterTextChanged(editable: Editable?) {
            }
        })

        // 다음 버튼 클릭 시
        binding.btnNext.setOnClickListener {
            val email = binding.tilLoginAddress.editText?.text.toString()

            // SignupPwFragment 이동
            if (isValidEmail(email)) {
                findNavController().navigate(R.id.action_signupEmail_to_pw)
            }
        }

        return binding.root
    }

    // 이메일 유효성 검사
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}