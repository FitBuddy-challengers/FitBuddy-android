package com.cookandroid.challengers

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
        dialog.setOnShowListener { dlg ->
            val bottomSheet =
                (dlg as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            // 여기서 requireContext() 대신 dialog.context 사용
            bottomSheet?.background = ContextCompat.getDrawable(
                dialog.context,
                R.drawable.bottom_sheet_background
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val height = resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.peekHeight = height
                behavior.maxHeight = height
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        // 세트 수정하기
        binding.layoutSetEdit.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val exerciseSets = db.exerciseSetDao().getSetsByExerciseId(exercise.id)
                withContext(Dispatchers.Main) {
                    // newInstance 로 번들까지 세팅
                    val sheet = ExerciseEditSetFragment.newInstance(
                        exercise.id,
                        exerciseSets,
                        /*highlightIndex=*/ -1
                    )
                    sheet.setOnSetsUpdatedListener { updatedSets ->
                        // 변경된 세트 받아서 처리
                    }
                    // 🚩 Activity의 FragmentManager 에 띄우기
                    sheet.show(requireActivity().supportFragmentManager, ExerciseEditSetFragment.TAG)
                    // 부모 시트 닫기
                    dismiss()
                }
            }
        }

        // 운동 변경하기
        binding.layoutExerciseChange.setOnClickListener {
            dismiss()
            // TODO: 운동 변경 다이얼로그
        }

        // 운동 가이드
        binding.layoutExerciseGuide.setOnClickListener {
            dismiss()
            findNavController().navigate(
                R.id.action_global_exerciseDetailFragment,
                Bundle().apply { putLong("exerciseId", exercise.id) }
            )
        }

        // 즐겨찾기
        binding.layoutFavorite.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val updated = exercise.copy(isFavorite = !exercise.isFavorite)
                db.exerciseDao().update(updated)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        if (updated.isFavorite) "${exercise.name} 즐겨찾기 추가"
                        else "${exercise.name} 즐겨찾기 해제",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.menuFavorite.text =
                        if (updated.isFavorite) "즐겨찾기 해제" else "즐겨찾기"
                    binding.iconFavorite.isSelected = updated.isFavorite
                }
            }
        }

        // 운동 숨기기
        binding.layoutHideExercise.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.exerciseDao().update(exercise.copy(isHidden = true))
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "${exercise.name} 숨김", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }

        // 운동 삭제하기
        binding.layoutDeleteExercise.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.planDetailDao().delete(planDetail)
                db.exerciseSetDao().deleteSetsByExerciseId(exercise.id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "${exercise.name} 삭제", Toast.LENGTH_SHORT).show()
                    dismiss()
                    onExerciseDeleted()
                }
            }
        }

        binding.textTitle.text = exercise.name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
