package com.cookandroid.challengers.auth.profile


import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentProfileWorkoutBinding
import com.cookandroid.challengers.viewmodel.ProfileViewModel

class ProfileWorkoutFragment : Fragment() {

    private var _binding: FragmentProfileWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var levelButtons: List<Button>
    private lateinit var prefExerciseButtons: List<Button>
    private lateinit var equipmentButtons: List<Button>

    private val selectedPrefExercises = mutableSetOf<Button>()
    private val selectedEquipments = mutableSetOf<Button>()

    // ✅ ViewModel 연결
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 버튼 리스트 수집
        levelButtons = collectButtons(binding.workoutLevel)
        prefExerciseButtons = collectButtons(binding.prefExercise)
        equipmentButtons = collectButtons(binding.equipment)

        // 초기 상태
        binding.prefExercise.visibility = View.GONE
        binding.equipment.visibility = View.GONE
        binding.btnNext.isEnabled = false

        // 운동 수준 (단일 선택)
        levelButtons.forEach { button ->
            button.setOnClickListener {
                selectSingleButton(levelButtons, button)
                binding.prefExercise.visibility = View.VISIBLE
            }
        }

        // 선호 운동 (최대 3개 선택)
        prefExerciseButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedPrefExercises.contains(button)) {
                    selectedPrefExercises.remove(button)
                    button.isSelected = false
                } else if (selectedPrefExercises.size < 3) {
                    selectedPrefExercises.add(button)
                    button.isSelected = true
                }

                // 선택되면 기구 영역 보여주기
                binding.equipment.visibility =
                    if (selectedPrefExercises.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        // 운동기구 (다중 선택)
        equipmentButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedEquipments.contains(button)) {
                    selectedEquipments.remove(button)
                    button.isSelected = false
                } else {
                    selectedEquipments.add(button)
                    button.isSelected = true
                }

                updateNextButtonState()
            }
        }

        // ✅ ViewModel 저장 후 다음 프래그먼트로 이동
        binding.btnNext.setOnClickListener {
            val workoutLevel = levelButtons.find { it.isSelected }?.text?.toString() ?: ""
            val preferredWorkouts = selectedPrefExercises.map { it.text.toString() }
            val equipment = selectedEquipments.map { it.text.toString() }

            if (preferredWorkouts.isEmpty()) {
                Toast.makeText(requireContext(), "선호 운동을 하나 이상 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (equipment.isEmpty()) {
                Toast.makeText(requireContext(), "운동 기구를 하나 이상 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.workoutLevel = workoutLevel
            viewModel.preferredWorkouts = preferredWorkouts
            viewModel.equipment = equipment

            findNavController().navigate(R.id.action_profileWorkout_to_profileDone)
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
            .filterIsInstance<Button>()
            .toList()
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

/*
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
            // ✅ Navigation으로 이동
            findNavController().navigate(R.id.action_profileWorkout_to_profileDone)
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
}*/