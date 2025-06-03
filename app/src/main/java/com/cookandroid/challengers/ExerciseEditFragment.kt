package com.cookandroid.challengers

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentExerciseEditBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseEditFragment(
    // Constructor parameters to match the call from ExerciseFragment
    private val initialPlanId: Long,
    private val initialExerciseId: Long, // This is the 'original' exerciseId being edited/deleted
    private val initialExerciseName: String,
    private val onExerciseDeletedAction: () -> Unit // Callback for when an exercise is deleted
) : BottomSheetDialogFragment() {

    private var _binding: FragmentExerciseEditBinding? = null
    private val binding get() = _binding!!

    // Member variables to store the initial data
    private var planId: Long = -1L
    private var exerciseId: Long = -1L
    private var exerciseName: String = ""

    private var currentIsFavorite: Boolean = false
    private var currentScheduleIdForEdit: Long = -1L // API를 통해 가져온, 현재 운동의 실제 schedule_id

    companion object {
        private const val TAG = "ExerciseEditFragment"
        // 부모 Fragment와 결과 교환을 위한 키 (삭제 외 다른 업데이트 알림용)
        const val REQUEST_KEY_EXERCISE_EDIT = "exercise_edit_request_key"
        const val RESULT_KEY_UPDATE_NEEDED = "result_update_needed"
        // RESULT_KEY_EXERCISE_DELETED는 직접 콜백으로 대체되지만, RESULT_KEY_UPDATE_NEEDED는 여전히 유용
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use constructor-injected values
        this.planId = initialPlanId
        this.exerciseId = initialExerciseId
        this.exerciseName = initialExerciseName
        Log.d(TAG, "onCreate: planId=$planId, exerciseId=$exerciseId, exerciseName='$exerciseName' (from constructor)")
    }

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
                val fixedHeight = resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height)
                behavior.peekHeight = fixedHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing for planId=$planId, exerciseId=$exerciseId, exerciseName='$exerciseName'")

        binding.textTitle.text = exerciseName
        disableInteractionButtons()
        loadInitialStates()

        parentFragmentManager.setFragmentResultListener("dialog_sets_updated", viewLifecycleOwner) { _, _ ->
            Log.d(TAG, "Received 'dialog_sets_updated'. Notifying parent (ExerciseFragment) and dismissing self.")
            setFragmentResult(REQUEST_KEY_EXERCISE_EDIT, bundleOf(RESULT_KEY_UPDATE_NEEDED to true))
            dismiss()
        }

        parentFragmentManager.setFragmentResultListener("exercise_changed", viewLifecycleOwner) { _, bundle ->
            val newExerciseId = bundle.getLong("newId", -1L)
            Log.d(TAG, "Received 'exercise_changed'. newExerciseId: $newExerciseId")
            if (newExerciseId != -1L && newExerciseId != exerciseId) {
                Toast.makeText(requireContext(), "운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                setFragmentResult(REQUEST_KEY_EXERCISE_EDIT, bundleOf(RESULT_KEY_UPDATE_NEEDED to true))
                dismiss()
            } else if (newExerciseId == -1L) {
                Log.w(TAG, "Exercise change reported, but newExerciseId is invalid.")
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.layoutSetEdit.setOnClickListener {
            if (currentScheduleIdForEdit <= 0) {
                Toast.makeText(requireContext(), "세트 정보를 불러올 수 없습니다 (스케줄 ID 오류).", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "layoutSetEdit: currentScheduleIdForEdit is invalid: $currentScheduleIdForEdit")
                return@setOnClickListener
            }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val exerciseInfoRes = RetrofitClient.scheduleApi.getExerciseInfo(currentScheduleIdForEdit)
                    if (!exerciseInfoRes.isSuccessful || exerciseInfoRes.body() == null) {
                        throw IllegalStateException("운동 타입 정보 조회 실패: ${exerciseInfoRes.code()}")
                    }
                    val isTimeType = exerciseInfoRes.body()!!.isTimeType
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        val sheet: BottomSheetDialogFragment = if (isTimeType) {
                            TimeSetEditDialogFragment.newInstance(currentScheduleIdForEdit)
                        } else {
                            RepsSetEditDialogFragment.newInstance(currentScheduleIdForEdit)
                        }
                        sheet.show(parentFragmentManager, if (isTimeType) TimeSetEditDialogFragment.TAG else RepsSetEditDialogFragment.TAG)
                        Log.d(TAG, "Showing ${if (isTimeType) "TimeSetEditDialogFragment" else "RepsSetEditDialogFragment"} for scheduleId: $currentScheduleIdForEdit")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Log.e(TAG, "세트 수정 화면 로드 실패", e)
                            Toast.makeText(requireContext(), "세트 수정 화면 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.layoutExerciseChange.setOnClickListener {
            if (currentScheduleIdForEdit <= 0) {
                Toast.makeText(requireContext(), "운동 변경 정보를 불러올 수 없습니다 (스케줄 ID 오류).", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "layoutExerciseChange: currentScheduleIdForEdit is invalid: $currentScheduleIdForEdit")
                return@setOnClickListener
            }
            val sheet = ExerciseChangeFragment.newInstance(planId, exerciseId, currentScheduleIdForEdit)
            sheet.show(parentFragmentManager, "ExerciseChangeFragment_TAG")
        }

        binding.layoutDeleteExercise.setOnClickListener {
            if (currentScheduleIdForEdit <= 0) {
                Toast.makeText(requireContext(), "삭제할 운동 정보를 찾을 수 없습니다 (스케줄 ID 오류).", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "layoutDeleteExercise: currentScheduleIdForEdit is invalid: $currentScheduleIdForEdit. Cannot delete.")
                return@setOnClickListener
            }
            Log.d(TAG, "Attempting to delete exercise with scheduleId: $currentScheduleIdForEdit, for original exerciseId: $exerciseId")
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val deleteResponse = RetrofitClient.scheduleApi.deleteExercise(currentScheduleIdForEdit)
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        if (deleteResponse.isSuccessful) {
                            Toast.makeText(requireContext(), "'${exerciseName}' 운동이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            onExerciseDeletedAction() // Invoke the callback passed from ExerciseFragment
                            setFragmentResult(REQUEST_KEY_EXERCISE_EDIT, bundleOf(RESULT_KEY_UPDATE_NEEDED to true)) // Notify for general update
                            dismiss()
                        } else {
                            val errorMsg = deleteResponse.errorBody()?.string() ?: "알 수 없는 오류 (${deleteResponse.code()})"
                            Log.e(TAG, "운동 삭제 실패: ${deleteResponse.code()} - $errorMsg")
                            Toast.makeText(requireContext(), "운동 삭제 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Log.e(TAG, "운동 삭제 중 예외 발생", e)
                            Toast.makeText(requireContext(), "운동 삭제 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.layoutExerciseGuide.setOnClickListener {
            dismiss()
            if (exerciseId > 0) {
                findNavController().navigate(
                    R.id.action_global_exerciseDetailFragment,
                    Bundle().apply { putLong("exerciseId", exerciseId) }
                )
            } else {
                Toast.makeText(requireContext(), "운동 가이드 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutFavorite.setOnClickListener {
            if (exerciseId <= 0) {
                Toast.makeText(requireContext(), "즐겨찾기할 운동 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            toggleFavoriteStatus()
        }
    }

    private fun loadInitialStates() {
        Log.d(TAG, "loadInitialStates: Fetching scheduleId and favorite status for planId=$planId, exerciseId=$exerciseId")
        if (planId <= 0 || exerciseId <= 0) {
            Log.e(TAG, "loadInitialStates: Invalid planId ($planId) or exerciseId ($exerciseId). Aborting.")
            if (isAdded) {
                Toast.makeText(requireContext(), "운동 정보를 불러올 수 없습니다 (ID 오류).", Toast.LENGTH_LONG).show()
            }
            disableInteractionButtons()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to fetch scheduleId for planId: $planId, exerciseId: $exerciseId")
                val scheduleIdResponse = RetrofitClient.scheduleApi.getScheduleId(planId, exerciseId)

                if (!scheduleIdResponse.isSuccessful || scheduleIdResponse.body() == null) {
                    val errorBodyString = try { scheduleIdResponse.errorBody()?.string() ?: "No error body" } catch (e: Exception) { "Error reading error body" }
                    val responseCode = scheduleIdResponse.code()
                    Log.e(TAG, "scheduleId 가져오기 API 실패: $responseCode - $errorBodyString")
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "운동 스케줄 정보 로드 실패 (코드: $responseCode)", Toast.LENGTH_LONG).show()
                            disableInteractionButtons()
                        }
                    }
                    return@launch
                }

                val scheduleIdResponseBody = scheduleIdResponse.body()!!
                val fetchedScheduleId = scheduleIdResponseBody.scheduleId
                Log.i(TAG, "Fetched scheduleId DTO: $scheduleIdResponseBody, scheduleId value from DTO: $fetchedScheduleId")

                if (fetchedScheduleId <= 0) {
                    Log.e(TAG, "Fetched scheduleId is invalid (<=0): $fetchedScheduleId. Parsed DTO: $scheduleIdResponseBody")
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "유효하지 않은 스케줄 ID 수신 ($fetchedScheduleId).", Toast.LENGTH_LONG).show()
                            disableInteractionButtons()
                        }
                    }
                    return@launch
                }
                currentScheduleIdForEdit = fetchedScheduleId
                Log.i(TAG, "Successfully fetched and validated scheduleId: $currentScheduleIdForEdit")

                Log.d(TAG, "Attempting to fetch exercise info for scheduleId: $currentScheduleIdForEdit")
                val exerciseInfoResponse = RetrofitClient.scheduleApi.getExerciseInfo(currentScheduleIdForEdit)
                if (!exerciseInfoResponse.isSuccessful || exerciseInfoResponse.body() == null) {
                    val errorBodyString = try { exerciseInfoResponse.errorBody()?.string() ?: "No error body" } catch (e: Exception) { "Error reading error body" }
                    val responseCode = exerciseInfoResponse.code()
                    Log.e(TAG, "운동 정보(즐겨찾기) 가져오기 API 실패: $responseCode - $errorBodyString for scheduleId: $currentScheduleIdForEdit")
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "운동 상세 정보 로드 실패 (코드: $responseCode)", Toast.LENGTH_LONG).show()
                        }
                    }
                    withContext(Dispatchers.Main) { if(isAdded) { updateFavoriteUI(); enableInteractionButtonsButFavorite(); } }
                    return@launch
                }

                val exerciseDto = exerciseInfoResponse.body()!!
                currentIsFavorite = exerciseDto.isFavorite
                if (exerciseDto.id != exerciseId) {
                    Log.w(TAG, "Mismatch: exerciseId from constructor ($exerciseId) vs exerciseId from getExerciseInfo DTO (${exerciseDto.id}) for scheduleId $currentScheduleIdForEdit")
                }
                Log.i(TAG, "Successfully fetched exercise info. isFavorite: $currentIsFavorite for exerciseId from DTO: ${exerciseDto.id}")

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        updateFavoriteUI()
                        enableInteractionButtons()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Log.e(TAG, "즐겨찾기 또는 스케줄 ID 상태 불러오기 중 예외 발생", e)
                        Toast.makeText(requireContext(), "초기 정보 로드 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
                        disableInteractionButtons()
                    }
                }
            }
        }
    }

    private fun toggleFavoriteStatus() {
        Log.d(TAG, "toggleFavoriteStatus: currentIsFavorite=$currentIsFavorite, for original exerciseId=$exerciseId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val newFavoriteState = !currentIsFavorite
                val requestBody = RetrofitClient.ToggleFavoriteRequest(newFavoriteState)
                val response = RetrofitClient.exerciseApi.toggleExerciseFavorite(exerciseId, requestBody)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        currentIsFavorite = serverResponse.isFavorite
                        updateFavoriteUI()
                        Toast.makeText(requireContext(), if (currentIsFavorite) "'${exerciseName}' 즐겨찾기 추가됨" else "'${exerciseName}' 즐겨찾기 해제됨", Toast.LENGTH_SHORT).show()
                        setFragmentResult(REQUEST_KEY_EXERCISE_EDIT, bundleOf(RESULT_KEY_UPDATE_NEEDED to true))
                        Log.i(TAG, "Favorite status updated to: $currentIsFavorite for exerciseId: $exerciseId")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류 (${response.code()})"
                        Log.e(TAG, "즐겨찾기 상태 변경 실패: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "즐겨찾기 상태 변경 실패: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Log.e(TAG, "즐겨찾기 상태 변경 중 예외 발생", e)
                        Toast.makeText(requireContext(), "즐겨찾기 상태 변경 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateFavoriteUI() {
        if (!isAdded || _binding == null) return
        binding.menuFavorite.text = if (currentIsFavorite) "즐겨찾기 해제" else "즐겨찾기"
        binding.iconFavorite.isSelected = currentIsFavorite
    }

    private fun disableInteractionButtons() {
        if (!isAdded || _binding == null) return
        binding.layoutSetEdit.isEnabled = false
        binding.layoutExerciseChange.isEnabled = false
        binding.layoutDeleteExercise.isEnabled = false
        binding.layoutFavorite.isEnabled = false
    }

    private fun enableInteractionButtons() {
        if (!isAdded || _binding == null) return
        binding.layoutSetEdit.isEnabled = true
        binding.layoutExerciseChange.isEnabled = true
        binding.layoutDeleteExercise.isEnabled = true
        binding.layoutFavorite.isEnabled = true
    }
    private fun enableInteractionButtonsButFavorite() {
        if (!isAdded || _binding == null) return
        binding.layoutSetEdit.isEnabled = true
        binding.layoutExerciseChange.isEnabled = true
        binding.layoutDeleteExercise.isEnabled = true
        binding.layoutFavorite.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}
