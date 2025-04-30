package com.cookandroid.challengers.auth.profile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentProfileInfoBinding
import com.cookandroid.challengers.viewmodel.ProfileViewModel

class ProfileInfoFragment : Fragment() {

    private var _binding: FragmentProfileInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var ageButtons: List<View>
    private lateinit var genderButtons: List<View>

    // ✅ ViewModel 연결
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 이메일 불러오기
        val prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
        viewModel.email = prefs.getString("email", "") ?: ""

        // 초기 상태
        binding.ageGroup.visibility = View.GONE
        binding.gender.visibility = View.GONE
        setNextButtonEnabled(false)

        // 버튼 리스트 설정
        ageButtons = binding.ageGroup.children
            .filterIsInstance<ViewGroup>()
            .flatMap { it.children }
            .toList()

        genderButtons = binding.gender.children
            .filterIsInstance<ViewGroup>()
            .flatMap { it.children }
            .toList()

        // 이름 입력 감지
        binding.tilProfileName.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isNotEmpty = !s.isNullOrBlank()
                if (isNotEmpty && binding.ageGroup.visibility == View.GONE) {
                    binding.ageGroup.visibility = View.VISIBLE
                }
                updateNextButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 연령대 선택
        ageButtons.forEach { button ->
            button.setOnClickListener {
                updateSelection(ageButtons, button)
                if (binding.gender.visibility == View.GONE) {
                    binding.gender.visibility = View.VISIBLE
                }
                updateNextButtonState()
            }
        }

        // 성별 선택
        genderButtons.forEach { button ->
            button.setOnClickListener {
                updateSelection(genderButtons, button)
                updateNextButtonState()
            }
        }

        // 다음 버튼 → ViewModel에 저장 후 이동
        binding.btnNext.setOnClickListener {
            viewModel.name = binding.tilProfileName.editText?.text.toString()
            viewModel.ageGroup = ageButtons.find { it.isSelected }?.let { (it as Button).text.toString() } ?: ""
            viewModel.gender = genderButtons.find { it.isSelected }?.let { (it as Button).text.toString() } ?: ""

            findNavController().navigate(R.id.action_profileInfo_to_profileHealth)
        }
    }

    private fun updateSelection(buttons: List<View>, selected: View) {
        buttons.forEach { it.isSelected = it == selected }
    }

    private fun updateNextButtonState() {
        val isNameEntered = !binding.tilProfileName.editText?.text.isNullOrBlank()
        val isAgeSelected = ageButtons.any { it.isSelected }
        val isGenderSelected = genderButtons.any { it.isSelected }

        val enabled = isNameEntered && isAgeSelected && isGenderSelected
        setNextButtonEnabled(enabled)
    }

    private fun setNextButtonEnabled(enabled: Boolean) {
        binding.btnNext.isEnabled = enabled
        val backgroundRes = if (enabled) R.drawable.btn_next_blue else R.drawable.btn_next_gray
        binding.btnNext.setBackgroundResource(backgroundRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}