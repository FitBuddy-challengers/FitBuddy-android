package com.cookandroid.challengers

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.ExerciseEditSetFragment

import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseEditBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseEditFragment(
    private val planDetail: PlanDetail,
    private val exercise: Exercise,
    private val onExerciseDeleted: () -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ExerciseEditFragment"
    }

    private var _binding: FragmentExerciseEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet =
                (dialogInterface as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.background = ContextCompat.getDrawable(
                    dialog.context,
                    R.drawable.bottom_sheet_background
                )
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight =
                    resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.maxHeight =
                    resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        binding.textTitle.text = exercise.name
        binding.menuFavorite.text = if (exercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기"
        binding.iconFavorite.isSelected = exercise.isFavorite

        binding.layoutSetEdit.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val exerciseSets = db.exerciseSetDao().getSetsByExerciseId(exercise.id)
                withContext(Dispatchers.Main) {
                    val sheet = ExerciseEditSetFragment.newInstance(
                        planDetail.exercisePlanId,  // planId
                        exercise.id,                // exerciseId
                        exerciseSets,               // initialSetList
                        -1,                          // 하이라이트 인덱스
                        exercise.equip
                    )
                    sheet.show(
                        childFragmentManager,
                        ExerciseEditSetFragment.TAG
                    )
                }
            }
        }

        binding.layoutExerciseChange.setOnClickListener {
            val sheet = ExerciseChangeFragment.newInstance(
                planDetail.exercisePlanId,
                exercise.id
            )

            parentFragmentManager.setFragmentResultListener(
                "exercise_changed", viewLifecycleOwner
            ) { _, bundle ->
                val newId = bundle.getLong("newId")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val updatedRows = db.planDetailDao().replaceExercise(
                            planDetail.exercisePlanId,
                            exercise.id,
                            newId
                        )
                        if (updatedRows > 0) {
                            db.exerciseSetDao().updateExerciseId(
                                oldExerciseId = exercise.id,
                                newExerciseId = newId
                            )
                            db.exerciseDao().getExerciseById(newId)?.let { newEx ->
                                withContext(Dispatchers.Main) {
                                    binding.textTitle.text = newEx.name
                                    Toast.makeText(
                                        requireContext(),
                                        "운동이 ${newEx.name}으로 변경되었습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    parentFragmentManager.setFragmentResult(
                                        "sets_updated",
                                        Bundle()
                                    )
                                    dismiss()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "운동 변경 실패: PlanDetail 업데이트 안됨",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ExerciseChange", "Error updating exercise: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "운동 변경 중 오류 발생: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            sheet.show(childFragmentManager, "ExerciseChange")
        }

        binding.layoutExerciseGuide.setOnClickListener {
            dismiss()
            findNavController().navigate(
                R.id.action_global_exerciseDetailFragment,
                Bundle().apply { putLong("exerciseId", exercise.id) }
            )
        }

        binding.layoutFavorite.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val updatedExercise = exercise.copy(isFavorite = !exercise.isFavorite)
                db.exerciseDao().update(updatedExercise)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        if (updatedExercise.isFavorite) "${exercise.name} 즐겨찾기 추가" else "${exercise.name} 즐겨찾기 해제",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.menuFavorite.text =
                        if (updatedExercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기"
                    binding.iconFavorite.isSelected = updatedExercise.isFavorite
                    parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                }
            }
        }

        binding.layoutDeleteExercise.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.planDetailDao().delete(planDetail)
                db.exerciseSetDao().deleteSetsByExerciseId(exercise.id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "${exercise.name} 삭제", Toast.LENGTH_SHORT)
                        .show()
                    parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                    dismiss()
                    onExerciseDeleted()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
