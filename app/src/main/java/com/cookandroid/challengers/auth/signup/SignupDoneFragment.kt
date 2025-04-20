package com.cookandroid.challengers.auth.signup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.HomeFragment
import com.cookandroid.challengers.databinding.FragmentSignupDoneBinding

class SignUpDoneFragment : Fragment() { // ★ 클래스명도 일관성

    private var _binding: FragmentSignupDoneBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupDoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "홈으로 이동" 버튼 클릭 시
        binding.btnNext.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, HomeFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}