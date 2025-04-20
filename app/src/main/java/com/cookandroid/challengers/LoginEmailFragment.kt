package com.cookandroid.challengers


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding


class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "이메일로 가입하기" 버튼 클릭하면 -> SignUpEmailFragment로 이동
        binding.btnEmailSignup.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, SignUpEmailFragment())
                .addToBackStack(null)  // 뒤로가기 가능하게
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}