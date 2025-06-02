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

    private var currentIsFavorite: Boolean = false // 현재 운동의 즐겨찾기 상태 (기존 선언 유지)

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

        // 즐겨찾기 초기 상태 불러오기
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val scheduleIdRes = RetrofitClient.scheduleApi.getScheduleId(planId, exerciseId)
                val scheduleId = scheduleIdRes.body()?.scheduleId
                    ?: throw IllegalStateException("❌ scheduleId 가져오기 실패")

                val exerciseInfoRes  = RetrofitClient.scheduleApi.getExerciseInfo(scheduleId)
                val exerciseDto = exerciseInfoRes.body()
                    ?: throw IllegalStateException("❌ 운동 정보 없음")

                currentIsFavorite = exerciseDto.isFavorite // isFavorite 필드 가져오기
                withContext(Dispatchers.Main) {
                    binding.menuFavorite.text = if (currentIsFavorite) "즐겨찾기 해제" else "즐겨찾기"
                    binding.iconFavorite.isSelected = currentIsFavorite
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ExerciseEditFragment", "운동 즐겨찾기 상태 불러오기 실패", e)
                    Toast.makeText(requireContext(), "즐겨찾기 상태 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        parentFragmentManager.setFragmentResultListener("dialog_sets_updated", viewLifecycleOwner) { requestKey, bundle ->
            if (requestKey == "dialog_sets_updated") {
                Log.d("ExerciseEditFragment", "dialog_sets_updated 결과 수신. 부모에게 sets_updated 전달.")
                // ExerciseEditFragment의 부모(주요 화면)에게 "sets_updated" 결과 전달
                parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                dismiss() // ExerciseEditFragment 자신을 닫음
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
                                .show(parentFragmentManager, TimeSetEditDialogFragment.TAG)
                        } else {
                            RepsSetEditDialogFragment.newInstance(scheduleId)
                                .show(parentFragmentManager, RepsSetEditDialogFragment.TAG)
                            Log.d("세트수정", "📦 다이얼로그 호출 전 scheduleId = $scheduleId")
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("ExerciseEditFragment", "세트 수정 불러오기 실패", e) // 에러 로그에 예외 포함
                        Toast.makeText(requireContext(), "세트 수정 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 현재 즐겨찾기 상태를 반전
                    val newFavoriteStatus = !currentIsFavorite
                    val requestBody = RetrofitClient.ToggleFavoriteRequest(newFavoriteStatus)
                    val response = RetrofitClient.exerciseApi.toggleExerciseFavorite(exerciseId, requestBody)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val serverResponseBody = response.body()!!
                            Log.d("ExerciseEditFragment", "서버 전체 응답 body: $serverResponseBody") // ★ 실제 서버 응답 객체 확인
                            Log.d("ExerciseEditFragment", "파싱된 isFavorite 값: ${serverResponseBody.isFavorite}") // ★ DTO에서 파싱된 값 확인

                            currentIsFavorite = serverResponseBody.isFavorite
                            Log.d("ExerciseEditFragment", "업데이트된 currentIsFavorite: $currentIsFavorite")

                            Toast.makeText(
                                requireContext(),
                                if (currentIsFavorite) "${exerciseName} 즐겨찾기 추가" else "${exerciseName} 즐겨찾기 해제",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.menuFavorite.text =
                                if (currentIsFavorite) "즐겨찾기 해제" else "즐겨찾기"
                            binding.iconFavorite.isSelected = currentIsFavorite
                            parentFragmentManager.setFragmentResult("sets_updated", Bundle())
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "즐겨찾기 상태 변경 실패: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("ExerciseEditFragment", "Toggle favorite failed: ${response.errorBody()?.string()}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "즐겨찾기 상태 변경 중 오류 발생: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("ExerciseEditFragment", "Toggle favorite exception", e)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
