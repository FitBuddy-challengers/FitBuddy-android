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
import androidx.room.withTransaction
import com.cookandroid.challengers.ExerciseEditSetFragment
import com.cookandroid.challengers.api.RetrofitClient

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

import com.cookandroid.challengers.api.ScheduleApi
import com.cookandroid.challengers.data.ExerciseSet
class ExerciseEditFragment(
    private val planId: Long,
    private val exerciseId: Long,
    private val exerciseName: String,
    private val onExerciseDeleted: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentExerciseEditBinding? = null
    private val binding get() = _binding!!

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
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.background = ContextCompat.getDrawable(dialog.context, R.drawable.bottom_sheet_background)
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.maxHeight = resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 운동 이름 반영
        binding.textTitle.text = exerciseName

        binding.layoutSetEdit.setOnClickListener {
            Toast.makeText(requireContext(), "세트 수정 기능은 추후 구현됩니다.", Toast.LENGTH_SHORT).show()
        }

        binding.layoutExerciseChange.setOnClickListener {
            val sheet = ExerciseChangeFragment.newInstance(planId, exerciseId)

            // ✅ 리스너 등록은 parent에
            parentFragmentManager.setFragmentResultListener("exercise_changed", viewLifecycleOwner) { _, bundle ->
                val newExerciseId = bundle.getLong("newId")
                if (newExerciseId != exerciseId) {
                    Toast.makeText(requireContext(), "운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                    dismiss()
                }
            }

            // ✅ 프래그먼트도 parent에 띄워야 setFragmentResult가 연동됨
            sheet.show(parentFragmentManager, "ExerciseChange")
        }

        binding.layoutDeleteExercise.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.scheduleApi.getScheduleId(planId, exerciseId)
                    val scheduleId = response.body()?.scheduleId
                        ?: throw IllegalStateException("❌ scheduleId 가져오기 실패")

                    val deleteResponse = RetrofitClient.scheduleApi.deleteExercise(scheduleId)

                    withContext(Dispatchers.Main) {
                        if (deleteResponse.isSuccessful) {
                            Toast.makeText(requireContext(), "운동 삭제 완료", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                            dismiss()
                            onExerciseDeleted()
                        } else {
                            Toast.makeText(requireContext(), "운동 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "운동 삭제 중 오류 발생", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.layoutSetEdit.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val scheduleIdRes = RetrofitClient.scheduleApi.getScheduleId(planId, exerciseId)
                    val scheduleId = scheduleIdRes.body()?.scheduleId
                        ?: throw IllegalStateException("❌ scheduleId 가져오기 실패")

                    val exerciseInfoRes = RetrofitClient.scheduleApi.getExerciseInfo(scheduleId)
                    val isTimeType = exerciseInfoRes.body()?.isTimeType
                        ?: throw IllegalStateException("❌ 운동 타입 정보 없음")

                    withContext(Dispatchers.Main) {
                        if (isTimeType) {
                            TimeSetEditDialogFragment.newInstance(scheduleId)
                                .show(parentFragmentManager, "TimeSetEdit")
                        } else {
                            RepsSetEditDialogFragment.newInstance(scheduleId)
                                .show(parentFragmentManager, "RepsSetEdit")
                            Log.d("세트수정", "📦 다이얼로그 호출 전 scheduleId = $scheduleId")
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "세트 수정 불러오기 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.layoutExerciseGuide.setOnClickListener {
            dismiss()
            findNavController().navigate(
                R.id.action_global_exerciseDetailFragment,
                Bundle().apply { putLong("exerciseId", exerciseId) }
            )
        }

        binding.layoutFavorite.setOnClickListener {
            Toast.makeText(requireContext(), "즐겨찾기 기능은 현재 비활성화됨", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//
//class ExerciseEditFragment(
//    private val planDetail: PlanDetail,
//    private val exercise: Exercise,
//    private val onExerciseDeleted: () -> Unit
//) : BottomSheetDialogFragment() {
//
//    companion object {
//        const val TAG = "ExerciseEditFragment"
//    }
//
//    private var _binding: FragmentExerciseEditBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var db: AppDatabase
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentExerciseEditBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
//        dialog.setOnShowListener { dialogInterface ->
//            val bottomSheet =
//                (dialogInterface as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//            bottomSheet?.let {
//                it.background = ContextCompat.getDrawable(
//                    dialog.context,
//                    R.drawable.bottom_sheet_background
//                )
//                val behavior = BottomSheetBehavior.from(it)
//                behavior.peekHeight =
//                    resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
//                behavior.maxHeight =
//                    resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
//                behavior.state = BottomSheetBehavior.STATE_EXPANDED
//            }
//        }
//        return dialog
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
//
//        binding.textTitle.text = exercise.name
//        binding.menuFavorite.text = if (exercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기"
//        binding.iconFavorite.isSelected = exercise.isFavorite
//
//        binding.layoutSetEdit.setOnClickListener {
//            // 생략됨
//        }
//
//        binding.layoutExerciseChange.setOnClickListener {
//            val sheet = ExerciseChangeFragment.newInstance(
//                planDetail.exercisePlanId,
//                exercise.id
//            )
//
//            // ✅ 콜백 방식 제거하고 다시 리스너 등록
//            parentFragmentManager.setFragmentResultListener(
//                "exercise_changed", viewLifecycleOwner
//            ) { _, bundle ->
//                val serverExerciseId = bundle.getLong("newId")
//
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//                        // ✅ 0. 서버에서 scheduleId 조회
//                        val scheduleIdResponse = RetrofitClient.scheduleApi.getScheduleId(
//                            planId = planDetail.exercisePlanId,
//                            exerciseId = exercise.id
//                        )
//
//                        if (!scheduleIdResponse.isSuccessful || scheduleIdResponse.body() == null) {
//                            throw IllegalStateException("서버에서 scheduleId를 찾을 수 없습니다.")
//                        }
//
//                        val scheduleId = scheduleIdResponse.body()!!.scheduleId
//                        val planId = planDetail.exercisePlanId // 기존 PlanDetail에서 planId만 따로 저장
//
//                        // ✅ 1. Room 트랜잭션
//                        val refreshedExercise = db.withTransaction {
//                            // ① Exercise 존재 확인 및 insert
//                            var newExercise = db.exerciseDao().getExerciseById(serverExerciseId)
//                            if (newExercise == null) {
//                                try {
//                                    db.exerciseDao().insert(
//                                        Exercise(
//                                            id = serverExerciseId,
//                                            name = "신규 운동",
//                                            part = "기타",
//                                            equip = "없음",
//                                            mets = 4.0
//                                        )
//                                    )
//                                    Log.d("InsertExercise", "✅ Exercise(id=$serverExerciseId) 삽입 시도")
//                                } catch (e: Exception) {
//                                    Log.e("InsertExercise", "❗ Exercise insert 중 예외 발생: ${e.message}")
//                                }
//
//                                newExercise = db.exerciseDao().getExerciseById(serverExerciseId)
//                                if (newExercise == null) {
//                                    throw IllegalStateException("🚨 Exercise insert 후에도 조회 실패 (id=$serverExerciseId)")
//                                }
//                            }
//                            val roomExerciseId = newExercise.id
//
//                            // ② PlanDetail 존재 여부 확인 및 insert → insert 후 재조회
//                            var newPlanDetail = db.planDetailDao().getByPlanAndExercise(planId, roomExerciseId)
//                            if (newPlanDetail == null) {
//                                try {
//                                    db.planDetailDao().insert(
//                                        PlanDetail(
//                                            exercisePlanId = planId,
//                                            exerciseId = roomExerciseId,
//                                            exOrder = 0
//                                        )
//                                    )
//                                    Log.d("InsertPlanDetail", "✅ PlanDetail(planId=$planId, exerciseId=$roomExerciseId) 삽입 시도")
//                                } catch (e: Exception) {
//                                    Log.e("InsertPlanDetail", "❗ PlanDetail insert 중 예외 발생: ${e.message}")
//                                }
//
//                                newPlanDetail = db.planDetailDao().getByPlanAndExercise(planId, roomExerciseId)
//                                if (newPlanDetail == null) {
//                                    throw IllegalStateException("🚨 PlanDetail insert 후에도 조회 실패 (planId=$planId, exId=$roomExerciseId)")
//                                }
//                            }
//
//                            // ③ 기존 ExerciseSet 삭제
//                            db.exerciseSetDao().deleteSetsByPlanAndExerciseId(
//                                planId = planId,
//                                exerciseId = exercise.id
//                            )
//
//                            // ④ PlanDetail 교체
//                            val updated = db.planDetailDao().replaceExercise(
//                                planId = planId,
//                                oldExerciseId = exercise.id,
//                                newExerciseId = roomExerciseId
//                            )
//                            Log.d("PlanDetail", "🔁 PlanDetail 교체 결과: $updated rows")
//
//                            // ⑤ 세트 삽입
//                            for (setNumber in 1..3) {
//                                try {
//                                    db.exerciseSetDao().insert(
//                                        ExerciseSet(
//                                            exercisePlanId = planId,
//                                            exerciseId = roomExerciseId,
//                                            setNumber = setNumber,
//                                            weight = 0,
//                                            reps = 12,
//                                            isCompleted = false
//                                        )
//                                    )
//                                    Log.d("InsertSet", "✅ 세트 삽입 완료 (set=$setNumber, exId=$roomExerciseId)")
//                                } catch (e: Exception) {
//                                    Log.e("InsertSet", "❗ 세트 삽입 중 오류 (set=$setNumber): ${e.message}")
//                                }
//                            }
//
//                            newExercise
//                        }
//
//                        // ✅ 2. 서버 반영
//                        val changeResponse = RetrofitClient.scheduleApi.changeExerciseServer(
//                            scheduleId = scheduleId,
//                            request = RetrofitClient.ChangeExerciseServerRequest(newExerciseId = serverExerciseId)
//                        )
//
//                        // ✅ 3. UI 갱신
//                        withContext(Dispatchers.Main) {
//                            if (changeResponse.isSuccessful) {
//                                binding.textTitle.text = refreshedExercise?.name ?: "운동 변경됨"
//                                Toast.makeText(
//                                    requireContext(),
//                                    "운동이 ${refreshedExercise?.name}으로 변경되었습니다.",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                                parentFragmentManager.setFragmentResult("sets_updated", Bundle())
//                                dismiss()
//                            } else {
//                                Toast.makeText(requireContext(), "서버 운동 변경 실패", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//
//                    } catch (e: Exception) {
//                        Log.e("ExerciseEditFragment", "❗ 오류 발생: ${e.message}")
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireContext(), "운동 변경 중 오류", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//
//            // ✅ 콜백 제거된 채로 show
//            sheet.show(childFragmentManager, "ExerciseChange")
//        }
//        binding.layoutExerciseGuide.setOnClickListener {
//            dismiss()
//            findNavController().navigate(
//                R.id.action_global_exerciseDetailFragment,
//                Bundle().apply { putLong("exerciseId", exercise.id) }
//            )
//        }
//
//        binding.layoutFavorite.setOnClickListener {
//            lifecycleScope.launch(Dispatchers.IO) {
//                val updatedExercise = exercise.copy(isFavorite = !exercise.isFavorite)
//                db.exerciseDao().update(updatedExercise)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        requireContext(),
//                        if (updatedExercise.isFavorite) "${exercise.name} 즐겨찾기 추가" else "${exercise.name} 즐겨찾기 해제",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    binding.menuFavorite.text =
//                        if (updatedExercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기"
//                    binding.iconFavorite.isSelected = updatedExercise.isFavorite
//                    parentFragmentManager.setFragmentResult("sets_updated", Bundle())
//                }
//            }
//        }
//
//        binding.layoutDeleteExercise.setOnClickListener {
//            lifecycleScope.launch(Dispatchers.IO) {
//                // ✅ 1. RoomDB 삭제
//                db.planDetailDao().delete(planDetail)
//                db.exerciseSetDao().deleteSetsByPlanAndExerciseId(
//                    planId = planDetail.exercisePlanId,
//                    exerciseId = exercise.id
//                )
//
//                // ✅ 2. 서버에서 scheduleId 조회 → 삭제 요청
//                try {
//                    val response = RetrofitClient.scheduleApi.getScheduleId(
//                        planId = planDetail.exercisePlanId,
//                        exerciseId = exercise.id
//                    )
//                    if (response.isSuccessful) {
//                        val scheduleId = response.body()?.scheduleId
//                        if (scheduleId != null) {
//                            val deleteResponse = RetrofitClient.scheduleApi.deleteExercise(scheduleId)
//                            if (!deleteResponse.isSuccessful) {
//                                Log.e("ExerciseDelete", "❌ 운동 삭제 실패: ${deleteResponse.code()}")
//                            }
//                        } else {
//                            Log.e("ExerciseDelete", "❌ scheduleId가 null입니다.")
//                        }
//                    } else {
//                        Log.e("ExerciseDelete", "❌ scheduleId 조회 실패: ${response.code()}")
//                    }
//                } catch (e: Exception) {
//                    Log.e("ExerciseDelete", "❗ 서버 운동 삭제 요청 실패: ${e.message}")
//                }
//
//                // ✅ 3. UI 갱신
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "${exercise.name} 삭제", Toast.LENGTH_SHORT)
//                        .show()
//                    parentFragmentManager.setFragmentResult("sets_updated", Bundle())
//                    dismiss()
//                    onExerciseDeleted()
//                }
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
