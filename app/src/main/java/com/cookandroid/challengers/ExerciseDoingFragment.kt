package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseDoingBinding
import com.cookandroid.challengers.databinding.ItemExerciseSetBinding
import com.cookandroid.challengers.network.dto.ScheduleDto
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.concurrent.TimeUnit
class ExerciseDoingFragment : Fragment() {

    private var _binding: FragmentExerciseDoingBinding? = null
    private val binding get() = _binding!!

    private lateinit var setAdapter: ExerciseSetAdapter
    private var currentSetIndex = 0 // 현재 진행 중이거나 다음에 진행할 세트의 인덱스 (0-based)
    private var exercisePlanId: Long = -1L // 현재 운동 계획의 서버 ID

    private var scheduleId: Long = -1L // 현재 화면에 표시된 운동의 서버 schedule_id
    private var currentExerciseId: Long = -1L // 현재 화면에 표시된 운동의 서버 exercise_id

    // planExerciseList는 오늘 해야 할 전체 운동 스케줄 목록
    private var currentExerciseOrderIndex = 0 // planExerciseList 내 현재 운동의 순서 (0-based)
    private var planExerciseList: List<ScheduleDto> = emptyList()

    private var currentSetStartTime: Long = 0L // 현재 '진행중인 세트'의 시작 시간 (ms)

    // Arguments에서 넘어온 운동 정보 (planExerciseList 로드 전 또는 실패 시 사용)
    private var passedExerciseName: String? = null
    private var passedImagePath: String? = null
    private var passedEquip: String? = null

    private var currentExerciseSets: MutableList<ExerciseSet> = mutableListOf() // 현재 운동의 세트 목록 (UI용)

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()
    private lateinit var dbForEnrich: AppDatabase // 운동 이름/이미지 등 로컬 정보 보강용

    companion object {
        private const val TAG = "ExerciseDoingFragment"

        // SharedPreferences 이름 정의
        private const val PREFS_NAME_INTERNAL = "exercise_doing_internal_prefs"
        private const val PREFS_NAME_SHARED_HEADER = "exercise_progress"

        // ExerciseDoingFragment 내부 상태 복원용 키
        private const val KEY_INTERNAL_EXERCISE_PLAN_ID = "internal_exercise_plan_id"
        private const val KEY_INTERNAL_CURRENT_EXERCISE_ORDER_INDEX = "internal_current_exercise_order_index"
        private const val KEY_INTERNAL_CURRENT_SET_INDEX = "internal_current_set_index"
        private const val KEY_INTERNAL_CURRENT_SET_START_TIME = "internal_current_set_start_time"
        private const val KEY_INTERNAL_SCHEDULE_ID = "internal_schedule_id"
        private const val KEY_INTERNAL_CURRENT_EXERCISE_ID = "internal_current_exercise_id"
        private const val KEY_INTERNAL_IS_WORKOUT_SESSION_ACTIVE = "internal_is_workout_session_active"

        // ExerciseFragment의 "진행 중 헤더" 와 공유하는 SharedPreferences 키
        private const val KEY_SHARED_PLAN_ID = "current_plan_id_prefs"
        private const val KEY_SHARED_EXERCISE_ORDER_INDEX = "current_exercise_index_prefs"
        private const val KEY_SHARED_SCHEDULE_ID = "current_schedule_id_prefs"
        private const val KEY_SHARED_EXERCISE_ID = "current_exercise_id_prefs"
        private const val KEY_SHARED_EXERCISE_NAME = "current_exercise_name_prefs"
        private const val KEY_SHARED_IMAGE_PATH = "current_image_path_prefs"
        private const val KEY_SHARED_EQUIP = "current_equip_prefs"
        private const val KEY_SHARED_ELAPSED_TIME = "stopwatch_elapsed_time_prefs" // 전체 운동 세션 경과 시간
        private const val KEY_SHARED_IS_IN_PROGRESS_HEADER = "is_in_progress"
    }

