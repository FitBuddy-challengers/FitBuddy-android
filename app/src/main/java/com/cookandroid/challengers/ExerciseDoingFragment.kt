package com.cookandroid.challengers

import android.app.Dialog
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
import com.cookandroid.challengers.data.db.AppDatabase // 로컬 DB (이미지 보완용)
import com.cookandroid.challengers.databinding.FragmentExerciseDoingBinding
import com.cookandroid.challengers.databinding.ItemExerciseSetBinding
import com.cookandroid.challengers.network.dto.ScheduleDto // 서버 응답 DTO
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson // 오류 메시지 파싱용
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response // Retrofit Response
import java.util.concurrent.TimeUnit

class ExerciseDoingFragment : Fragment() {

    private var _binding: FragmentExerciseDoingBinding? = null
    private val binding get() = _binding!!

    private lateinit var setAdapter: ExerciseSetAdapter
    private var currentSetIndex = 0
    private var exercisePlanId: Long = -1L // 현재 운동 계획의 ID

    private var scheduleId: Long = -1L // 현재 진행 중인 스케줄의 서버 ID
    private var currentExerciseId: Long = -1L // 현재 진행 중인 운동의 서버 ID

    private var currentExerciseIndex = 0 // planExerciseList 내 현재 아이템의 인덱스
    private var planExerciseList: List<ScheduleDto> = emptyList() // 서버에서 받은 ScheduleDto 리스트
    private var setStartTime: Long = 0L

    // Arguments에서 넘어온 초기값 또는 첫 운동 정보 표시용
    private var passedExerciseName: String? = null
    private var passedImagePath: String? = null
    private var passedEquip: String? = null

    private var currentExerciseSets: MutableList<ExerciseSet> = mutableListOf() // 현재 운동의 세트 목록

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()
    private lateinit var dbForEnrich: AppDatabase // 이미지 경로 등 보완용


    companion object {
        private const val PREFS_PROGRESS = "exercise_progress"
        private const val KEY_PLAN_ID = "current_plan_id"
        private const val KEY_EXERCISE_INDEX = "current_exercise_index"
        private const val KEY_SET_INDEX = "current_set_index"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_IN_PROGRESS = "is_in_progress"
        private const val KEY_SCHEDULE_ID = "current_schedule_id"
        private const val KEY_EXERCISE_ID = "current_exercise_id"
        private const val KEY_SAVED_PLAN_ID = "current_plan_id_prefs"
        private const val KEY_SAVED_EXERCISE_INDEX = "current_exercise_index_prefs"
        private const val KEY_SAVED_SCHEDULE_ID = "current_schedule_id_prefs"
        private const val KEY_SAVED_EXERCISE_ID = "current_exercise_id_prefs"
        private const val KEY_SAVED_EXERCISE_NAME = "current_exercise_name_prefs"
        private const val KEY_SAVED_IMAGE_PATH = "current_image_path_prefs"
        private const val KEY_SAVED_EQUIP = "current_equip_prefs"
        private const val KEY_ELAPSED_TIME = "stopwatch_elapsed_time_prefs"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
    }

    private var isInProgress: Boolean
        get() = prefs.getBoolean(KEY_IN_PROGRESS, false)
        set(v) = prefs.edit().putBoolean(KEY_IN_PROGRESS, v).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbForEnrich = AppDatabase.getDatabase(requireContext(), lifecycleScope)

        arguments?.let {
            exercisePlanId = it.getLong("planId", prefs.getLong(KEY_PLAN_ID, -1L))
            scheduleId = it.getLong("scheduleId", prefs.getLong(KEY_SCHEDULE_ID, -1L))
            currentExerciseId = it.getLong("exerciseId", prefs.getLong(KEY_EXERCISE_ID, -1L))
            passedExerciseName = it.getString("exerciseName")
            passedImagePath = it.getString("imagePath")
            passedEquip = it.getString("equip")
            currentExerciseIndex = it.getInt("initialExerciseIndex", prefs.getInt(KEY_EXERCISE_INDEX, 0))
        } ?: run {
            exercisePlanId = prefs.getLong(KEY_PLAN_ID, -1L)
            scheduleId = prefs.getLong(KEY_SCHEDULE_ID, -1L)
            currentExerciseId = prefs.getLong(KEY_EXERCISE_ID, -1L)
            currentExerciseIndex = prefs.getInt(KEY_EXERCISE_INDEX, 0)
        }
        Log.d("ExerciseDoingFragment", "onCreate - Args/Prefs: planId: $exercisePlanId, scheduleId: $scheduleId, exerciseId: $currentExerciseId, exerciseIndex: $currentExerciseIndex")

