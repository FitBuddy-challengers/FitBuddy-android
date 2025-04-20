package com.cookandroid.challengers.auth.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentProfileInfoBinding
import com.cookandroid.challengers.R

class ProfileInfoFragment : Fragment() {

    private var _binding: FragmentProfileInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var ageButtons: List<View>
    private lateinit var genderButtons: List<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기 상태
        binding.ageGroup.visibility = View.GONE
        binding.gender.visibility = View.GONE
        setNextButtonEnabled(false)

        // 버튼 리스트
        ageButtons = binding.ageGroup.children
            .filterIsInstance<ViewGroup>()
            .flatMap { it.children }
            .toList()

        genderButtons = binding.gender.children
            .filterIsInstance<ViewGroup>()
            .flatMap { it.children }
            .toList()

        binding.tilProfileName.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isNotEmpty = !s.isNullOrBlank()
                if (isNotEmpty && binding.ageGroup.visibility == View.GONE) {
                    binding.ageGroup.visibility = View.VISIBLE
                }
                updateNextButtonState()
            }
            override fun afterTextChanged(s: Editable?) {
            }
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

        // ProfileHealthFragment로 이동
        binding.btnNext.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.exercise_fragment_container, ProfileHealthFragment())
                .addToBackStack(null)  // 뒤로가기 가능하게
                .commit()
        }
    }

    private fun updateSelection(buttons: List<View>, selected: View) {
        buttons.forEach {
            it.isSelected = it == selected
        }
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