package com.cookandroid.challengers.auth.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.auth.profile.ProfileDoneFragment
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentProfileWorkoutBinding

class ProfileWorkoutFragment : Fragment() {

    private var _binding: FragmentProfileWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var levelButtons: List<Button>
    private lateinit var prefExerciseButtons: List<Button>
    private lateinit var equipmentButtons: List<Button>

    private val selectedPrefExercises = mutableSetOf<Button>()
    private val selectedEquipments = mutableSetOf<Button>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 버튼 모음
        levelButtons = collectButtons(binding.workoutLevel)
        prefExerciseButtons = collectButtons(binding.prefExercise)
        equipmentButtons = collectButtons(binding.equipment)

        // 초기 설정
        binding.prefExercise.visibility = View.GONE
        binding.equipment.visibility = View.GONE
        binding.btnNext.isEnabled = false

        // 운동 수준
        levelButtons.forEach { button ->
            button.setOnClickListener {
                selectSingleButton(levelButtons, button)
                binding.prefExercise.visibility = View.VISIBLE
            }
        }

        // 선호 운동 버튼 - 최대 3개 선택
        prefExerciseButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedPrefExercises.contains(button)) {
                    selectedPrefExercises.remove(button)
                    button.isSelected = false
                } else if (selectedPrefExercises.size < 3) {
                    selectedPrefExercises.add(button)
                    button.isSelected = true
                }

                // 선호 운동 선택 - 기구 레이아웃
                binding.equipment.visibility =
                    if (selectedPrefExercises.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        // 운동 기구 버튼 - 중복 선택
        equipmentButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedEquipments.contains(button)) {
                    selectedEquipments.remove(button)
                    button.isSelected = false
                } else {
                    selectedEquipments.add(button)
                    button.isSelected = true
                }

                // 기구가 하나 이상 선택되면 다음 버튼 활성화
                updateNextButtonState()
            }
        }

        binding.btnNext.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.exercise_fragment_container, ProfileDoneFragment())
                .addToBackStack(null) // 뒤로가기 가능하도록
                .commit()
        }
    }

    private fun updateNextButtonState() {
        binding.btnNext.isEnabled = selectedEquipments.isNotEmpty()
        binding.btnNext.setBackgroundResource(
            if (selectedEquipments.isNotEmpty()) R.drawable.btn_next_blue
            else R.drawable.btn_next_gray
        )
    }

    private fun collectButtons(container: ViewGroup): List<Button> {
        return container.children
            .filterIsInstance<LinearLayout>()
            .flatMap { it.children.toList() }
            .filterIsInstance<Button>().toList()
    }

    private fun selectSingleButton(buttons: List<Button>, selected: Button) {
        buttons.forEach { it.isSelected = false }
        selected.isSelected = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}