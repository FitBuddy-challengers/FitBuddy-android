package com.cookandroid.challengers.auth.signup

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentSignupEmailBinding

class SignUpEmailFragment : Fragment() {

    private var _binding: FragmentSignupEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 처음에는 버튼 비활성화
        binding.btnNext.isEnabled = false
        binding.btnNext.setBackgroundResource(R.drawable.btn_inactive_background)

        binding.tilLoginAddress.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val emailInput = s.toString()

                if (isValidEmail(emailInput)) {
                    binding.btnNext.isEnabled = true
                    binding.btnNext.setBackgroundResource(R.drawable.btn_next_blue) // 파란색 버튼 배경으로
                } else {
                    binding.btnNext.isEnabled = false
                    binding.btnNext.setBackgroundResource(R.drawable.btn_next_gray) // 회색 버튼 배경으로
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnNext.setOnClickListener {
            val emailInput = binding.tilLoginAddress.editText?.text.toString()

            if (isValidEmail(emailInput)) {
                val bundle = Bundle()
                bundle.putString("email", emailInput)

                findNavController().navigate(
                    R.id.action_signUpEmail_to_signUpPassword,
                    bundle
                )
            } else {
                Toast.makeText(requireContext(), "올바른 이메일을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}