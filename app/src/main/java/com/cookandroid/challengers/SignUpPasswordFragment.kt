package com.cookandroid.challengers


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.databinding.FragmentSignupPwBinding

class SignUpPasswordFragment : Fragment() {

    private var _binding: FragmentSignupPwBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignupPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

            // 비밀번호 검증 통과하면 OTP 입력 화면으로 이동
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, SignUpOtpFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}