    private val internalPrefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME_INTERNAL, Context.MODE_PRIVATE)
    }
    private val sharedHeaderPrefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME_SHARED_HEADER, Context.MODE_PRIVATE)
    }

    private var isWorkoutSessionActive: Boolean = false // 이 화면에서 운동 세션이 활성화되었는지

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbForEnrich = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        Log.d(TAG, "onCreate CALLED")

        // 1. Restore from savedInstanceState if available (highest priority for config changes)
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring state from savedInstanceState")
            exercisePlanId = savedInstanceState.getLong(KEY_INTERNAL_EXERCISE_PLAN_ID, -1L)
            scheduleId = savedInstanceState.getLong(KEY_INTERNAL_SCHEDULE_ID, -1L)
            currentExerciseId = savedInstanceState.getLong(KEY_INTERNAL_CURRENT_EXERCISE_ID, -1L)
            currentExerciseOrderIndex = savedInstanceState.getInt(KEY_INTERNAL_CURRENT_EXERCISE_ORDER_INDEX, 0)
            currentSetIndex = savedInstanceState.getInt(KEY_INTERNAL_CURRENT_SET_INDEX, 0)
            currentSetStartTime = savedInstanceState.getLong(KEY_INTERNAL_CURRENT_SET_START_TIME, 0L)
            isWorkoutSessionActive = savedInstanceState.getBoolean(KEY_INTERNAL_IS_WORKOUT_SESSION_ACTIVE, false)
            // If restored from savedInstanceState, elapsedTime should already be in ViewModel due to onSaveInstanceState in ViewModel.
            // No need to explicitly set it from sharedHeaderPrefs here for config changes.
        } else {
            // 2. Initialize from arguments (passed from ExerciseFragment) and then sharedHeaderPrefs for session state
            Log.d(TAG, "Initializing state from arguments and/or SharedPreferences")
            arguments?.let {
                exercisePlanId = it.getLong("planId", sharedHeaderPrefs.getLong(KEY_SHARED_PLAN_ID, -1L)) // Use shared if arg missing
                scheduleId = it.getLong("scheduleId", sharedHeaderPrefs.getLong(KEY_SHARED_SCHEDULE_ID, -1L))
                currentExerciseId = it.getLong("exerciseId", sharedHeaderPrefs.getLong(KEY_SHARED_EXERCISE_ID, -1L))
                passedExerciseName = it.getString("exerciseName")
                passedImagePath = it.getString("imagePath")
                passedEquip = it.getString("equip")
                currentExerciseOrderIndex = it.getInt("initialExerciseIndex", sharedHeaderPrefs.getInt(KEY_SHARED_EXERCISE_ORDER_INDEX, 0))
            } ?: run { // Fallback if arguments are null (should ideally not happen if navigated correctly)
                Log.w(TAG, "Arguments are null, attempting to restore all from sharedHeaderPrefs")
                exercisePlanId = sharedHeaderPrefs.getLong(KEY_SHARED_PLAN_ID, -1L)
                scheduleId = sharedHeaderPrefs.getLong(KEY_SHARED_SCHEDULE_ID, -1L)
                currentExerciseId = sharedHeaderPrefs.getLong(KEY_SHARED_EXERCISE_ID, -1L)
                currentExerciseOrderIndex = sharedHeaderPrefs.getInt(KEY_SHARED_EXERCISE_ORDER_INDEX, 0)
                // passedXxx variables might be null here, UI should handle
            }

            // Determine initial isWorkoutSessionActive and stopwatch time based on sharedHeaderPrefs
            // This covers "continue from header" and "start new exercise" scenarios from ExerciseFragment
            isWorkoutSessionActive = sharedHeaderPrefs.getBoolean(KEY_SHARED_IS_IN_PROGRESS_HEADER, false)
            val initialTotalElapsedTime = sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)

            if (isWorkoutSessionActive) {
                // If continuing an active session, set the ViewModel's time to the last saved total elapsed time.
                stopwatchViewModel.setElapsedTime(initialTotalElapsedTime)
                // Restore set-specific state if available from internal prefs for this exact exercise
                // This ensures if user was on set 3, they resume at set 3
                if (scheduleId != -1L && scheduleId == internalPrefs.getLong(KEY_INTERNAL_SCHEDULE_ID, -2L)) {
                    currentSetIndex = internalPrefs.getInt(KEY_INTERNAL_CURRENT_SET_INDEX, 0)
                    currentSetStartTime = internalPrefs.getLong(KEY_INTERNAL_CURRENT_SET_START_TIME, 0L)
                } else { // Continuing a session, but maybe a different exercise or no specific internal state for this one
                    currentSetIndex = 0 // Default to first set if internal state doesn't match
                    currentSetStartTime = 0L
                }
                Log.i(TAG, "Session is ACTIVE. Initial total elapsed: $initialTotalElapsedTime. SetIndex: $currentSetIndex, SetStartTime: $currentSetStartTime")
            } else {
                // If not an active session (e.g., new exercise started from ExerciseFragment where isContinuing=false),
                // KEY_SHARED_ELAPSED_TIME should be 0, and StopwatchViewModel should be reset.
                stopwatchViewModel.stopStopwatch() // Resets time to 0 and stops.
                currentSetIndex = 0
                currentSetStartTime = 0L
                Log.i(TAG, "Session is NOT active (new exercise). Stopwatch reset. SetIndex: $currentSetIndex, SetStartTime: $currentSetStartTime")
            }
        }
        Log.i(TAG, "Final onCreate state: planId=$exercisePlanId, scheduleId=$scheduleId, exerciseId=$currentExerciseId, orderIndex=$currentExerciseOrderIndex, setIndex=$currentSetIndex, sessionActive=$isWorkoutSessionActive, elapsedVM=${stopwatchViewModel.elapsedTime.value}")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseDoingBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView CALLED")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated CALLED. Initial currentSetIndex: $currentSetIndex, isWorkoutSessionActive: $isWorkoutSessionActive")

        parentFragmentManager.setFragmentResultListener("sets_updated", viewLifecycleOwner) { _, _ ->
            Log.i(TAG, "Received fragment result 'sets_updated', refreshing sets.")
            fetchSetsForCurrentExercise()
        }

        setAdapter = ExerciseSetAdapter(
            getExerciseIsTimeType = {
                planExerciseList.getOrNull(currentExerciseOrderIndex)?.is_time_type ?: false
            }
        )
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setAdapter
        }

        setupStopwatch()
        setupListeners()

        loadTodayPlanAndSetupInitialExercise {
            Log.d(TAG, "loadTodayPlanAndSetupInitialExercise - onComplete triggered.")
            updateExerciseInfoUI()
            fetchSetsForCurrentExercise()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause CALLED")
        saveInternalStateToPrefs()
        saveGlobalProgressToHeaderPrefs()
        // ★ 스톱워치가 실행 중이었다면, 현재 '전체' 경과 시간을 KEY_SHARED_ELAPSED_TIME에 저장
        //    ViewModel의 스톱워치는 계속 실행될 수 있으므로, 화면 벗어날 때 현재 값 저장.
        if (isWorkoutSessionActive && stopwatchViewModel.isRunning.value == true) { // 세션이 활성이고 스톱워치가 돌고 있을 때만
            sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L).apply()
            Log.i(TAG,"onPause: Session active & Stopwatch was running. Saved total elapsed time to sharedHeaderPrefs: ${stopwatchViewModel.elapsedTime.value}")
        } else if (isWorkoutSessionActive && stopwatchViewModel.isRunning.value == false) {
            // 세션은 활성인데 스톱워치가 멈춰있다면 (예: 사용자가 일시정지) 그 시간도 저장
            sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L).apply()
            Log.i(TAG,"onPause: Session active & Stopwatch was PAUSED. Saved total elapsed time to sharedHeaderPrefs: ${stopwatchViewModel.elapsedTime.value}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState CALLED")
        outState.putLong(KEY_INTERNAL_EXERCISE_PLAN_ID, exercisePlanId)
        outState.putLong(KEY_INTERNAL_SCHEDULE_ID, scheduleId)
        outState.putLong(KEY_INTERNAL_CURRENT_EXERCISE_ID, currentExerciseId)
        outState.putInt(KEY_INTERNAL_CURRENT_EXERCISE_ORDER_INDEX, currentExerciseOrderIndex)
        outState.putInt(KEY_INTERNAL_CURRENT_SET_INDEX, currentSetIndex)
        outState.putBoolean(KEY_INTERNAL_IS_WORKOUT_SESSION_ACTIVE, isWorkoutSessionActive)
        outState.putLong(KEY_INTERNAL_CURRENT_SET_START_TIME, currentSetStartTime)
    }

    private fun saveInternalStateToPrefs() {
        if (!isAdded) return
        internalPrefs.edit().apply {
            putLong(KEY_INTERNAL_EXERCISE_PLAN_ID, exercisePlanId)
            putLong(KEY_INTERNAL_SCHEDULE_ID, scheduleId)
            putLong(KEY_INTERNAL_CURRENT_EXERCISE_ID, currentExerciseId)
            putInt(KEY_INTERNAL_CURRENT_EXERCISE_ORDER_INDEX, currentExerciseOrderIndex)
            putInt(KEY_INTERNAL_CURRENT_SET_INDEX, currentSetIndex)
            putBoolean(KEY_INTERNAL_IS_WORKOUT_SESSION_ACTIVE, isWorkoutSessionActive)
            putLong(KEY_INTERNAL_CURRENT_SET_START_TIME, currentSetStartTime)
            apply()
        }
        Log.i(TAG, "Internal state saved: planId=$exercisePlanId, scheduleId=$scheduleId, exerciseId=$currentExerciseId, orderIndex=$currentExerciseOrderIndex, setIndex=$currentSetIndex, sessionActive=$isWorkoutSessionActive, setStartTime=$currentSetStartTime")
    }

    private fun saveGlobalProgressToHeaderPrefs() {
        if (!isAdded) return

        val currentScheduleForHeader = planExerciseList.getOrNull(currentExerciseOrderIndex)
        val exerciseNameForHeader = currentScheduleForHeader?.exercise_name ?: passedExerciseName ?: ""
        val imagePathForHeader = currentScheduleForHeader?.image_path ?: passedImagePath
        val equipForHeader = currentScheduleForHeader?.equip ?: passedEquip

        // 헤더 표시 조건: 이 화면에서 운동 세션이 활성화 & 현재 운동이 완료되지 않았을 때
        val shouldShowHeader = isWorkoutSessionActive && (currentScheduleForHeader?.is_completed == false)

        val editor = sharedHeaderPrefs.edit()
        editor.putBoolean(KEY_SHARED_IS_IN_PROGRESS_HEADER, shouldShowHeader)

        if (shouldShowHeader && exercisePlanId != -1L && scheduleId != -1L && currentExerciseId != -1L) {
            editor.putLong(KEY_SHARED_PLAN_ID, exercisePlanId)
            editor.putLong(KEY_SHARED_SCHEDULE_ID, scheduleId)
            editor.putLong(KEY_SHARED_EXERCISE_ID, currentExerciseId)
            editor.putInt(KEY_SHARED_EXERCISE_ORDER_INDEX, currentExerciseOrderIndex)
            editor.putString(KEY_SHARED_EXERCISE_NAME, exerciseNameForHeader)
            editor.putString(KEY_SHARED_IMAGE_PATH, imagePathForHeader)
            editor.putString(KEY_SHARED_EQUIP, equipForHeader)
            // KEY_SHARED_ELAPSED_TIME은 스톱워치 pause/stop 시점 또는 onPause에서 업데이트됨.
            // 여기서는 현재 ViewModel 값을 한번 더 반영할 수 있으나, 잦은 업데이트는 피하는 것이 좋음.
            // editor.putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L))
            Log.i(TAG, "Global progress for header SAVED (or updated): showHeader=$shouldShowHeader, scheduleId=$scheduleId, name=$exerciseNameForHeader")
        } else {
            if (!shouldShowHeader) Log.i(TAG, "Global progress for header: showHeader is FALSE. Only is_in_progress_header=false saved.")
            else Log.w(TAG, "Global progress for header: Valid IDs missing or exercise completed. Not saving detailed info for header.")
        }
        editor.apply()
    }

    private fun setupStopwatch() {
        Log.d(TAG, "setupStopwatch CALLED")
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
        }
        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.pauseButton.setImageResource(
                if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black
            )
            // 스톱워치가 멈췄고(사용자 또는 로직에 의해), 운동 세션이 활성 상태라면
            // 현재 누적된 전체 시간을 SharedPreferences에 저장 (헤더 및 이어하기용)
            if (!running && isWorkoutSessionActive) {
                sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L).apply()
                Log.d(TAG,"Stopwatch is NOT running (paused/stopped by user/logic), but session was active. Saved total elapsed time to sharedHeaderPrefs: ${stopwatchViewModel.elapsedTime.value}")
            }
            // 스톱워치 실행 상태가 변경될 때마다 헤더 정보 업데이트
            saveGlobalProgressToHeaderPrefs()
        }
        // 초기 시간 설정은 onCreate에서 수행됨. 여기서는 Observer만 설정.
        Log.d(TAG, "setupStopwatch: Observers set.")
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners CALLED")
        binding.backButton.setOnClickListener {
            saveInternalStateToPrefs()
            saveGlobalProgressToHeaderPrefs()
            findNavController().popBackStack()
        }

        binding.pauseButton.setOnClickListener {
            if (planExerciseList.isEmpty() || (currentExerciseSets.isEmpty() && currentSetIndex == 0) || (currentSetIndex >= currentExerciseSets.size && currentExerciseSets.isNotEmpty()) ) {
                Toast.makeText(requireContext(), "진행할 운동 또는 세트가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (stopwatchViewModel.isRunning.value == true) {
                stopwatchViewModel.pauseStopwatch() // 전체 운동 시간 일시 정지
                // 멈췄을 때의 '전체' 경과 시간을 KEY_SHARED_ELAPSED_TIME에 저장 (헤더 및 이어하기용)
                sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L).apply()
                Log.i(TAG,"Stopwatch paused by button. Total elapsed time saved: ${stopwatchViewModel.elapsedTime.value}")
            } else { // 스톱워치가 멈춰있을 때 (수동으로 전체 운동 시간 이어하기)
                // 현재 세트가 미완료 상태일 때만 전체 시간 이어하기 가능
                if (currentExerciseSets.getOrNull(currentSetIndex)?.isCompleted == false) {
                    // ViewModel에 이미 설정된 시간 (onCreate 또는 이전 pause에서 설정된) 부터 이어 시작
                    val resumeTime = stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)
                    stopwatchViewModel.startStopwatch(resumeTime)
                    if (!isWorkoutSessionActive) isWorkoutSessionActive = true // 스톱워치 시작 = 세션 활성
                    Log.i(TAG,"Stopwatch manually started/resumed by button. Resumed from total elapsed: $resumeTime.")
                } else {
                    Toast.makeText(requireContext(), "이미 완료된 세트이거나 진행할 세트가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.completeSetButton.setOnClickListener {
            if (currentSetIndex < currentExerciseSets.size) { // 완료할 세트가 남아있다면
                if (!isWorkoutSessionActive) isWorkoutSessionActive = true // 세트 완료 시도 = 세션 활성

                if (stopwatchViewModel.isRunning.value == false) { // 전체 스톱워치가 멈춰있으면 현재 시간 기준으로 시작/재개
                    val resumeTime = stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)
                    stopwatchViewModel.startStopwatch(resumeTime)
                }
                // 현재 '세트'의 시작 시간 기록 (이 세트가 처음 시작되는 경우)
                if (currentSetStartTime == 0L) {
                    currentSetStartTime = System.currentTimeMillis()
                    internalPrefs.edit().putLong(KEY_INTERNAL_CURRENT_SET_START_TIME, currentSetStartTime).apply()
                    Log.d(TAG,"CompleteSetButton: Set $currentSetIndex started. SetStartTime: $currentSetStartTime")
                }
                Log.d(TAG, "CompleteSetButton clicked for set $currentSetIndex. CurrentSetStartTime: $currentSetStartTime. TotalElapsed: ${stopwatchViewModel.elapsedTime.value}")
                completeCurrentSet()
            } else { // 모든 세트 완료 (currentSetIndex == currentExerciseSets.size)
                Log.d(TAG, "CompleteSetButton clicked when all sets are done. Moving to rest/next exercise.")
                Toast.makeText(requireContext(), "모든 세트를 완료했습니다.", Toast.LENGTH_SHORT).show()
                showRestTimer(autoStart = true) // 다음 운동으로 진행 유도
            }
        }
        binding.restTimerButton.setOnClickListener { showRestTimer() }
        binding.addSetButton.setOnClickListener { addNewSet() }
        binding.editSetButton.setOnClickListener { showEditSetBottomSheet() }
    }

    private fun loadTodayPlanAndSetupInitialExercise(onComplete: () -> Unit) {
        Log.d(TAG, "loadTodayPlanAndSetupInitialExercise CALLED")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userPref = com.cookandroid.challengers.util.UserPreference(requireContext())
                val userId = userPref.getUserId()
                if (userId == -1) {
                    Log.e(TAG, "Invalid userId (-1), aborting plan load.")
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_LONG).show(); findNavController().popBackStack() }
                    return@launch
                }

                val response = RetrofitClient.scheduleApi.getTodayPlan(userId)
                if (response.isSuccessful) {
                    val todayPlanData = response.body()
                    if (todayPlanData != null && todayPlanData.schedules.isNotEmpty()) {
                        exercisePlanId = todayPlanData.plan.id.toLong()
                        val validSchedulesFromServer = todayPlanData.schedules.filter { it.exercise_id != 0 }

                        planExerciseList = validSchedulesFromServer.map { scheduleDto ->
                            val localExercise = dbForEnrich.exerciseDao().getExerciseById(scheduleDto.exercise_id.toLong())
                            scheduleDto.copy(
                                exercise_name = scheduleDto.exercise_name.ifBlank { localExercise?.name ?: "운동 이름 없음" },
                                image_path = if (scheduleDto.image_path.isNullOrBlank()) localExercise?.imagePath else scheduleDto.image_path,
                                equip = scheduleDto.equip.ifBlank { localExercise?.equip ?: "정보 없음" },
                                part = scheduleDto.part.ifBlank { localExercise?.part ?: "부위 없음" }
                            )
                        }.sortedBy { it.exercise_order }
                        Log.i(TAG, "Loaded and processed planExerciseList: ${planExerciseList.size} items.")

                        if (planExerciseList.isNotEmpty()) {
                            currentExerciseOrderIndex = currentExerciseOrderIndex.coerceIn(0, planExerciseList.size - 1)
                            planExerciseList.getOrNull(currentExerciseOrderIndex)?.let { currentScheduleDto ->
                                scheduleId = currentScheduleDto.schedule_id.toLong()
                                currentExerciseId = currentScheduleDto.exercise_id.toLong()
                                passedExerciseName = currentScheduleDto.exercise_name
                                passedImagePath = currentScheduleDto.image_path
                                passedEquip = currentScheduleDto.equip
                            } ?: run {
                                scheduleId = -1L; currentExerciseId = -1L;
                                Log.e(TAG, "Could not set scheduleId/exerciseId from orderIndex $currentExerciseOrderIndex after plan load")
                            }
                        } else {
                            scheduleId = -1L; currentExerciseId = -1L; currentExerciseOrderIndex = 0;
                            Log.w(TAG, "Plan loaded but schedule list is empty. IDs reset.")
                        }
                        Log.i(TAG, "Final initial setup after plan load: planId=$exercisePlanId, scheduleId=$scheduleId, exerciseId=$currentExerciseId, orderIndex=$currentExerciseOrderIndex")
                        withContext(Dispatchers.Main) { onComplete() }
                    } else {
                        Log.w(TAG, "Today's plan data is null or schedules list is empty.")
                        planExerciseList = emptyList()
                        scheduleId = -1L; currentExerciseId = -1L; currentExerciseOrderIndex = 0;
                        withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "오늘 진행할 운동이 없습니다.", Toast.LENGTH_SHORT).show(); onComplete() }
                    }
                } else {
                    val errorCode = response.code(); val errorMsg = response.message()
                    Log.e(TAG, "Failed to load today's plan: $errorCode - $errorMsg")
                    planExerciseList = emptyList(); scheduleId = -1L; currentExerciseId = -1L; currentExerciseOrderIndex = 0;
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "운동 계획 로드 실패 (코드: $errorCode)", Toast.LENGTH_LONG).show(); onComplete() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading today's plan", e)
                planExerciseList = emptyList(); scheduleId = -1L; currentExerciseId = -1L; currentExerciseOrderIndex = 0;
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "데이터 로드 중 오류: ${e.message}", Toast.LENGTH_LONG).show(); onComplete() }
            }
        }
    }

    private fun fetchSetsForCurrentExercise() {
        if (scheduleId == -1L) {
            Log.e(TAG, "fetchSets: Invalid scheduleId (-1L). Cannot load sets.")
            if (::setAdapter.isInitialized) setAdapter.submitList(emptyList())
            currentExerciseSets.clear(); updateSetProgressUI(); binding.completeSetButton.isEnabled = false
            if (isWorkoutSessionActive) {
                stopwatchViewModel.pauseStopwatch()
                Log.w(TAG, "fetchSets: Invalid scheduleId, session was active. Pausing stopwatch.")
            } else {
                stopwatchViewModel.stopStopwatch()
                Log.d(TAG, "fetchSets: Invalid scheduleId, session not active. Stopping and resetting stopwatch.")
            }
            saveGlobalProgressToHeaderPrefs()
            return
        }

        val currentScheduleInfo = planExerciseList.getOrNull(currentExerciseOrderIndex)
        if (currentScheduleInfo == null || currentScheduleInfo.schedule_id.toLong() != scheduleId) {
            Log.e(TAG, "fetchSets: Mismatch or no current schedule info. orderIndex: $currentExerciseOrderIndex, current scheduleId: $scheduleId, expected from planList: ${currentScheduleInfo?.schedule_id}")
            if (::setAdapter.isInitialized) setAdapter.submitList(emptyList())
            currentExerciseSets.clear(); updateSetProgressUI(); binding.completeSetButton.isEnabled = false
            if (isWorkoutSessionActive) stopwatchViewModel.pauseStopwatch() else stopwatchViewModel.stopStopwatch()
            saveGlobalProgressToHeaderPrefs()
            return
        }

        val exerciseIsTimeType = currentScheduleInfo.is_time_type
        val exerciseIdForSetsApi = currentScheduleInfo.exercise_id.toLong()
        Log.d(TAG, "Fetching sets for: scheduleId=$scheduleId, exerciseIdForSetsApi=$exerciseIdForSetsApi, isTimeType=$exerciseIsTimeType")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedExerciseSetsSource: List<ExerciseSet> = if (exerciseIsTimeType) {
                    val response = RetrofitClient.scheduleApi.getTimeSets(scheduleId).execute()
                    Log.d(TAG, "getTimeSets API for $scheduleId: code=${response.code()}, success=${response.isSuccessful}")
                    if (response.isSuccessful) {
                        response.body()?.map { dto ->
                            ExerciseSet(exercisePlanId = exercisePlanId, exerciseId = exerciseIdForSetsApi, setNumber = dto.setNumber, weight = dto.weight.toInt(), reps = 0, times = dto.seconds.toLong(), isCompleted = dto.isCompleted, elapsedTimeMillis = dto.seconds * 1000L)
                        } ?: emptyList()
                    } else { Log.e(TAG, "Failed to fetch time sets: ${response.code()} ${response.message()}"); emptyList() }
                } else {
                    val response = RetrofitClient.scheduleApi.getRepsSets(scheduleId).execute()
                    Log.d(TAG, "getRepsSets API for $scheduleId: code=${response.code()}, success=${response.isSuccessful}, body: ${response.body()?.joinToString { "S${it.setNumber}(${it.reps}r ${it.weight}kg done:${it.isCompleted})" }}")
                    if (response.isSuccessful) {
                        response.body()?.map { dto ->
                            ExerciseSet(exercisePlanId = exercisePlanId, exerciseId = exerciseIdForSetsApi, setNumber = dto.setNumber, weight = dto.weight.toInt(), reps = dto.reps, times = 0L, isCompleted = dto.isCompleted)
                        } ?: emptyList()
                    } else { Log.e(TAG, "Failed to fetch reps sets: ${response.code()} ${response.message()}"); emptyList() }
                }

                currentExerciseSets = fetchedExerciseSetsSource.sortedBy { it.setNumber }.toMutableList()
                Log.i(TAG, "Fetched ${currentExerciseSets.size} sets for '${currentScheduleInfo.exercise_name}'. Data: ${currentExerciseSets.joinToString { "Set${it.setNumber}(done:${it.isCompleted})" }}")

                var determinedNextSetIndex = currentExerciseSets.indexOfFirst { !(it.isCompleted ?: false) }
                if (determinedNextSetIndex == -1) {
                    determinedNextSetIndex = if (currentExerciseSets.isNotEmpty()) currentExerciseSets.size else 0
                }
                currentSetIndex = determinedNextSetIndex
                Log.i(TAG, "Determined currentSetIndex = $currentSetIndex (0-based; .size if all done).")

                val listForAdapter = currentExerciseSets.mapIndexed { index, set ->
                    set.copy(isHighlighted = (index == currentSetIndex && index < currentExerciseSets.size))
                }

                withContext(Dispatchers.Main) {
                    _binding?.let {
                        if (::setAdapter.isInitialized) {
                            setAdapter.submitList(listForAdapter) {
                                if (listForAdapter.isNotEmpty() && currentSetIndex < currentExerciseSets.size) {
                                    binding.setsRecyclerView.smoothScrollToPosition(currentSetIndex)
                                }
                            }
                        }
                        updateSetProgressUI()
                        binding.completeSetButton.isEnabled = currentExerciseSets.isNotEmpty() && currentSetIndex < currentExerciseSets.size

                        val overallExerciseIsMarkedCompleted = planExerciseList.getOrNull(currentExerciseOrderIndex)?.is_completed ?: false
                        val currentSetToStartNow = currentExerciseSets.getOrNull(currentSetIndex)

                        // ★★★ 스톱워치 자동 시작/상태 관리 로직 (사용자 요구사항 반영) ★★★
                        if (isWorkoutSessionActive && // 현재 운동 세션이 활성화 상태이고 (예: 이어하기)
                            !overallExerciseIsMarkedCompleted && // 전체 운동이 완료되지 않았고
                            currentSetToStartNow?.isCompleted == false) { // 현재 시작할 세트도 미완료 상태라면

                            // ViewModel에 이미 설정된 시간 (onCreate에서 sharedPrefs 값으로 설정됨) 부터 이어 시작
                            if (stopwatchViewModel.isRunning.value == false) {
                                stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
                                Log.i(TAG, "fetchSets: Session active & conditions met. Stopwatch WAS STOPPED, now STARTED from ViewModel time ${stopwatchViewModel.elapsedTime.value}.")
                            } else {
                                Log.i(TAG, "fetchSets: Session active & conditions met. Stopwatch ALREADY RUNNING. Total elapsed ${stopwatchViewModel.elapsedTime.value}.")
                            }
                        } else if (currentSetIndex >= currentExerciseSets.size && currentExerciseSets.isNotEmpty()) {
                            // 현재 운동의 모든 세트가 완료된 상태로 로드됨
                            binding.completeSetButton.text = "운동 완료"
                            if (isWorkoutSessionActive && stopwatchViewModel.isRunning.value == true) {
                                stopwatchViewModel.pauseStopwatch() // 전체 세션은 계속될 수 있으므로 일시정지
                                Log.i(TAG, "fetchSets: All sets for this exercise loaded as completed. Pausing session stopwatch. Total elapsed: ${stopwatchViewModel.elapsedTime.value}")
                            }
                        } else if (!isWorkoutSessionActive) {
                            // 새 운동 세션이거나, 이어하기 상태가 아니면 스톱워치는 사용자가 수동으로 시작해야 함.
                            // 이 경우, 전체 운동 시간은 0부터 시작해야 하므로 stopStopwatch() 호출.
                            // (ExerciseFragment에서 navigateToExerciseDoingFragment(isContinuing=false) 시 이미 처리됨, onCreate에서도 처리)
                            if (stopwatchViewModel.elapsedTime.value != 0L || stopwatchViewModel.isRunning.value == true) {
                                Log.d(TAG, "fetchSets: Workout session NOT active but stopwatch not zero/stopped. Calling stopStopwatch() to ensure reset.")
                                stopwatchViewModel.stopStopwatch() // 시간 0으로 리셋하고 멈춤
                            } else {
                                Log.d(TAG, "fetchSets: Workout session NOT active. Stopwatch already stopped/reset.")
                            }
                        }
                        // ★★★ 스톱워치 로직 끝 ★★★
                        saveInternalStateToPrefs()
                        saveGlobalProgressToHeaderPrefs()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sets for scheduleId $scheduleId: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _binding?.let {
                        Toast.makeText(requireContext(), "세트 정보를 가져오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        if (::setAdapter.isInitialized) setAdapter.submitList(emptyList())
                        currentExerciseSets.clear(); updateSetProgressUI()
                    }
                }
            }
        }
    }

    private fun updateExerciseInfoUI() {
        if (!isAdded || _binding == null) return
        Log.d(TAG, "updateExerciseInfoUI CALLED for orderIndex: $currentExerciseOrderIndex")

        val currentScheduleDtoToDisplay = planExerciseList.getOrNull(currentExerciseOrderIndex)
        if (currentScheduleDtoToDisplay == null || scheduleId == -1L) {
            binding.exerciseNameTextView.text = passedExerciseName ?: "운동 정보 없음"
            binding.titleTextView.text = passedExerciseName ?: "운동 로딩 중..."
            val imagePath = passedImagePath ?: ""
            val resId = if (imagePath.isNotBlank()) try { resources.getIdentifier(imagePath, "drawable", requireContext().packageName).takeIf { it != 0 } } catch (_:Exception) {null} else null
            Glide.with(requireContext()).load(resId ?: R.drawable.ic_launcher_background).into(binding.exerciseImageView)
            if (::setAdapter.isInitialized) setAdapter.updateEquipAndType(passedEquip, false)
            Log.w(TAG, "updateExerciseInfoUI: currentScheduleDto is null or scheduleId invalid. Using passed arguments.")
        } else {
            binding.exerciseNameTextView.text = currentScheduleDtoToDisplay.exercise_name
            binding.titleTextView.text = currentScheduleDtoToDisplay.exercise_name
            val imagePath = currentScheduleDtoToDisplay.image_path ?: ""
            val resId = if (imagePath.isNotBlank()) try { resources.getIdentifier(imagePath, "drawable", requireContext().packageName).takeIf { it != 0 } } catch (_:Exception) {null} else null
            Glide.with(requireContext()).load(resId ?: R.drawable.ic_launcher_background).transition(DrawableTransitionOptions.withCrossFade()).error(R.drawable.ic_launcher_background).into(binding.exerciseImageView)
            if (::setAdapter.isInitialized) setAdapter.updateEquipAndType(currentScheduleDtoToDisplay.equip, currentScheduleDtoToDisplay.is_time_type)
        }
        updateProgressText()
    }

    private fun completeCurrentSet() {
        if (currentSetIndex >= currentExerciseSets.size) {
            Log.w(TAG, "All sets already marked as completed. CurrentSetIndex: $currentSetIndex, SetSize: ${currentExerciseSets.size}")
            showRestTimer(autoStart = true)
            return
        }

        val now = System.currentTimeMillis()
        val elapsedForThisSpecificSet = if (currentSetStartTime > 0L) now - currentSetStartTime else 0L
        if (currentSetStartTime == 0L) {
            Log.w(TAG, "completeCurrentSet: currentSetStartTime was 0. Elapsed time for this set recorded as 0 or based on total if time-based.")
        }
        Log.i(TAG, "Completing set index $currentSetIndex. Elapsed for this specific set: $elapsedForThisSpecificSet ms.")

        val setToComplete = currentExerciseSets[currentSetIndex]
        val completedSetInLocal = setToComplete.copy(
            isCompleted = true,
            isHighlighted = false,
            elapsedTimeMillis = if (planExerciseList.getOrNull(currentExerciseOrderIndex)?.is_time_type == true) elapsedForThisSpecificSet else 0L
        )
        currentExerciseSets[currentSetIndex] = completedSetInLocal

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = RetrofitClient.SetCompletionRequest(scheduleId, completedSetInLocal.setNumber, true)
                Log.d(TAG, "🚀 Sending set completion to server: scheduleId=${requestBody.scheduleId}, setNumber=${requestBody.setNumber}, isCompleted=true")
                val updateResponse = RetrofitClient.scheduleApi.updateRepsSetCompletion(requestBody).execute()

                if (updateResponse.isSuccessful) {
                    Log.i(TAG, "✅ Server ACK for set completion: scheduleId=${scheduleId}, setNumber=${setToComplete.setNumber}")
                } else {
                    Log.e(TAG, "❌ Server FAILED to ACK set completion: ${updateResponse.code()} - ${updateResponse.message()}. Body: ${updateResponse.errorBody()?.string()}")
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "세트 완료 저장 실패(서버)", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during server set completion for scheduleId $scheduleId, set ${setToComplete.setNumber}", e)
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "세트 완료 저장 중 네트워크 오류", Toast.LENGTH_SHORT).show() }
            }
        }

        currentSetIndex++

        val listForAdapter = currentExerciseSets.mapIndexed { idx, s ->
            s.copy(isHighlighted = (idx == currentSetIndex && currentSetIndex < currentExerciseSets.size))
        }
        if (::setAdapter.isInitialized) { setAdapter.submitList(listForAdapter.toList()) {
            if (currentSetIndex < currentExerciseSets.size) binding.setsRecyclerView.smoothScrollToPosition(currentSetIndex)
        }}
