package com.cookandroid.challengers

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.databinding.FragmentSignupEmailBinding

class SignUpEmailFragment : Fragment() {

    private var _binding: FragmentSignupEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignupEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "다음" 버튼 클릭
        binding.btnNext.setOnClickListener {
            val emailInput = binding.tilLoginAddress.editText?.text.toString()

            if (isValidEmail(emailInput)) {
                // 수정 시작
                val fragment = SignUpPasswordFragment()
                val bundle = Bundle()
                bundle.putString("email", emailInput) // 입력한 이메일을 번들에 저장
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, fragment)
                    .addToBackStack(null)
                    .commit()
                // 수정 끝
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