        if (savedInstanceState != null) {
            currentExerciseIndex = savedInstanceState.getInt(KEY_EXERCISE_INDEX, currentExerciseIndex)
            currentSetIndex = savedInstanceState.getInt(KEY_SET_INDEX, 0)
            setStartTime = savedInstanceState.getLong(KEY_START_TIME, System.currentTimeMillis())
            isInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS, false)
            scheduleId = savedInstanceState.getLong(KEY_SCHEDULE_ID, scheduleId)
            currentExerciseId = savedInstanceState.getLong(KEY_EXERCISE_ID, currentExerciseId)
            exercisePlanId = savedInstanceState.getLong(KEY_PLAN_ID, exercisePlanId)
            Log.d("ExerciseDoingFragment", "Restored from savedInstanceState")
        } else if (arguments?.containsKey("initialExerciseIndex") == null && isInProgress) {
            currentSetIndex = prefs.getInt(KEY_SET_INDEX, 0)
            setStartTime = prefs.getLong(KEY_START_TIME, 0L)
            Log.d("ExerciseDoingFragment", "Restored from prefs for ongoing exercise")
        } else if (arguments?.containsKey("initialExerciseIndex") == true) {
            currentSetIndex = 0
            setStartTime = System.currentTimeMillis()
            isInProgress = true
            Log.d("ExerciseDoingFragment", "New exercise started via NavArgs")
        }

        if (isInProgress && setStartTime == 0L && currentSetIndex >= 0) {
            setStartTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_START_TIME, setStartTime).apply()
            Log.w("ExerciseDoingFragment", "Corrected setStartTime to current time")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDoingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResultListener("sets_updated", viewLifecycleOwner) { _, _ ->
            fetchSetsForCurrentExercise()
        }

        setAdapter = ExerciseSetAdapter(
            getExerciseIsTimeType = {
                planExerciseList.getOrNull(currentExerciseIndex)?.is_time_type ?: false
            }
        )
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setAdapter
        }

        loadTodayPlanAndSetupInitialExercise {
            updateExerciseInfoUI()
            fetchSetsForCurrentExercise()
            val currentSchedule = planExerciseList.getOrNull(currentExerciseIndex)
            if (isInProgress && currentSchedule != null && !(currentSchedule.is_completed) && currentSetIndex < currentExerciseSets.size) {
                if (!(stopwatchViewModel.isRunning.value ?: false)) {
                    val elapsedTimeSinceSetStart = if (setStartTime > 0L) System.currentTimeMillis() - setStartTime else 0L
                    stopwatchViewModel.startStopwatch(elapsedTimeSinceSetStart)
                }
            } else if (!isInProgress && currentSetIndex == 0 && (stopwatchViewModel.elapsedTime.value ?: 0L) == 0L && planExerciseList.isNotEmpty()) {
                // 새 운동 시작 시는 사용자가 플레이 버튼을 눌러 시작하도록 유도
            } else if ((currentSchedule?.is_completed == true) || (currentSetIndex >= currentExerciseSets.size && currentExerciseSets.isNotEmpty())) {
                stopwatchViewModel.stopStopwatch()
                isInProgress = false
            }
            saveProgressToPrefs()
        }

        setupStopwatch()
        setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_SAVED_PLAN_ID, exercisePlanId)
        outState.putLong(KEY_SAVED_SCHEDULE_ID, scheduleId)
        outState.putLong(KEY_SAVED_EXERCISE_ID, currentExerciseId)
        outState.putInt(KEY_SAVED_EXERCISE_INDEX, currentExerciseIndex)
        outState.putInt(KEY_SET_INDEX, currentSetIndex)
        outState.putBoolean(KEY_IN_PROGRESS, isInProgress)
        outState.putLong(KEY_START_TIME, setStartTime)
    }

    private fun saveProgressToPrefs() {
        if (exercisePlanId == -1L && scheduleId == -1L && currentExerciseId == -1L && !isInProgress) {
            prefs.edit().clear().apply()
            Log.d("ExerciseDoingFragment", "No valid progress to save, prefs cleared.")
            return
        }
        val currentSchedule = planExerciseList.getOrNull(currentExerciseIndex)
        val currentImagePath = currentSchedule?.image_path ?: passedImagePath // 현재 표시되는 이미지 경로
        val currentEquip = currentSchedule?.equip ?: passedEquip
        val currentExerciseName = currentSchedule?.exercise_name ?: passedExerciseName


        prefs.edit()
            .putLong(KEY_SAVED_PLAN_ID, exercisePlanId)
            .putLong(KEY_SAVED_SCHEDULE_ID, scheduleId)
            .putLong(KEY_SAVED_EXERCISE_ID, currentExerciseId)
            .putInt(KEY_SAVED_EXERCISE_INDEX, currentExerciseIndex)
            .putInt(KEY_SET_INDEX, currentSetIndex)
            .putBoolean(KEY_IN_PROGRESS, isInProgress)
            .putLong(KEY_START_TIME, setStartTime)
            .putString(KEY_SAVED_EXERCISE_NAME, currentExerciseName)
            .putString(KEY_SAVED_IMAGE_PATH, currentImagePath)
            .putString(KEY_SAVED_EQUIP, currentEquip)
            .putLong(KEY_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L)
            .apply()
        Log.d("ExerciseDoingFragment", "Progress saved to prefs: planId=$exercisePlanId, scheduleId=$scheduleId, exerciseId=$currentExerciseId, exerciseIndex=$currentExerciseIndex, setIndex=$currentSetIndex, startTime=$setStartTime, isInProgress=$isInProgress, imagePath=$currentImagePath")
    }

    private fun setupStopwatch() {
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
            // 운동이 시작되었음을 감지 (예: 스톱워치가 0보다 크고, 아직 isInProgress가 false일 때)
            if (time > 0L && !isInProgress && planExerciseList.isNotEmpty()) { // 운동 목록이 있을 때만 진행 중으로 간주
                isInProgress = true
                // KEY_PLAN_ID는 onCreate에서 이미 설정되었을 수 있음. 여기서 다시 저장할 필요는 없음.
                prefs.edit()
                    .putBoolean(KEY_IN_PROGRESS, true)
                    .apply()
                Log.d(
                    "ExerciseDoingFragment",
                    "setupStopwatch: Detected exercise start via stopwatch. isInProgress set to true."
                )
            }
        }
        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.pauseButton.setImageResource(
                if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black
            )
        }

        // 스톱워치 초기 상태 설정
        if (isInProgress) {
            if (!stopwatchViewModel.isRunning.value!!) { // 진행 중인데 스톱워치가 멈춰있으면
                // 이전 elapsedTime 값으로 시작 (앱 재시작 등)
                // ExerciseDoingFragment가 다시 활성화될 때 setStartTime을 기준으로 elapsedTime을 복원하는 로직이
                // StopwatchViewModel에 있다면 더 좋을 수 있음. 여기서는 간단히 현재 값으로 시작.
                val previousElapsedTime = stopwatchViewModel.elapsedTime.value ?: 0L
                // 만약 setStartTime과 현재 시간으로 계산된 경과시간이 있다면 그것을 사용하는 것이 더 정확할 수 있음.
                // 지금은 ViewModel의 현재 값으로 시작.
                stopwatchViewModel.startStopwatch(previousElapsedTime)
                Log.d(
                    "ExerciseDoingFragment",
                    "setupStopwatch: Restored stopwatch for in-progress exercise from previous time: $previousElapsedTime"
                )
            } else {
                Log.d(
                    "ExerciseDoingFragment",
                    "setupStopwatch: Stopwatch already running for in-progress exercise."
                )
            }
        } else { // 진행 중이 아닐 때 (예: 새 운동 시작)
            stopwatchViewModel.stopStopwatch() // 확실히 0으로 리셋하고 시작
            if (planExerciseList.isNotEmpty()) { // 운동할 목록이 있을 때만 시작
                stopwatchViewModel.startStopwatch()
                Log.d(
                    "ExerciseDoingFragment",
                    "setupStopwatch: New exercise session, starting stopwatch from 0."
                )
            } else {
                Log.d(
                    "ExerciseDoingFragment",
                    "setupStopwatch: No exercises in plan, stopwatch not started."
                )
            }
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            // (선택적) 운동 진행 중 뒤로가기 시 사용자 확인 로직 추가 가능
            findNavController().popBackStack()
        }
        binding.pauseButton.setOnClickListener {
            if (stopwatchViewModel.isRunning.value == true) {
                stopwatchViewModel.pauseStopwatch()
            } else {
                stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
            }
        }
        binding.completeSetButton.setOnClickListener {
            if (currentSetIndex < currentExerciseSets.size) {
                if (!isInProgress) {
                    val baseTime = stopwatchViewModel.elapsedTime.value ?: 0L
                    stopwatchViewModel.startStopwatch(baseTime)
                    setStartTime = System.currentTimeMillis() - baseTime
                    isInProgress = true
                    saveProgressToPrefs()
                }
                completeCurrentSet()
            } else {
                Toast.makeText(requireContext(), "모든 세트를 완료했습니다. 휴식 후 다음 운동으로 진행됩니다.", Toast.LENGTH_SHORT).show()
                // ★ 수정: 모든 세트 완료 시에도 휴식 타이머 호출
                showRestTimer(autoStart = true)
            }
        }
        binding.restTimerButton.setOnClickListener { showRestTimer() }
        binding.addSetButton.setOnClickListener { addNewSet() }
        binding.editSetButton.setOnClickListener { showEditSetBottomSheet() }
    }


    private fun loadTodayPlanAndSetupInitialExercise(onComplete: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = 21
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId)
                if (response.isSuccessful) {
                    val todayPlanData = response.body()
                    if (todayPlanData != null && todayPlanData.schedules.isNotEmpty()) {
                        exercisePlanId = todayPlanData.plan.id.toLong()
                        val schedulesFromServer = todayPlanData.schedules.filter { it.exercise_id != 0 }

                        planExerciseList = schedulesFromServer.map { scheduleDto ->
                            val localExerciseImage = dbForEnrich.exerciseDao().getExerciseById(scheduleDto.exercise_id.toLong())?.imagePath
                            Log.d("ExerciseDoingFragment", "🧪 mapping exercise_id: ${scheduleDto.exercise_id} for schedule_id: ${scheduleDto.schedule_id}, 서버 image_path: ${scheduleDto.image_path}, 로컬 imagePath: $localExerciseImage")
                            scheduleDto.copy(
                                image_path = if (scheduleDto.image_path.isNullOrBlank()) localExerciseImage else scheduleDto.image_path
                            )
                        }.sortedBy { it.exercise_order }

                        Log.d("ExerciseDoingFragment", "로드 및 변환된 planExerciseList (ScheduleDto): ${planExerciseList.size}개")

                        val initialScheduleIdFromArgs = arguments?.getLong("scheduleId", prefs.getLong(KEY_SCHEDULE_ID, -1L)) ?: -1L


                        if (planExerciseList.isNotEmpty()) {
                            var determinedStartIndex = 0
                            if (initialScheduleIdFromArgs != -1L) {
                                determinedStartIndex = planExerciseList.indexOfFirst { it.schedule_id.toLong() == initialScheduleIdFromArgs }
                                if (determinedStartIndex == -1) {
                                    Log.w("ExerciseDoingFragment", "전달받은 scheduleId $initialScheduleIdFromArgs 를 planExerciseList에서 찾지 못함. Args의 index 사용 또는 첫번째.")
                                    determinedStartIndex = arguments?.getInt("initialExerciseIndex", prefs.getInt(KEY_EXERCISE_INDEX, 0)) ?: prefs.getInt(KEY_EXERCISE_INDEX, 0)
                                    if (determinedStartIndex >= planExerciseList.size || determinedStartIndex < 0) determinedStartIndex = 0 // 최종 범위 보정
                                }
                            } else {
                                val savedExerciseIndex = prefs.getInt(KEY_EXERCISE_INDEX, 0)
                                determinedStartIndex = if (savedExerciseIndex < planExerciseList.size && savedExerciseIndex >= 0) savedExerciseIndex else 0
                            }
                            currentExerciseIndex = if (determinedStartIndex < planExerciseList.size && determinedStartIndex >=0) determinedStartIndex else 0
                        } else {
                            currentExerciseIndex = 0
                        }

                        planExerciseList.getOrNull(currentExerciseIndex)?.let { currentScheduleDto ->
                            scheduleId = currentScheduleDto.schedule_id.toLong()
                            currentExerciseId = currentScheduleDto.exercise_id.toLong()
                        }
                        Log.d("ExerciseDoingFragment", "최종 결정된 currentExerciseIndex: $currentExerciseIndex, scheduleId: ${scheduleId}, exerciseId: ${currentExerciseId}")

                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            onComplete()
                        }
                    } else {
                        Log.e("ExerciseDoingFragment", "오늘의 운동 계획 데이터가 비어있습니다 (schedules is empty or null).")
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Toast.makeText(requireContext(), "오늘 진행할 운동이 없습니다.", Toast.LENGTH_SHORT).show()
                            planExerciseList = emptyList()
                            onComplete()
                        }
                    }
                } else {
                    val errorCode = response.code()
                    var toastMessage = "오늘의 운동 계획 로드에 실패했습니다. (코드: $errorCode)"
                    if (errorCode == 404) {
                        try {
                            val errorBody = response.errorBody()?.string()
                            if (!errorBody.isNullOrBlank()) {
                                val gson = Gson()
                                val errorResponse = gson.fromJson(errorBody, RetrofitClient.ServerResponse::class.java)
                                toastMessage = errorResponse.message ?: "오늘 진행할 운동 계획이 없습니다."
                            } else {
                                toastMessage = "오늘 진행할 운동 계획이 없습니다."
                            }
                        } catch (e: Exception) {
                            Log.e("ExerciseDoingFragment", "404 오류 본문 파싱 실패", e)
                        }
                    } else {
                        Log.e("ExerciseDoingFragment", "오늘의 운동 계획 로드 실패: $errorCode - ${response.message()}")
                    }
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
                        planExerciseList = emptyList()
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseDoingFragment", "오늘의 운동 계획 로드 중 예외 발생", e)
                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    Toast.makeText(requireContext(), "데이터 로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                    planExerciseList = emptyList()
                    onComplete()
                }
            }
        }
    }

    private fun fetchSetsForCurrentExercise() {
        if (scheduleId == -1L) {
            Log.e("ExerciseDoingFragment", "fetchSets: 유효하지 않은 scheduleId ($scheduleId).")
            if(::setAdapter.isInitialized) setAdapter.submitList(emptyList())
            currentExerciseSets.clear()
            updateSetProgressUI()
            return
        }

        val currentSchedule = planExerciseList.getOrNull(currentExerciseIndex)
        if (currentSchedule == null) {
            Log.e("ExerciseDoingFragment", "fetchSets: 현재 운동 스케줄 정보 없음 (index: $currentExerciseIndex). planExerciseList size: ${planExerciseList.size}")
            if(::setAdapter.isInitialized) setAdapter.submitList(emptyList())
            currentExerciseSets.clear()
            updateSetProgressUI()
            return
        }
        val exerciseIsTimeType = currentSchedule.is_time_type
        val exerciseIdForSets = currentSchedule.exercise_id.toLong()

        Log.d("ExerciseDoingFragment", "Fetching sets for current scheduleId: $scheduleId, exerciseIdForSets: $exerciseIdForSets, isTimeType: $exerciseIsTimeType")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedSets: List<ExerciseSet>
                Log.d("ExerciseDoingFragment", "Calling API for sets: scheduleId=$scheduleId, isTimeType=$exerciseIsTimeType")
                if (exerciseIsTimeType) {
                    val responseCall = RetrofitClient.scheduleApi.getTimeSets(scheduleId)
                    val response = responseCall.execute()
                    Log.d("ExerciseDoingFragment", "getTimeSets API response for scheduleId $scheduleId: code=${response.code()}, successful=${response.isSuccessful}, message=${response.message()}, body_size=${response.body()?.size}")
                    if (response.body() != null) Log.d("ExerciseDoingFragment", "getTimeSets API body: ${response.body()}")

                    if (response.isSuccessful) {
                        fetchedSets = response.body()?.map { dto -> // dto는 TimeSetDto
                            ExerciseSet(
                                id = 0L, // 서버에서 세트 ID를 내려준다면 해당 ID 사용, 여기서는 임시로 0L
                                exercisePlanId = exercisePlanId,
                                exerciseId = exerciseIdForSets,
                                setNumber = dto.setNumber,
                                weight = dto.weight.toInt(),
                                reps = 0, // 시간 기반이므로 reps는 0 또는 null
                                times = dto.seconds.toLong(),
                                isCompleted = dto.isCompleted ?: false, // ★ TimeSetDto의 isCompleted 사용
                                elapsedTimeMillis = dto.seconds * 1000L // 서버가 초 단위로 주면 밀리초로 변환
                            )
                        } ?: emptyList()
                    } else {
                        Log.e("ExerciseDoingFragment", "시간 세트 로드 실패: ${response.code()} - ${response.message()}")
                        fetchedSets = emptyList()
                    }
                } else {
                    val responseCall = RetrofitClient.scheduleApi.getRepsSets(scheduleId)
                    val response = responseCall.execute()
                    Log.d("ExerciseDoingFragment", "getRepsSets API response for scheduleId $scheduleId: code=${response.code()}, successful=${response.isSuccessful}, message=${response.message()}, body_size=${response.body()?.size}")
                    if (response.body() != null) Log.d("ExerciseDoingFragment", "getRepsSets API body: ${response.body()}")

                    if (response.isSuccessful) {
                        fetchedSets = response.body()?.map { dto -> // dto는 RepsSetDto
                            ExerciseSet(
                                id = 0L, // 서버에서 세트 ID를 내려준다면 해당 ID 사용
                                exercisePlanId = exercisePlanId,
                                exerciseId = exerciseIdForSets,
                                setNumber = dto.setNumber,
                                weight = dto.weight.toInt(),
                                reps = dto.reps,
                                times = 0L, // 횟수 기반이므로 times는 0 또는 null
                                isCompleted = dto.isCompleted ?: false,// RepsSetDto의 isCompleted 필드 사용
                            )
                        } ?: emptyList()
                    } else {
                        Log.e("ExerciseDoingFragment", "횟수 세트 로드 실패: ${response.code()} - ${response.message()}")
                        fetchedSets = emptyList()
                    }
                }

                currentExerciseSets = fetchedSets.sortedBy { it.setNumber }.toMutableList()
                Log.d("ExerciseDoingFragment", "Fetched ${currentExerciseSets.size} sets for exercise ${currentSchedule.exercise_name}")

                val startIndexToHighlight = if (isInProgress && currentSetIndex < currentExerciseSets.size && currentSetIndex >= 0) {
                    currentSetIndex
                } else {
                    currentExerciseSets.indexOfFirst { !(it.isCompleted ?: false) }.takeIf { it != -1 } ?: 0
                }
                currentSetIndex = if (startIndexToHighlight < currentExerciseSets.size) startIndexToHighlight else 0

                val highlightedList = currentExerciseSets.mapIndexed { index, set ->
                    set.copy(isHighlighted = (index == currentSetIndex))
                }

                withContext(Dispatchers.Main) {
                    _binding?.let {
                        if(::setAdapter.isInitialized) setAdapter.submitList(highlightedList) {
                            if (highlightedList.isNotEmpty() && currentSetIndex < highlightedList.size) {
                                it.setsRecyclerView.scrollToPosition(currentSetIndex)
                            }
                        }
                        updateSetProgressUI()
                        if (isInProgress && setStartTime == 0L && highlightedList.getOrNull(currentSetIndex)?.isCompleted == false) {
                            setStartTime = System.currentTimeMillis()
                            saveProgressToPrefs()
                            Log.d("ExerciseDoingFragment", "Set started/restored, new setStartTime: $setStartTime for set $currentSetIndex")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ExerciseDoingFragment", "세트 정보 로드 중 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _binding?.let {
                        Toast.makeText(requireContext(), "세트 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        if(::setAdapter.isInitialized) setAdapter.submitList(emptyList())
                        currentExerciseSets.clear()
                        updateSetProgressUI()
                    }
                }
            }
        }
    }
    private fun updateExerciseInfoUI() {
        if (!isAdded || _binding == null) return

        val currentScheduleDto = planExerciseList.getOrNull(currentExerciseIndex)
        if (currentScheduleDto == null) {
            binding.exerciseNameTextView.text = passedExerciseName ?: "운동 정보 없음"
            val imagePathToLoad = passedImagePath
            val resId = if (!imagePathToLoad.isNullOrBlank()) {
                try { resources.getIdentifier(imagePathToLoad, "drawable", requireContext().packageName).takeIf { it != 0 } } catch (e: Exception) { null }
            } else { null }
            Glide.with(requireContext())
                .load(resId ?: R.drawable.ic_launcher_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_launcher_background)
                .into(binding.exerciseImageView)
            if(::setAdapter.isInitialized) setAdapter.updateEquipAndType(passedEquip, false)
            binding.titleTextView.text = passedExerciseName ?: "운동 중"
            updateProgressText()
            return
        }

        binding.exerciseNameTextView.text = currentScheduleDto.exercise_name
        binding.titleTextView.text = currentScheduleDto.exercise_name

        val imagePathToLoad = currentScheduleDto.image_path
        val resId = if (!imagePathToLoad.isNullOrBlank()) {
            try { resources.getIdentifier(imagePathToLoad, "drawable", requireContext().packageName).takeIf { it != 0 } } catch (e: Exception) { null }
        } else { null }

        Glide.with(requireContext())
            .load(resId ?: R.drawable.ic_launcher_background)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_launcher_background)
            .into(binding.exerciseImageView)

        if(::setAdapter.isInitialized) setAdapter.updateEquipAndType(currentScheduleDto.equip, currentScheduleDto.is_time_type)
        updateProgressText()
    }

    private fun completeCurrentSet() {
        if (currentSetIndex >= currentExerciseSets.size) {
            Log.w("ExerciseDoingFragment", "No more sets to complete or currentSetIndex out of bounds.")
            // ★ 수정: 모든 세트 완료 시에도 항상 휴식 타이머를 보여주고, 타이머 완료 후 다음 운동으로 넘어감
            showRestTimer(autoStart = true)
            return
        }

        val now = System.currentTimeMillis()
        val elapsedForThisSet = if (setStartTime > 0L) now - setStartTime else (currentExerciseSets[currentSetIndex].elapsedTimeMillis ?: 0L)

        Log.d("ExerciseDoingFragment", "Completing set index ${currentSetIndex}. Elapsed time: $elapsedForThisSet ms. scheduleId: $scheduleId")

        val setToComplete = currentExerciseSets[currentSetIndex]
        val completedSet = setToComplete.copy(
            isCompleted = true,
            isHighlighted = false,
            elapsedTimeMillis = elapsedForThisSet
        )
        currentExerciseSets[currentSetIndex] = completedSet

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = RetrofitClient.SetCompletionRequest(scheduleId, completedSet.setNumber, true)
                val updateResponse = RetrofitClient.scheduleApi.updateRepsSetCompletion(requestBody).execute()

                if (updateResponse.isSuccessful) {
                    Log.d("ExerciseDoingFragment", "✅ 서버 세트 완료 반영 성공: scheduleId=${scheduleId}, setNumber=${setToComplete.setNumber}")
                } else {
                    Log.e("ExerciseDoingFragment", "❌ 서버 세트 완료 반영 실패: ${updateResponse.code()} - ${updateResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e("ExerciseDoingFragment", "❌ 서버 세트 완료 반영 중 예외", e)
            }
        }

        val nextSetToShowIndex = currentSetIndex + 1

        val updatedListForAdapter = currentExerciseSets.mapIndexed { idx, s ->
            // 다음 세트 하이라이트는 showRestTimer의 콜백에서 처리하거나, 여기서 다음 세트가 있다면 임시로 하이라이트
            s.copy(isHighlighted = (nextSetToShowIndex < currentExerciseSets.size && idx == nextSetToShowIndex))
        }

        if(::setAdapter.isInitialized) setAdapter.submitList(updatedListForAdapter.toList())

        currentSetIndex = nextSetToShowIndex
        setStartTime = 0L
        saveProgressToPrefs()
        updateSetProgressUI()

        showRestTimer(autoStart = true)
    }

    private fun addNewSet() {
        if (scheduleId == -1L) {
            Toast.makeText(requireContext(), "현재 운동 정보가 없어 세트를 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val currentSchedule = planExerciseList.getOrNull(currentExerciseIndex) ?: return
        val exerciseIsTimeType = currentSchedule.is_time_type

        lifecycleScope.launch(Dispatchers.IO) {
            val nextSetNumber = (currentExerciseSets.maxByOrNull { it.setNumber }?.setNumber ?: 0) + 1

            val newSetForApi = if (exerciseIsTimeType) {
                RetrofitClient.TimeSetDto(setNumber = nextSetNumber, seconds = 60, weight = 0f)
            } else {
                RetrofitClient.RepsSetDto(setNumber = nextSetNumber, reps = 12, weight = 0f, isCompleted = false)
            }

            val currentSetsForApi = currentExerciseSets.map { existingSet ->
                if (exerciseIsTimeType) {
                    RetrofitClient.TimeSetDto(
                        setNumber = existingSet.setNumber,
                        seconds = (existingSet.elapsedTimeMillis ?: 0L).toInt() / 1000,
                        weight = existingSet.weight?.toFloat() ?: 0f,
                        isCompleted = existingSet.isCompleted ?: false
                    )
                } else {
                    RetrofitClient.RepsSetDto(
                        setNumber = existingSet.setNumber,
                        reps = existingSet.reps ?: 0,
                        weight = existingSet.weight?.toFloat() ?: 0f,
                        isCompleted = existingSet.isCompleted ?: false
                    )
                }
            }.toMutableList()

            if (exerciseIsTimeType) {
                currentSetsForApi.add(newSetForApi as RetrofitClient.TimeSetDto)
            } else {
                currentSetsForApi.add(newSetForApi as RetrofitClient.RepsSetDto)
            }

            try {
                val response: Response<Void> = if (exerciseIsTimeType) {
                    RetrofitClient.scheduleApi.updateTimeSets(scheduleId, currentSetsForApi.filterIsInstance<RetrofitClient.TimeSetDto>()).execute()
                } else {
                    RetrofitClient.scheduleApi.updateRepsSets(scheduleId, currentSetsForApi.filterIsInstance<RetrofitClient.RepsSetDto>()).execute()
                }

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        fetchSetsForCurrentExercise()
                        Toast.makeText(requireContext(), "${nextSetNumber}세트 추가됨", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Log.e("ExerciseDoingFragment", "새 세트 추가 실패 (서버): ${response.code()} - ${response.message()}")
                        Toast.makeText(requireContext(), "세트 추가에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    Log.e("ExerciseDoingFragment", "새 세트 추가 중 오류", e)
                    Toast.makeText(requireContext(), "세트 추가 중 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditSetBottomSheet() {
        if (scheduleId == -1L ) {
            Toast.makeText(requireContext(), "편집할 운동 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val currentScheduleForSheet = planExerciseList.getOrNull(currentExerciseIndex)
        val isTimeType = currentScheduleForSheet?.is_time_type ?: false

        val sheet: BottomSheetDialogFragment = if (isTimeType) {
            TimeSetEditDialogFragment.newInstance(scheduleId)
        } else {
            RepsSetEditDialogFragment.newInstance(scheduleId)
        }
        sheet.show(parentFragmentManager, if(isTimeType) TimeSetEditDialogFragment.TAG else RepsSetEditDialogFragment.TAG)
    }

    private fun updateProgressText() {
        if (!isAdded || _binding == null) return
        val actualCurrentExerciseDisplayIndex = planExerciseList.indexOfFirst { it.schedule_id.toLong() == scheduleId }
        val cur = if (actualCurrentExerciseDisplayIndex != -1) actualCurrentExerciseDisplayIndex + 1 else currentExerciseIndex + 1
        val total = planExerciseList.size
        binding.exerciseProgressTextView.text = if (total > 0) "$cur/$total" else "0/0"
    }

    private fun updateSetProgressUI() {
        if (!isAdded || _binding == null) return
        val completedSets = currentExerciseSets.count { it.isCompleted ?: false }
        val totalSets = currentExerciseSets.size
        Log.d("ExerciseDoingFragment", "Set progress UI updated: $completedSets / $totalSets")
    }

    private fun showRestTimer(autoStart: Boolean = false) {
        if (!isAdded || _binding == null) return
        // ★ 수정: isLastSetOfExercise는 currentSetIndex가 currentExerciseSets.size와 같거나 클 때 true
        val isLastSetOfCurrentExercise = currentSetIndex >= currentExerciseSets.size

        Log.d(
            "ExerciseDoingFragment",
            "showRestTimer: currentSetIndex=$currentSetIndex, totalSets=${currentExerciseSets.size}, isLastSetOfCurrentExercise=$isLastSetOfCurrentExercise"
        )

        val sheet = RestTimerFragment.newInstance(autoStart)
        sheet.setOnTimerFinishedListener {
            if (!isAdded || _binding == null) return@setOnTimerFinishedListener

            if (!isLastSetOfCurrentExercise) { // 현재 운동의 다음 세트가 남아있다면
                val nextSetToHighlightIndex = currentSetIndex // 이 시점의 currentSetIndex가 다음에 수행할 세트
                val listForAdapter = currentExerciseSets.mapIndexed { idx, s ->
                    s.copy(isHighlighted = (idx == nextSetToHighlightIndex))
                }
                if(::setAdapter.isInitialized) setAdapter.submitList(listForAdapter.toList()) {
                    if (nextSetToHighlightIndex < currentExerciseSets.size) {
                        binding.setsRecyclerView.smoothScrollToPosition(nextSetToHighlightIndex)
                    }
                }
                setStartTime = System.currentTimeMillis()
                saveProgressToPrefs() // setStartTime 및 setIndex 저장
                updateSetProgressUI()
                Log.d("ExerciseDoingFragment", "Rest timer finished. Preparing for next set (index $nextSetToHighlightIndex). New setStartTime: $setStartTime")
            } else { // 현재 운동의 모든 세트 완료
                Log.d("ExerciseDoingFragment", "Rest timer finished. All sets for current exercise are done. Completing current exercise.")
                completeCurrentExercise() // 다음 운동으로 넘어가는 로직 호출
            }
        }
        try {
            sheet.show(childFragmentManager, RestTimerFragment.TAG)
        } catch (e: IllegalStateException) {
            Log.e("ExerciseDoingFragment", "Error showing RestTimerFragment: ${e.message}")
        }
    }

    private fun completeCurrentExercise() {
        if (!isAdded) return
        val completedScheduleId = this.scheduleId
        val currentGlobalIndex = planExerciseList.indexOfFirst { it.schedule_id.toLong() == completedScheduleId }
        val nextExerciseGlobalIndex = if (currentGlobalIndex != -1) currentGlobalIndex + 1 else -1

        Log.d("ExerciseDoingFragment", "completeCurrentExercise called for schedule ID: $completedScheduleId, currentGlobalIndex: $currentGlobalIndex, nextGlobalIndex: $nextExerciseGlobalIndex")

        if (completedScheduleId == -1L && planExerciseList.isNotEmpty()) {
            Log.e("ExerciseDoingFragment", "Attempting to complete exercise with invalid scheduleId (-1L).")
            if (planExerciseList.isEmpty() || nextExerciseGlobalIndex >= planExerciseList.size) {
                isInProgress = false
                saveProgressToPrefs()
            }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (scheduleId != -1L) {
                    // 서버에 현재 스케줄(운동) 완료 상태 업데이트 API 호출
                    // RetrofitClient.ScheduleApi에 markScheduleAsComplete(scheduleId: Long): Response<Void> 와 같은 함수 필요
                    val completeResponse = RetrofitClient.scheduleApi.markScheduleAsComplete(scheduleId)
                    if (completeResponse.isSuccessful) {
                        Log.d("ExerciseDoingFragment", "서버에 현재 운동(scheduleId: $scheduleId) 완료 처리 성공")
                    } else {
                        Log.e("ExerciseDoingFragment", "서버 운동 완료 처리 실패: ${completeResponse.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseDoingFragment", "서버 운동 완료 처리 중 예외", e)
            }

            withContext(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@withContext
                parentFragmentManager.setFragmentResult("sets_updated", Bundle.EMPTY)

                if (nextExerciseGlobalIndex != -1 && nextExerciseGlobalIndex < planExerciseList.size) {
                    val nextScheduleDto = planExerciseList[nextExerciseGlobalIndex]
                    currentExerciseIndex = nextExerciseGlobalIndex
                    currentSetIndex = 0
                    setStartTime = System.currentTimeMillis()
                    scheduleId = nextScheduleDto.schedule_id.toLong()
                    currentExerciseId = nextScheduleDto.exercise_id.toLong()

                    saveProgressToPrefs()

                    updateExerciseInfoUI()
                    fetchSetsForCurrentExercise()
                    Log.d("ExerciseDoingFragment", "Moving to next exercise. New scheduleId: $scheduleId, New exerciseId: $currentExerciseId, New setStartTime: $setStartTime")
                } else {
                    Log.d("ExerciseDoingFragment", "All exercises in plan completed.")
                    isInProgress = false
                    prefs.edit()
                        .putBoolean(KEY_IN_PROGRESS, false)
                        .remove(KEY_EXERCISE_INDEX)
                        .remove(KEY_SET_INDEX)
                        .remove(KEY_START_TIME)
                        .remove(KEY_SCHEDULE_ID)
                        .remove(KEY_EXERCISE_ID)
                        .remove(KEY_PLAN_ID)
                        .apply()
                    try {
                        if (findNavController().currentDestination?.id == R.id.exerciseDoingFragment) {
                            findNavController().navigate(R.id.action_exerciseDoing_to_coolDownStretch)
                        }
                    } catch (e: Exception) { Log.e("ExerciseDoingFragment", "Navigation failed: ${e.message}") }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ExerciseDoingFragment", "onDestroyView called.")
        _binding = null
    }

    private class ExerciseSetAdapter(
        private val onSetClick: ((ExerciseSet) -> Unit)? = null,
        private val getExerciseIsTimeType: () -> Boolean
    ) : ListAdapter<ExerciseSet, ExerciseSetAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<ExerciseSet>() {
        override fun areItemsTheSame(old: ExerciseSet, new: ExerciseSet): Boolean {
            return if (old.id != 0L && new.id != 0L) old.id == new.id else old.setNumber == new.setNumber && old.exerciseId == new.exerciseId
        }
        override fun areContentsTheSame(old: ExerciseSet, new: ExerciseSet) = old == new
    }) {
        private var currentEquip: String? = null

        fun updateEquipAndType(equip: String?, isTimeTypeIgnored: Boolean) {
            if (currentEquip != equip?.trim()) {
                currentEquip = equip?.trim()
                notifyDataSetChanged()
            }
        }

        inner class ViewHolder(val b: ItemExerciseSetBinding) : RecyclerView.ViewHolder(b.root) {
            init {
                b.root.setOnClickListener {
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onSetClick?.invoke(getItem(pos))
                    }
                }
            }

            fun bind(s: ExerciseSet) {
                b.setNumberTextView.text = "${s.setNumber}세트"

                if (getExerciseIsTimeType()) {
                    val totalSeconds = s.times ?: 0L
                    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                    val secondsValue = totalSeconds % 60
                    b.repsTextView.text = String.format("%02d:%02d:%02d", hours, minutes, secondsValue)
                } else {
                    b.repsTextView.text = "${s.reps ?: "-"}회"
                }

                val lp = b.repsTextView.layoutParams as ViewGroup.MarginLayoutParams
                if (currentEquip.isNullOrBlank() || currentEquip in listOf("맨몸", "스텝박스", "세라밴드", "짐볼")) {
                    b.weightTextView.visibility = View.GONE
                    b.dividerImageView.visibility = View.GONE
                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
                        R.dimen.item_reps_margin_end_no_weight
                    )
                } else {
                    b.weightTextView.visibility = View.VISIBLE
                    b.dividerImageView.visibility = View.VISIBLE
                    b.weightTextView.text = s.weight?.let { if (it > 0) "${it}kg" else "-" } ?: "-"
                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
                        R.dimen.item_reps_margin_end_default
                    )
                }
                b.repsTextView.layoutParams = lp

                b.completionCheckImageView.alpha = if (s.isCompleted == true) 1f else 0.2f
                b.root.background = ContextCompat.getDrawable(
                    b.root.context,
                    when {
                        s.isCompleted == true -> R.drawable.set_item_background_completed
                        s.isHighlighted == true -> R.drawable.set_item_background_emphasized
                        else -> R.drawable.set_item_background
                    }
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                ItemExerciseSetBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(getItem(position))
    }
}
