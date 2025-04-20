package com.cookandroid.challengers.auth.signup

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentSignupOtpBinding

class SignupOtpFragment : Fragment() {

    private var _binding: FragmentSignupOtpBinding? = null
    private val binding get() = _binding!!

    private lateinit var otpFields: List<EditText>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // OTP 입력 처리
        otpFields = listOf(
            binding.otp1, binding.otp2, binding.otp3,
            binding.otp4, binding.otp5, binding.otp6
        )
        otpFields.forEachIndexed { i, field ->
            field.inputType = InputType.TYPE_CLASS_NUMBER
            field.addTextChangedListener(createOtpTextWatcher(i))
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            val otp = otpFields.joinToString("") { it.text.toString() }

            if (otp.length == 6 && otp.all { it.isDigit() }) {
                findNavController().navigate(R.id.action_otp_to_done)
            } else {
                Toast.makeText(requireContext(), "인증번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        // 뒤로 버튼
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // TextWatcher
    private fun createOtpTextWatcher(index: Int): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentField = otpFields[index]

                // 입력값 1자
                if (s?.length ?: 0 > 1) {
                    currentField.setText(s?.last().toString())
                    currentField.setSelection(1)
                }
                // 다음 칸 이동
                if (s?.length == 1 && index < otpFields.size - 1) {
                    otpFields[index + 1].requestFocus()
                }
                // 삭제 시 이전 칸 이동
                if (s?.isEmpty() == true && before == 1 && index > 0) {
                    otpFields[index - 1].apply {
                        requestFocus()
                        setSelection(text?.length ?: 0)
                    }
                }
                validateOtp()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    // OTP 유효성 검사
    private fun validateOtp() {
        val otp = otpFields.joinToString("") { it.text.toString() }
        val isComplete = otp.length == 6 && otp.all { it.isDigit() }

        binding.btnNext.apply {
            isEnabled = isComplete
            setBackgroundResource(
                if (isComplete) R.drawable.btn_next_blue
                else R.drawable.btn_next_gray
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}