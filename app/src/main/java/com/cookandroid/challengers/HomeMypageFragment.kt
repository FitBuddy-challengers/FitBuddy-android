package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentHomeMypageBinding
import com.cookandroid.challengers.util.UserPreference

class HomeMypageFragment : Fragment() {

    private var _binding: FragmentHomeMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userPref = UserPreference(requireContext())

        // 사용자 정보 표시
        binding.userName.text = userPref.getUserName()
        binding.userEmail.text = userPref.getUserEmail()

        // 뒤로 가기 버튼
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 정보 수정
        binding.editInfo.setOnClickListener {
            findNavController().navigate(R.id.action_homeMypageFragment_to_mypageInfoSetFragment)
        }

        // 계정 설정
        binding.editAccount.setOnClickListener {
            findNavController().navigate(R.id.action_homeMypageFragment_to_mypageAccountSetFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