//        currentSetStartTime = 0L // ★ 다음 세트 시작 전이므로 현재 세트 시작 시간 초기화
//        stopwatchViewModel.pauseStopwatch() // ★ 전체 스톱워치는 '일시 정지' (휴식 시작)
        // 현재까지 누적된 '전체' 운동 시간을 SharedPreferences에 저장 (헤더 및 이어하기용)
        sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L).apply()
        Log.d(TAG, "Set completed. Stopwatch paused. Total elapsed time saved: ${stopwatchViewModel.elapsedTime.value}")

        saveInternalStateToPrefs()
        saveGlobalProgressToHeaderPrefs()
        updateSetProgressUI()

        showRestTimer(autoStart = true)
    }

    private fun addNewSet() {
        if (scheduleId == -1L) { Toast.makeText(requireContext(), "현재 운동 정보가 없어 세트를 추가할 수 없습니다.", Toast.LENGTH_SHORT).show(); return }
        val currentScheduleInfo = planExerciseList.getOrNull(currentExerciseOrderIndex) ?: return
        val exerciseIsTimeType = currentScheduleInfo.is_time_type

        lifecycleScope.launch(Dispatchers.IO) {
            val nextSetNumber = (currentExerciseSets.maxByOrNull { it.setNumber }?.setNumber ?: 0) + 1
            val newSetDefaultReps = 12; val newSetDefaultSeconds = 60

            val newSetForApiDto = if (exerciseIsTimeType) RetrofitClient.TimeSetDto(setNumber = nextSetNumber, seconds = newSetDefaultSeconds, weight = 0f, isCompleted = false)
            else RetrofitClient.RepsSetDto(setNumber = nextSetNumber, reps = newSetDefaultReps, weight = 0f, isCompleted = false)

            val existingSetsForApi = currentExerciseSets.map { es ->
                if (exerciseIsTimeType) RetrofitClient.TimeSetDto(es.setNumber, (es.times ?: newSetDefaultSeconds.toLong()).toInt(), es.weight?.toFloat() ?: 0f, es.isCompleted ?: false)
                else RetrofitClient.RepsSetDto(es.setNumber, es.reps ?: newSetDefaultReps, es.weight?.toFloat() ?: 0f, es.isCompleted ?: false)
            }.toMutableList()

            if (newSetForApiDto is RetrofitClient.TimeSetDto) existingSetsForApi.add(newSetForApiDto)
            else if (newSetForApiDto is RetrofitClient.RepsSetDto) existingSetsForApi.add(newSetForApiDto)

            try {
                val response: Response<Void> = if (exerciseIsTimeType) RetrofitClient.scheduleApi.updateTimeSets(scheduleId, existingSetsForApi.filterIsInstance<RetrofitClient.TimeSetDto>()).execute()
                else RetrofitClient.scheduleApi.updateRepsSets(scheduleId, existingSetsForApi.filterIsInstance<RetrofitClient.RepsSetDto>()).execute()

                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully added new set and updated server. Refreshing sets.")
                    withContext(Dispatchers.Main) { fetchSetsForCurrentExercise(); Toast.makeText(requireContext(), "${nextSetNumber}세트 추가됨", Toast.LENGTH_SHORT).show() }
                } else {
                    Log.e(TAG, "Failed to update server after adding new set: ${response.code()} - ${response.message()}")
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "세트 추가 실패 (서버 반영 오류).", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while updating server after adding new set.", e)
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "세트 추가 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showEditSetBottomSheet() {
        if (scheduleId == -1L ) { Toast.makeText(requireContext(), "편집할 운동 정보가 없습니다.", Toast.LENGTH_SHORT).show(); return }
        val isTimeType = planExerciseList.getOrNull(currentExerciseOrderIndex)?.is_time_type ?: false
        val sheet: BottomSheetDialogFragment = if (isTimeType) TimeSetEditDialogFragment.newInstance(scheduleId) else RepsSetEditDialogFragment.newInstance(scheduleId)
        sheet.show(parentFragmentManager, if(isTimeType) TimeSetEditDialogFragment.TAG else RepsSetEditDialogFragment.TAG)
    }

    private fun updateProgressText() {
        if (!isAdded || _binding == null) return
        val currentDisplayOrder = if (planExerciseList.isNotEmpty()) currentExerciseOrderIndex + 1 else 0
        val totalExercises = planExerciseList.size
        binding.exerciseProgressTextView.text = "$currentDisplayOrder/$totalExercises"
        Log.d(TAG, "Updated progress text: $currentDisplayOrder/$totalExercises")
    }

    private fun updateSetProgressUI() {
        if (!isAdded || _binding == null) return
        val completedSetsCount = currentExerciseSets.count { it.isCompleted == true }
        val totalSetsCount = currentExerciseSets.size
        Log.i(TAG, "Set progress UI updated: $completedSetsCount / $totalSetsCount. CurrentSetIndex to do: $currentSetIndex")
        binding.completeSetButton.text = if (currentSetIndex < totalSetsCount) "세트 완료" else "운동 완료"
        binding.completeSetButton.isEnabled = totalSetsCount > 0
    }

    private fun showRestTimer(autoStart: Boolean = false) {
        if (!isAdded || _binding == null) return

        val allSetsDoneForCurrentExercise = currentSetIndex >= currentExerciseSets.size && currentExerciseSets.isNotEmpty()
        Log.i(TAG, "showRestTimer: currentSetIndex=$currentSetIndex, totalSets=${currentExerciseSets.size}, allSetsDoneForCurrentExercise=$allSetsDoneForCurrentExercise")

        val sheet = RestTimerFragment.newInstance(autoStart)
        sheet.setOnTimerFinishedListener {
            if (!isAdded || _binding == null) return@setOnTimerFinishedListener

            if (!allSetsDoneForCurrentExercise) { // 현재 운동의 다음 세트가 남아있다면
                Log.i(TAG, "Rest timer finished. Preparing for next set (index $currentSetIndex).")
                // ★ 다음 세트 시작: 전체 스톱워치는 계속 이어가도록 시작 (멈춰있었다면)
                if (stopwatchViewModel.isRunning.value == false) {
                    // 이전 전체 누적 시간부터 이어가도록 시작
                    val timeToResumeFrom = stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)
                    stopwatchViewModel.startStopwatch(timeToResumeFrom)
                    Log.d(TAG, "Next set ($currentSetIndex) starting. Stopwatch resumed from $timeToResumeFrom.")
                }
                currentSetStartTime = System.currentTimeMillis() // ★ 다음 세트의 시작 시간 새로 기록
                internalPrefs.edit().putLong(KEY_INTERNAL_CURRENT_SET_START_TIME, currentSetStartTime).apply()
                Log.d(TAG, "New SetStartTime: $currentSetStartTime. Stopwatch running: ${stopwatchViewModel.isRunning.value}")

                val listForAdapter = currentExerciseSets.mapIndexed { idx, s ->
                    s.copy(isHighlighted = (idx == currentSetIndex && currentSetIndex < currentExerciseSets.size))
                }
                if (::setAdapter.isInitialized) { setAdapter.submitList(listForAdapter.toList()) {
                    if (currentSetIndex < currentExerciseSets.size) binding.setsRecyclerView.smoothScrollToPosition(currentSetIndex)
                }}
                updateSetProgressUI()
                saveInternalStateToPrefs()
                saveGlobalProgressToHeaderPrefs()
            } else { // 현재 운동의 모든 세트를 완료한 경우 -> 다음 운동으로
                Log.i(TAG, "Rest timer finished. All sets for current exercise are done. Moving to completeCurrentExercise.")
                completeCurrentExercise()
            }
        }
        try { sheet.show(childFragmentManager, RestTimerFragment.TAG) }
        catch (e: IllegalStateException) { Log.e(TAG, "Error showing RestTimerFragment: ${e.message}") }
    }


    private fun completeCurrentExercise() {
        if (!isAdded) { Log.w(TAG, "completeCurrentExercise: Fragment not added, aborting."); return }
        val completedScheduleIdServer = this.scheduleId
        if (completedScheduleIdServer == -1L) {
            Log.e(TAG, "completeCurrentExercise: Invalid scheduleId (-1L). Cannot complete.")
            moveToNextExerciseOrFinish()
            return
        }
        Log.i(TAG, "completeCurrentExercise CALLED for schedule ID: $completedScheduleIdServer")
        if (!isWorkoutSessionActive) isWorkoutSessionActive = true

        lifecycleScope.launch(Dispatchers.IO) {
            var serverUpdateSuccess = false
            try {
                val completeResponse = RetrofitClient.scheduleApi.markScheduleAsComplete(completedScheduleIdServer)
                if (completeResponse.isSuccessful) {
                    Log.i(TAG, "✅ Server ACK for EXERCISE (scheduleId: $completedScheduleIdServer) completion")
                    serverUpdateSuccess = true
                    val indexToUpdate = planExerciseList.indexOfFirst { it.schedule_id.toLong() == completedScheduleIdServer }
                    if (indexToUpdate != -1) {
                        val oldScheduleDto = planExerciseList[indexToUpdate]
                        if (oldScheduleDto.is_completed != true) {
                            val updatedScheduleDto = oldScheduleDto.copy(is_completed = true)
                            val mutablePlanList = planExerciseList.toMutableList()
                            mutablePlanList[indexToUpdate] = updatedScheduleDto
                            planExerciseList = mutablePlanList.toList()
                            Log.d(TAG, "Locally updated planExerciseList for scheduleId $completedScheduleIdServer, is_completed set to true")
                        } else {
                            Log.d(TAG, "Local planExerciseList for scheduleId $completedScheduleIdServer already marked as completed.")
                        }
                    } else {
                        Log.w(TAG, "Could not find scheduleId $completedScheduleIdServer in planExerciseList to update is_completed locally.")
                    }
                } else {
                    Log.e(TAG, "❌ Server FAILED to ACK EXERCISE completion: ${completeResponse.code()} - ${completeResponse.message()}. Error body: ${completeResponse.errorBody()?.string()}")
                    withContext(Dispatchers.Main) { if (isAdded) Toast.makeText(requireContext(), "운동 완료 상태 저장 실패(서버)", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during server EXERCISE completion for scheduleId $completedScheduleIdServer", e)
                withContext(Dispatchers.Main) { if (isAdded) Toast.makeText(requireContext(), "운동 완료 저장 중 네트워크 오류", Toast.LENGTH_SHORT).show() }
            } finally {
                withContext(Dispatchers.Main) { if (isAdded) moveToNextExerciseOrFinish() }
            }
        }
    }

    private fun moveToNextExerciseOrFinish() {
        if (!isAdded || _binding == null) return
        Log.d(TAG, "moveToNextExerciseOrFinish CALLED. Current orderIndex: $currentExerciseOrderIndex, Plan size: ${planExerciseList.size}")

        parentFragmentManager.setFragmentResult("sets_updated", Bundle.EMPTY)

        val nextExerciseGlobalOrderIndex = currentExerciseOrderIndex + 1

        if (nextExerciseGlobalOrderIndex < planExerciseList.size) { // 다음 운동이 있는 경우
            val nextScheduleDto = planExerciseList[nextExerciseGlobalOrderIndex]
            Log.i(TAG, "Moving to next exercise: ${nextScheduleDto.exercise_name} (orderIndex: $nextExerciseGlobalOrderIndex)")

            currentExerciseOrderIndex = nextExerciseGlobalOrderIndex
            scheduleId = nextScheduleDto.schedule_id.toLong()
            currentExerciseId = nextScheduleDto.exercise_id.toLong()
            passedExerciseName = nextScheduleDto.exercise_name
            passedImagePath = nextScheduleDto.image_path
            passedEquip = nextScheduleDto.equip
            currentSetIndex = 0
            currentSetStartTime = 0L // 새 운동의 첫 세트 시작 시간은 0으로 초기화
            currentExerciseSets.clear()
            // isWorkoutSessionActive는 true 유지 (전체 운동 세션은 계속)

            // ★ 스톱워치는 리셋하지 않고 계속 진행 ★
            // 스톱워치가 일시정지 상태였다면 (이전 운동의 마지막 세트 완료 후), 다음 운동 시작을 위해 다시 시작.
            if (stopwatchViewModel.isRunning.value == false && isWorkoutSessionActive) {
                val timeToResumeFrom = stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)
                stopwatchViewModel.startStopwatch(timeToResumeFrom)
                Log.i(TAG, "Stopwatch resumed for next exercise. Total elapsed: $timeToResumeFrom")
            } else {
                Log.i(TAG, "Stopwatch continues for next exercise. Current total elapsed: ${stopwatchViewModel.elapsedTime.value}")
            }

            updateExerciseInfoUI()
            fetchSetsForCurrentExercise() // 새 운동의 세트 목록 로드 (이 안에서 SharedPreferences 저장)

        } else { // ★ 모든 운동을 완료한 경우 ★
            Log.i(TAG, "All exercises in plan completed. Cleaning up and preparing for photo upload.")
            isWorkoutSessionActive = false // 운동 세션 종료

            val completionTimestamp = System.currentTimeMillis()
            val totalWorkoutDuration = stopwatchViewModel.elapsedTime.value ?: 0L // 최종 전체 운동 시간

            // ★ 모든 운동 완료 시 스톱워치 정지 및 시간 0으로 리셋 ★
            stopwatchViewModel.stopStopwatch() // 시간 기록 후 정지 및 시간 0으로 리셋
            Log.i(TAG, "All exercises finished. Final total workout time: ${StopwatchViewModel().formatElapsedTime(totalWorkoutDuration)}. Stopwatch reset.") // ViewModel 인스턴스화 주의

            // SharedPreferences 정리 (헤더용)
            // KEY_SHARED_ELAPSED_TIME 은 ChallengeUploadPhotoFragment로 전달하기 위해 유지.
            sharedHeaderPrefs.edit()
                .putBoolean(KEY_SHARED_IS_IN_PROGRESS_HEADER, false) // 헤더 숨김
                .putLong(KEY_SHARED_ELAPSED_TIME, totalWorkoutDuration) // 최종 운동 시간 저장 (전달용)
                .remove(KEY_SHARED_EXERCISE_ORDER_INDEX)
                .remove(KEY_SHARED_SCHEDULE_ID)
                .remove(KEY_SHARED_EXERCISE_ID)
                .remove(KEY_SHARED_PLAN_ID)
                .remove(KEY_SHARED_EXERCISE_NAME)
                .remove(KEY_SHARED_IMAGE_PATH)
                .remove(KEY_SHARED_EQUIP)
                .apply()
            internalPrefs.edit().clear().apply() // 이 프래그먼트 내부 상태도 초기화

            // ChallengeUploadPhotoFragment로 이동
            try {
                if (findNavController().currentDestination?.id == R.id.exerciseDoingFragment) {
                    val args = Bundle().apply {
                        putLong("completion_time_millis", completionTimestamp)
                        putLong("total_duration_millis", totalWorkoutDuration)
                    }
                    val actionId = R.id.action_exerciseDoing_to_challengeUpload // 실제 네비게이션 그래프의 액션 ID로 변경!
                    findNavController().navigate(actionId, args)
                    Log.i(TAG, "Navigating to ChallengeUploadPhotoFragment with completionTime: $completionTimestamp, duration: $totalWorkoutDuration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to ChallengeUploadPhotoFragment failed: ${e.message}", e)
                Toast.makeText(requireContext(), "사진 인증 화면으로 이동 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView CALLED.")
        val elapsedTimeOnDestroy = stopwatchViewModel.elapsedTime.value ?: sharedHeaderPrefs.getLong(KEY_SHARED_ELAPSED_TIME, 0L)
        if (isWorkoutSessionActive) {
            sharedHeaderPrefs.edit().putLong(KEY_SHARED_ELAPSED_TIME, elapsedTimeOnDestroy).apply()
            Log.i(TAG,"onDestroyView: Saved total elapsed time to sharedHeaderPrefs: $elapsedTimeOnDestroy")
        }
        _binding = null
    }

    private class ExerciseSetAdapter(
        private val onSetClick: ((ExerciseSet) -> Unit)? = null,
        private val getExerciseIsTimeType: () -> Boolean
    ) : ListAdapter<ExerciseSet, ExerciseSetAdapter.ViewHolder>(object : DiffUtil.ItemCallback<ExerciseSet>() {
        override fun areItemsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
            return oldItem.exerciseId == newItem.exerciseId && oldItem.setNumber == newItem.setNumber
        }
        override fun areContentsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
            return oldItem == newItem
        }
    }) {
        private var currentEquipAdapter: String? = null

        fun updateEquipAndType(equip: String?, isTimeTypeIgnoredForNow: Boolean) {
            val newEquipTrimmed = equip?.trim()
            if (currentEquipAdapter != newEquipTrimmed) {
                currentEquipAdapter = newEquipTrimmed
                notifyDataSetChanged()
            }
        }

        inner class ViewHolder(val binding: ItemExerciseSetBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onSetClick?.invoke(getItem(pos))
                    }
                }
            }

            fun bind(set: ExerciseSet) {
                binding.setNumberTextView.text = "${set.setNumber}세트"
                val isTimeTypeExercise = getExerciseIsTimeType()

                if (isTimeTypeExercise) {
                    val totalSeconds = set.times ?: 0L
                    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                    val secondsValue = totalSeconds % 60
                    binding.repsTextView.text =
                        String.format("%02d:%02d:%02d", hours, minutes, secondsValue)
                } else {
                    binding.repsTextView.text = "${set.reps ?: "-"}회"
                }

                val layoutParams = binding.repsTextView.layoutParams as ViewGroup.MarginLayoutParams
                if (currentEquipAdapter.isNullOrBlank() || currentEquipAdapter in listOf(
                        "맨몸",
                        "스텝박스",
                        "세라밴드",
                        "짐볼"
                    )
                ) {
                    binding.weightTextView.visibility = View.GONE
                    binding.dividerImageView.visibility = View.GONE
                    layoutParams.marginEnd =
                        itemView.context.resources.getDimensionPixelSize(R.dimen.item_reps_margin_end_no_weight)
                } else {
                    binding.weightTextView.visibility = View.VISIBLE
                    binding.dividerImageView.visibility = View.VISIBLE
                    binding.weightTextView.text =
                        set.weight?.let { if (it > 0) "${it}kg" else "-" } ?: "-"
                    layoutParams.marginEnd =
                        itemView.context.resources.getDimensionPixelSize(R.dimen.item_reps_margin_end_default)
                }
                binding.repsTextView.layoutParams = layoutParams

                binding.completionCheckImageView.alpha = if (set.isCompleted == true) 1.0f else 0.2f
                binding.root.background = ContextCompat.getDrawable(
                    binding.root.context,
                    when {
                        set.isCompleted == true -> R.drawable.set_item_background_completed
                        set.isHighlighted == true -> R.drawable.set_item_background_emphasized
                        else -> R.drawable.set_item_background
                    }
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemExerciseSetBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }}