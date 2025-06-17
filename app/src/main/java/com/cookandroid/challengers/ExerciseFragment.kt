package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.network.dto.ScheduleDto
import com.cookandroid.challengers.util.UserPreference
import com.cookandroid.challengers.viewmodel.ServerExerciseAdapter
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var serverAdapter: ServerExerciseAdapter
    private lateinit var db: AppDatabase
    private var planId: Long = -1L
    private var currentScheduleList: List<ScheduleDto> = emptyList()
    private var serverJob: Job? = null

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    companion object {
        private const val TAG = "ExerciseFragment"
        private const val PREFS_PROGRESS = "exercise_progress"
        private const val KEY_IN_PROGRESS = "is_in_progress"
        private const val KEY_SAVED_PLAN_ID = "current_plan_id_prefs"
        private const val KEY_SAVED_EXERCISE_INDEX = "current_exercise_index_prefs"
        private const val KEY_SAVED_SCHEDULE_ID = "current_schedule_id_prefs"
        private const val KEY_SAVED_EXERCISE_ID = "current_exercise_id_prefs"
        private const val KEY_SAVED_EXERCISE_NAME = "current_exercise_name_prefs"
        private const val KEY_SAVED_IMAGE_PATH = "current_image_path_prefs"
        private const val KEY_SAVED_EQUIP = "current_equip_prefs"
        private const val KEY_ELAPSED_TIME = "stopwatch_elapsed_time_prefs"
        private const val KEY_PHOTO_UPLOAD_COMPLETED_TODAY_PREFIX =
            "photo_upload_completed_for_date_"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun hasPhotoUploadBeenCompletedToday(): Boolean {
        val key = KEY_PHOTO_UPLOAD_COMPLETED_TODAY_PREFIX + getTodayDateString()
        return prefs.getBoolean(key, false)
    }

    private fun markPhotoUploadAsCompletedForToday() {
        val key = KEY_PHOTO_UPLOAD_COMPLETED_TODAY_PREFIX + getTodayDateString()
        prefs.edit().putBoolean(key, true).apply()
        Log.i(TAG, "Photo upload marked as completed for today: $key")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        planId = -1L
        Log.d(TAG, "onCreateView called")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupRecyclerView()
        setupClickListeners()
        setupFragmentResultListeners()
        setupInProgressHeaderListeners()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Loading plan and updating UI.")
        // ★★★ 화면에 돌아올 때마다 항상 최신 데이터를 불러오도록 onResume에서 호출 ★★★
        loadTodayPlanFromServer()
    }

    private fun setupRecyclerView() {
        serverAdapter = ServerExerciseAdapter(
            exerciseDao = db.exerciseDao(),
            onExerciseItemClicked = { clickedSchedule ->
                val index = currentScheduleList.indexOf(clickedSchedule)
                if (index != -1) {
                    // ★★★ 어떤 운동 아이템을 클릭하든 isContinuing을 true로 설정하여 스톱워치 시간 유지 ★★★
                    navigateToExerciseDoingFragment(index, clickedSchedule, true)
                } else {
                    navigateToFirstIncompleteExercise()
                }
            },
            onMoreButtonClicked = { schedule ->
                if (planId != -1L) {
                    val editFragment = ExerciseEditFragment(
                        initialPlanId = planId,
                        initialExerciseId = schedule.exercise_id.toLong(),
                        initialExerciseName = schedule.exercise_name
                    ) {
                        loadTodayPlanFromServer()
                    }
                    editFragment.show(parentFragmentManager, "ExerciseEditFragmentTag")
                } else {
                    Toast.makeText(requireContext(), "오늘의 운동 계획을 먼저 로드해주세요.", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onAddButtonClicked = {
                if (planId != -1L) {
                    val args = Bundle().apply { putLong("planId", planId) }
                    findNavController().navigate(R.id.action_exercise_to_exerciseAdd, args)
                } else {
                    Toast.makeText(requireContext(), "오늘의 운동 계획을 먼저 로드해주세요.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )

        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = serverAdapter
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition
                    if (viewHolder.itemViewType != ServerExerciseAdapter.TYPE_EXERCISE || target.itemViewType != ServerExerciseAdapter.TYPE_EXERCISE) {
                        return false
                    }
                    if (fromPosition < serverAdapter.currentList.size && toPosition < serverAdapter.currentList.size) {
                        serverAdapter.moveItem(fromPosition, toPosition)
                        return true
                    }
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    return if (viewHolder.itemViewType == ServerExerciseAdapter.TYPE_EXERCISE) {
                        super.getMovementFlags(recyclerView, viewHolder)
                    } else {
                        0
                    }
                }
            })
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun setupClickListeners() {
        binding.startExerciseButton.setOnClickListener {
            Log.d(TAG, "Start exercise button clicked")
            if (currentScheduleList.isNotEmpty() && currentScheduleList.all { it.is_completed }) {
                checkAndNavigateToPhotoUploadIfNeeded(true)
            } else {
                navigateToFirstIncompleteExercise()
            }
        }

        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exercise_to_exerciseList)
        }
    }

    private fun setupFragmentResultListeners() {
        val reloadDataListener: (String, Bundle) -> Unit = { _, _ -> loadTodayPlanFromServer() }
        parentFragmentManager.setFragmentResultListener(
            "plan_updated_from_add",
            viewLifecycleOwner,
            reloadDataListener
        )
        parentFragmentManager.setFragmentResultListener(
            "exercise_hidden",
            viewLifecycleOwner,
            reloadDataListener
        )
        parentFragmentManager.setFragmentResultListener(
            "exercise_unhidden",
            viewLifecycleOwner,
            reloadDataListener
        )
        parentFragmentManager.setFragmentResultListener(
            "sets_updated",
            viewLifecycleOwner,
            reloadDataListener
        )
        parentFragmentManager.setFragmentResultListener(
            ExerciseEditFragment.REQUEST_KEY_EXERCISE_EDIT,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(ExerciseEditFragment.RESULT_KEY_UPDATE_NEEDED, false)) {
                loadTodayPlanFromServer()
            }
        }
        parentFragmentManager.setFragmentResultListener(
            ChallengeUploadPhotoFragment.REQUEST_KEY_UPLOAD_PHOTO,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(
                    ChallengeUploadPhotoFragment.RESULT_KEY_PHOTO_ACTION_DONE,
                    false
                )
            ) {
                markPhotoUploadAsCompletedForToday()
                loadTodayPlanFromServer()
            }
        }
    }

//    private fun loadTodayPlanFromServer() {
//        if (!isAdded || _binding == null) {
//            Log.w(TAG, "loadTodayPlanFromServer: Fragment not added or binding is null. Aborting.")
//            return
//        }
//        Log.d(TAG, "loadTodayPlanFromServer: Fetching today's plan...")
//
//        serverJob?.cancel()
//        serverJob = viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val userId = UserPreference(requireContext()).getUserId()
//                if (userId == -1) {
//                    if (isActive) Toast.makeText(
//                        requireContext(),
//                        "로그인이 필요합니다.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@launch
//                }
//
//                val response =
//                    withContext(Dispatchers.IO) { RetrofitClient.scheduleApi.getTodayPlan(userId) }
//
//                if (!isActive) return@launch
//
//                if (response.isSuccessful) {
//                    val data = response.body()
//                    planId = data?.plan?.id?.toLong() ?: -1L
//
//                    val validSchedules =
//                        data?.schedules?.filter { it.exercise_id != 0 } ?: emptyList()
//                    val enrichedSchedules = withContext(Dispatchers.Default) {
//                        validSchedules.map { schedule ->
//                            val localExercise =
//                                db.exerciseDao().getExerciseById(schedule.exercise_id.toLong())
//                            schedule.copy(
//                                exercise_name = schedule.exercise_name.ifBlank {
//                                    localExercise?.name ?: "운동 이름 없음"
//                                },
//                                image_path = if (schedule.image_path.isNullOrBlank()) localExercise?.imagePath else schedule.image_path,
//                                equip = schedule.equip.ifBlank { localExercise?.equip ?: "정보 없음" },
//                                part = schedule.part.ifBlank { localExercise?.part ?: "부위 없음" }
//                            )
//                        }.sortedBy { it.exercise_order }
//                    }
//
//                    if (!isActive) return@launch
//
//                    currentScheduleList = enrichedSchedules
//                    serverAdapter.submitList(currentScheduleList.toList())
//                    checkAndShowInProgressHeader()
//                    updateStartButtonState()
//                    checkAndNavigateToPhotoUploadIfNeeded()
//
//                } else {
//                    val errorCode = response.code()
//                    currentScheduleList = emptyList()
//                    serverAdapter.submitList(emptyList())
//                    checkAndShowInProgressHeader()
//                    updateStartButtonState()
//                    val displayMessage =
//                        if (errorCode == 404) "오늘 진행할 운동 계획이 없습니다." else "운동 계획 로드 실패 (코드: $errorCode)"
//                    if (isActive) Toast.makeText(
//                        requireContext(),
//                        displayMessage,
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            } catch (e: Exception) {
//                if (isActive) {
//                    Log.e(TAG, "loadTodayPlanFromServer: Exception.", e)
//                    currentScheduleList = emptyList()
//                    serverAdapter.submitList(emptyList())
//                    checkAndShowInProgressHeader()
//                    updateStartButtonState()
//                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
private fun loadTodayPlanFromServer() {
    if (!isAdded || _binding == null) {
        Log.w(TAG, "loadTodayPlanFromServer: Fragment not added or binding is null. Aborting.")
        return
    }
    Log.d(TAG, "loadTodayPlanFromServer: Fetching today's plan...")

    serverJob?.cancel()
    serverJob = viewLifecycleOwner.lifecycleScope.launch {
        try {
            val userId = UserPreference(requireContext()).getUserId()
            if (userId == -1) {
                if (isActive) Toast.makeText(
                    requireContext(),
                    "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.scheduleApi.getTodayPlan(userId)
            }

            if (!isActive) return@launch

            if (response.isSuccessful) {
                val data = response.body()
                planId = data?.plan?.id?.toLong() ?: -1L

                val validSchedules = data?.schedules?.filter { it.exercise_id != 0 } ?: emptyList()
                val enrichedSchedules = withContext(Dispatchers.Default) {
                    validSchedules.map { schedule ->
                        val localExercise =
                            db.exerciseDao().getExerciseById(schedule.exercise_id.toLong())
                        schedule.copy(
                            exercise_name = schedule.exercise_name.ifBlank {
                                localExercise?.name ?: "운동 이름 없음"
                            },
                            image_path = if (schedule.image_path.isNullOrBlank()) localExercise?.imagePath else schedule.image_path,
                            equip = schedule.equip.ifBlank { localExercise?.equip ?: "정보 없음" },
                            part = schedule.part.ifBlank { localExercise?.part ?: "부위 없음" }
                        )
                    }.sortedBy { it.exercise_order }
                }

                if (!isActive) return@launch

                currentScheduleList = enrichedSchedules
                serverAdapter.submitList(currentScheduleList.toList())

                checkAndShowInProgressHeader()
                updateStartButtonState()


//                // ✅ 모든 운동 완료 + 오늘 인증 안 했을 때만 서버에 완료 카운트 요청
//                if (
//                    currentScheduleList.isNotEmpty() &&
//                    currentScheduleList.all { it.is_completed } &&
//                    !hasPhotoUploadBeenCompletedToday()
//                ) {
//                    Log.d(TAG, "운동 완료 API 호출 시도")
//                    try {
//                        val countResponse = withContext(Dispatchers.IO) {
//                            RetrofitClient.challengeApi.markExerciseDone(userId)
//                        }
//                        if (countResponse.isSuccessful) {
//                            Log.i(TAG, "✅ 운동 완료 1회 기록 성공")
//                        } else {
//                            Log.w(TAG, "⚠️ 운동 완료 기록 실패: ${countResponse.code()} ${countResponse.message()}")
//                            Log.d(TAG, "운동 완료 조건 불충족: " +
//                                    "currentScheduleList.size=${currentScheduleList.size}, " +
//                                    "allCompleted=${currentScheduleList.all { it.is_completed }}, " +
//                                    "photoUploaded=${hasPhotoUploadBeenCompletedToday()}")
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "🚨 운동 완료 기록 중 예외 발생", e)
//                    }
//                }
                // ✅ 모든 운동 완료 시 서버에 완료 카운트 요청 (사진 인증 여부와 무관)
                if (
                    currentScheduleList.isNotEmpty() &&
                    currentScheduleList.all { it.is_completed }
                ) {
                    Log.d(TAG, "운동 완료 API 호출 시도")
                    try {
                        val countResponse = withContext(Dispatchers.IO) {
                            RetrofitClient.challengeApi.markExerciseDone(userId)
                        }
                        if (countResponse.isSuccessful) {
                            Log.i(TAG, "✅ 운동 완료 1회 기록 성공")
                        } else {
                            Log.w(TAG, "⚠️ 운동 완료 기록 실패: ${countResponse.code()} ${countResponse.message()}")
                            Log.d(TAG, "운동 완료 조건 불충족: " +
                                    "currentScheduleList.size=${currentScheduleList.size}, " +
                                    "allCompleted=${currentScheduleList.all { it.is_completed }}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "🚨 운동 완료 기록 중 예외 발생", e)
                    }
                }

                checkAndNavigateToPhotoUploadIfNeeded()

            } else {
                val errorCode = response.code()
                currentScheduleList = emptyList()
                serverAdapter.submitList(emptyList())
                checkAndShowInProgressHeader()
                updateStartButtonState()
                val displayMessage =
                    if (errorCode == 404) "오늘 진행할 운동 계획이 없습니다." else "운동 계획 로드 실패 (코드: $errorCode)"
                if (isActive) Toast.makeText(
                    requireContext(),
                    displayMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            if (isActive) {
                Log.e(TAG, "loadTodayPlanFromServer: Exception.", e)
                currentScheduleList = emptyList()
                serverAdapter.submitList(emptyList())
                checkAndShowInProgressHeader()
                updateStartButtonState()
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

    private fun updateStartButtonState() {
        if (_binding == null || !isAdded) return

        if (binding.inProgressHeader.visibility == View.VISIBLE) {
            binding.startExerciseButton.visibility = View.GONE
        } else {
            binding.startExerciseButton.visibility = View.VISIBLE
            if (currentScheduleList.any { !it.is_completed }) {
                binding.startExerciseButton.text = "운동 시작하기"
                binding.startExerciseButton.isEnabled = true
            } else if (currentScheduleList.isNotEmpty()) { // 모든 운동 완료
                if (hasPhotoUploadBeenCompletedToday()) { // ★ 오늘 이미 사진 인증 완료했으면
                    binding.startExerciseButton.text = "오늘 운동 완료" // 또는 다른 적절한 텍스트
                    binding.startExerciseButton.isEnabled = false // 비활성화
                } else {
                    binding.startExerciseButton.text = "사진 인증하기"
                    binding.startExerciseButton.isEnabled = true
                }
            } else { // 운동 목록 없음
                binding.startExerciseButton.text = "운동 추가하기"
                binding.startExerciseButton.isEnabled = planId != -1L
            }
        }
        Log.d(
            TAG,
            "updateStartButtonState: Text='${binding.startExerciseButton.text}', Enabled=${binding.startExerciseButton.isEnabled}"
        )
    }

    private fun checkAndShowInProgressHeader() {
        if (_binding == null || !isAdded) return
        val showHeader = prefs.getBoolean(KEY_IN_PROGRESS, false)
        val savedPlanId = prefs.getLong(KEY_SAVED_PLAN_ID, -1L)
        val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
        Log.d(
            TAG,
            "checkAndShowInProgressHeader: showHeader=$showHeader, savedPlanId=$savedPlanId, currentPlanId=$planId, savedScheduleId=$savedScheduleId"
        )

        if (showHeader && savedPlanId == planId && planId != -1L && savedScheduleId != -1L) {
            val inProgressScheduleInfo =
                currentScheduleList.find { it.schedule_id.toLong() == savedScheduleId }
            if (inProgressScheduleInfo != null && !inProgressScheduleInfo.is_completed) {
                val savedExerciseName = prefs.getString(KEY_SAVED_EXERCISE_NAME, null)
                val savedImagePath = prefs.getString(KEY_SAVED_IMAGE_PATH, null)
                // val savedElapsedTime = stopwatchViewModel.elapsedTime.value ?: prefs.getLong(KEY_ELAPSED_TIME, 0L) // ViewModel 우선
                val displayName = inProgressScheduleInfo.exercise_name.takeIf { it.isNotBlank() }
                    ?: savedExerciseName ?: "진행 중인 운동"
                val displayImagePath =
                    inProgressScheduleInfo.image_path.takeIf { !it.isNullOrBlank() }
                        ?: savedImagePath

                binding.inProgressHeader.visibility = View.VISIBLE
                binding.startExerciseButton.visibility = View.GONE // 진행 중 헤더 보이면 시작 버튼 숨김
                binding.inProgressExerciseName.text = displayName
                val resId = if (!displayImagePath.isNullOrBlank()) try {
                    resources.getIdentifier(
                        displayImagePath,
                        "drawable",
                        requireContext().packageName
                    ).takeIf { it != 0 }
                } catch (e: Exception) {
                    null
                } else null
                Glide.with(this).load(resId ?: R.drawable.ic_fitbuddy_logo)
                    .error(R.drawable.ic_fitbuddy_logo).into(binding.exerciseImageView)
                setRecyclerViewMargin(true)
                Log.d(TAG, "In-progress header SHOWN for: $displayName")

                // ★ ViewModel 관찰 시작 (ExerciseFragment2 스타일)
                stopwatchViewModel.elapsedTime.removeObservers(viewLifecycleOwner) // 이전 관찰자 제거
                stopwatchViewModel.isRunning.removeObservers(viewLifecycleOwner)   // 이전 관찰자 제거

                stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner, Observer { time ->
                    if (binding.inProgressHeader.visibility == View.VISIBLE) { // 헤더가 보일 때만 업데이트
                        binding.inProgressTime.text = stopwatchViewModel.formatElapsedTime(time)
                    }
                })
                stopwatchViewModel.isRunning.observe(viewLifecycleOwner, Observer { running ->
                    if (binding.inProgressHeader.visibility == View.VISIBLE) { // 헤더가 보일 때만 업데이트
                        binding.mainStopwatchButton.setImageResource(if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black)
                    }
                })

                val initialElapsedTime =
                    stopwatchViewModel.elapsedTime.value ?: prefs.getLong(KEY_ELAPSED_TIME, 0L)
                binding.inProgressTime.text =
                    stopwatchViewModel.formatElapsedTime(initialElapsedTime)

                binding.mainStopwatchButton.setImageResource(if (stopwatchViewModel.isRunning.value == true) R.drawable.ic_pause_black else R.drawable.ic_play_black)

            } else { // 진행 중인 운동 정보가 없거나, 완료된 경우 헤더 숨김
                binding.inProgressHeader.visibility = View.GONE
                if (showHeader) {
                    Log.d(
                        TAG,
                        "In-progress header conditions not fully met (e.g., exercise completed or info mismatch). Clearing state."
                    ); clearInProgressState()
                }
                setRecyclerViewMargin(false)
                Log.d(TAG, "In-progress header HIDDEN (or cleared).")
            }
        } else {
            binding.inProgressHeader.visibility = View.GONE
            if (showHeader && savedPlanId != planId) {
                Log.d(
                    TAG,
                    "In-progress header for a different plan. Clearing state."
                ); clearInProgressState()
            }
            setRecyclerViewMargin(false)
            Log.d(TAG, "In-progress header HIDDEN (initial state or different plan).")
        }
        updateStartButtonState() // 시작 버튼 상태는 헤더 표시 여부에 따라 달라지므로 항상 호출
    }

    private fun clearInProgressState() {
        prefs.edit()
            .putBoolean(KEY_IN_PROGRESS, false)
            .remove(KEY_SAVED_EXERCISE_INDEX)
            .remove(KEY_SAVED_PLAN_ID)
            .remove(KEY_SAVED_SCHEDULE_ID)
            .remove(KEY_SAVED_EXERCISE_ID)
            .remove(KEY_SAVED_EXERCISE_NAME)
            .remove(KEY_SAVED_IMAGE_PATH)
            .remove(KEY_SAVED_EQUIP)
            // KEY_ELAPSED_TIME은 모든 운동 완료 후 사진 인증으로 넘어갈 때 사용되므로, 여기서 바로 지우지 않도록 주의.
            // .remove(KEY_ELAPSED_TIME) // <- 이 부분은 신중히 결정
            .apply()
        Log.i(TAG, "clearInProgressState: Cleared most in-progress state from SharedPreferences.")
        if (isAdded && _binding != null) checkAndShowInProgressHeader()
    }

    private fun setRecyclerViewMargin(isHeaderVisible: Boolean) {
        if (_binding == null || !isAdded) return
        val params =
            binding.exerciseListRecyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.topToBottom =
            if (isHeaderVisible) binding.inProgressHeader.id else binding.todayExerciseText.id
        binding.exerciseListRecyclerView.requestLayout()
    }

    private fun setupInProgressHeaderListeners() {
        if (_binding == null || !isAdded) return
        binding.goToDoingButton.setOnClickListener {
            Log.d(TAG, "Go to doing button clicked.")
            val savedExerciseIndex = prefs.getInt(KEY_SAVED_EXERCISE_INDEX, -1)
            val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
            val savedPlanIdFromPrefs = prefs.getLong(KEY_SAVED_PLAN_ID, -1L)

            if (savedPlanIdFromPrefs == planId && planId != -1L && savedScheduleId != -1L) {
                val targetSchedule =
                    currentScheduleList.find { it.schedule_id.toLong() == savedScheduleId }
                val targetIndex =
                    if (targetSchedule != null) currentScheduleList.indexOf(targetSchedule) else savedExerciseIndex

                if (targetIndex != -1 && targetIndex < currentScheduleList.size) {
                    navigateToExerciseDoingFragment(
                        targetIndex,
                        currentScheduleList[targetIndex],
                        true
                    )
                } else {
                    Log.w(
                        TAG,
                        "Could not find target for continuing. Clearing state. TargetIndex: $targetIndex, ListSize: ${currentScheduleList.size}"
                    )
                    Toast.makeText(requireContext(), "진행 중인 운동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                    clearInProgressState()
                }
            } else {
                Log.w(TAG, "No valid in-progress data to continue.")
                Toast.makeText(requireContext(), "진행 중인 운동 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                clearInProgressState()
            }
        }
        binding.mainStopwatchButton.setOnClickListener {
            Log.d(
                TAG,
                "Main stopwatch button in header clicked. ViewModel isRunning: ${stopwatchViewModel.isRunning.value}"
            )

            // 진행 중인 운동 세션이 유효한지 먼저 확인 (현재 planId와 저장된 planId 일치 여부 등)
            val isValidInProgressSession = prefs.getBoolean(KEY_IN_PROGRESS, false) &&
                    prefs.getLong(KEY_SAVED_PLAN_ID, -1L) == planId && // 현재 planId와 일치하는지 확인
                    planId != -1L &&
                    prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L) != -1L

            if (!isValidInProgressSession) {
                Toast.makeText(
                    requireContext(),
                    "현재 진행 중인 운동 세션 정보가 올바르지 않습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                // 필요하다면 clearInProgressState() 호출 또는 UI 강제 갱신
                return@setOnClickListener
            }

            if (stopwatchViewModel.isRunning.value == true) {
                stopwatchViewModel.pauseStopwatch()
                // ViewModel의 현재 시간을 SharedPreferences에 백업 (앱 재시작 대비)
                prefs.edit().putLong(KEY_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L)
                    .apply()
                Log.d(
                    TAG,
                    "Stopwatch paused via header. Elapsed time (${stopwatchViewModel.elapsedTime.value}) saved to prefs."
                )
            } else {
                // ViewModel의 현재 시간부터 이어 시작.
                // 이 값은 이전에 pause 하면서 저장되었거나, ExerciseDoingFragment에서 업데이트된 값일 수 있음.
                val timeToStartFrom =
                    stopwatchViewModel.elapsedTime.value ?: 0L // ViewModel의 현재 시간 사용
                stopwatchViewModel.startStopwatch(timeToStartFrom)
                Log.d(TAG, "Stopwatch resumed/started via header from: $timeToStartFrom")
            }
        }
        binding.completeButton.setOnClickListener {
            Log.d(TAG, "Header Complete button clicked.")
            val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
            if (savedScheduleId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val finalElapsedTime =
                            stopwatchViewModel.elapsedTime.value ?: prefs.getLong(
                                KEY_ELAPSED_TIME,
                                0L
                            )
                        withContext(Dispatchers.Main) {
                            prefs.edit().putLong(KEY_ELAPSED_TIME, finalElapsedTime).apply()
                            Log.d(
                                TAG,
                                "Saved final elapsed time from header completion: $finalElapsedTime"
                            )
                        }

                        val response =
                            RetrofitClient.scheduleApi.markScheduleAsComplete(savedScheduleId)
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            if (response.isSuccessful) {
                                Log.i(
                                    TAG,
                                    "Exercise (scheduleId: $savedScheduleId) marked as complete via header."
                                )
                                clearInProgressState() // 진행 중 상태 헤더 제거

                                stopwatchViewModel.stopStopwatch()
                                Log.i(
                                    TAG,
                                    "Stopwatch explicitly reset after completing exercise via header completeButton."
                                )

                                loadTodayPlanFromServer() // 목록 새로고침
                                Toast.makeText(requireContext(), "운동을 완료했습니다!", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Log.e(
                                    TAG,
                                    "Failed to mark exercise as complete via header: ${response.code()} ${response.message()}"
                                )
                                Toast.makeText(
                                    requireContext(),
                                    "운동 완료 처리에 실패했습니다: ${response.message()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            Log.e(TAG, "Exception marking exercise as complete via header.", e)
                            Toast.makeText(
                                requireContext(),
                                "운동 완료 처리 중 오류: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "완료할 운동 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToFirstIncompleteExercise() {
        if (planId == -1L && currentScheduleList.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "오늘 진행할 운동 계획이 없습니다. 먼저 계획을 로드하거나 추가해주세요.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val firstIncompleteIndex = currentScheduleList.indexOfFirst { !it.is_completed }
        if (firstIncompleteIndex != -1) {
            Log.d(TAG, "Navigating to first incomplete exercise at index $firstIncompleteIndex")
            // 첫 미완료 운동으로 가는 것은 '새로 시작'이므로 isContinuing = false
            navigateToExerciseDoingFragment(
                firstIncompleteIndex,
                currentScheduleList[firstIncompleteIndex],
                false
            )
        } else if (currentScheduleList.isNotEmpty()) { // 모든 운동 완료
            Log.i(
                TAG,
                "All exercises in current list are completed. Checking for photo upload navigation."
            )
            checkAndNavigateToPhotoUploadIfNeeded(true)
        } else {
            if (planId != -1L) {
                Toast.makeText(requireContext(), "운동 계획에 운동이 없습니다. 운동을 추가해주세요.", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigate(
                    R.id.action_exercise_to_exerciseAdd,
                    Bundle().apply { putLong("planId", planId) })
            } else {
                Toast.makeText(requireContext(), "오늘 진행할 운동 계획이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndNavigateToPhotoUploadIfNeeded(forceNavigationCheck: Boolean = false) {
        if (!isAdded || _binding == null) return

        // ★ 조건 추가: 오늘 이미 사진 인증을 완료했는지 확인
        if (hasPhotoUploadBeenCompletedToday()) {
            Log.i(TAG, "Photo upload has already been completed today. Skipping navigation.")
            // 필요하다면 버튼 상태 등을 "오늘 운동 모두 완료" 등으로 업데이트
            if (currentScheduleList.isNotEmpty() && currentScheduleList.all { it.is_completed }) {
                updateStartButtonState() // 버튼 텍스트 등을 "오늘 운동 완료"로 업데이트
            }
            return
        }

        if (currentScheduleList.isNotEmpty() && currentScheduleList.all { it.is_completed }) {
            Log.i(
                TAG,
                "All exercises are marked as complete AND photo upload not yet done today. Preparing to navigate to photo upload."
            )

            val completionTimeMillis = System.currentTimeMillis()
            val totalDurationMillis =
                prefs.getLong(KEY_ELAPSED_TIME, 0L) // ExerciseDoingFragment 또는 헤더 완료 버튼에서 저장한 값

            Log.d(
                TAG,
                "Photo Upload Nav: CompletionTime=$completionTimeMillis, TotalDuration=$totalDurationMillis (from prefs)"
            )

            stopwatchViewModel.stopStopwatch() // ViewModel의 스톱워치는 리셋 (전체 세션 종료)
            // clearInProgressState()는 헤더를 숨기고 관련 prefs를 정리. KEY_ELAPSED_TIME은 남겨둠.
            // KEY_ELAPSED_TIME은 다음 날 새 운동 시작 시 0으로 리셋되거나,
            // navigateToExerciseDoingFragment에서 !isContinuing일 때 0으로 리셋됨.
            prefs.edit().putBoolean(KEY_IN_PROGRESS, false).apply() // 헤더만 숨김

            val args = Bundle().apply {
                putLong("completion_time_millis", completionTimeMillis)
                putLong("total_duration_millis", totalDurationMillis)
            }
            try {
                // TODO: 네비게이션 그래프에 action_exercise_to_challengeUpload ID 확인 및 정의
                val actionId = R.id.action_exercise_to_challengeUpload // 실제 정의된 액션 ID로 변경!
                findNavController().navigate(actionId, args)
                Log.i(TAG, "Navigated to ChallengeUploadPhotoFragment.")
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Navigation to ChallengeUploadPhotoFragment failed. Action ID might be missing or incorrect.",
                    e
                )
                Toast.makeText(requireContext(), "사진 인증 화면 이동 실패: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            if (forceNavigationCheck) Log.d(
                TAG,
                "checkAndNavigateToPhotoUploadIfNeeded: Not all exercises completed, list empty, or photo already uploaded today."
            )
        }
    }


    private fun navigateToExerciseDoingFragment(
        index: Int,
        schedule: ScheduleDto,
        isContinuing: Boolean
    ) {
        if (planId == -1L) {
            Log.e(TAG, "navigateToExerciseDoingFragment: planId is -1."); Toast.makeText(
                requireContext(),
                "유효한 운동 계획 정보가 없습니다.",
                Toast.LENGTH_SHORT
            ).show(); return
        }
        if (index < 0 || index >= currentScheduleList.size) {
            Log.e(TAG, "navigateToExerciseDoingFragment: Invalid index $index"); Toast.makeText(
                requireContext(),
                "운동 정보를 불러올 수 없습니다 (인덱스 오류).",
                Toast.LENGTH_SHORT
            ).show(); return
        }

        val targetSchedule = currentScheduleList[index] // 이 부분은 schedule 파라미터를 직접 사용해도 됨
        val args = Bundle().apply {
            putLong("planId", planId)
            putInt("initialExerciseIndex", index)
            putLong("scheduleId", schedule.schedule_id.toLong()) // 파라미터 schedule 사용
            putLong("exerciseId", schedule.exercise_id.toLong()) // 파라미터 schedule 사용
            putString("exerciseName", schedule.exercise_name)     // 파라미터 schedule 사용
            putString("imagePath", schedule.image_path)         // 파라미터 schedule 사용
            putString("equip", schedule.equip)                 // 파라미터 schedule 사용
        }

        val editor = prefs.edit()
        editor.putBoolean(KEY_IN_PROGRESS, true)
        editor.putLong(KEY_SAVED_PLAN_ID, planId)
        editor.putInt(KEY_SAVED_EXERCISE_INDEX, index)
        editor.putLong(KEY_SAVED_SCHEDULE_ID, schedule.schedule_id.toLong())
        editor.putLong(KEY_SAVED_EXERCISE_ID, schedule.exercise_id.toLong())
        editor.putString(KEY_SAVED_EXERCISE_NAME, schedule.exercise_name)
        editor.putString(KEY_SAVED_IMAGE_PATH, schedule.image_path)
        editor.putString(KEY_SAVED_EQUIP, schedule.equip)

        if (!isContinuing) {
            editor.putLong(KEY_ELAPSED_TIME, 0L) // 새 운동 시작 시 SharedPreferences의 누적 시간 0으로.
            stopwatchViewModel.stopStopwatch() // ★ ViewModel 스톱워치 리셋 (시간도 0으로)
            Log.d(
                TAG,
                "Navigating to new exercise: ${schedule.exercise_name}. Resetting stopwatch and elapsed time in ViewModel. KEY_ELAPSED_TIME set to 0."
            )
        } else {
            // 이어하기 시에는 prefs에 저장된 KEY_ELAPSED_TIME (총 누적 시간)을 사용.
            // ExerciseDoingFragment가 이 값을 읽어 ViewModel의 시간을 설정할 것임.
            val elapsedTimeForContinuing = prefs.getLong(KEY_ELAPSED_TIME, 0L)
            // editor.putLong(KEY_ELAPSED_TIME, elapsedTimeForContinuing) // 이미 prefs에 있으므로 다시 쓸 필요는 없음.
            // 만약 ExerciseDoingFragment가 시작 시 ViewModel을 prefs 값으로 항상 덮어쓴다면 이 줄은 불필요.
            // 여기서는 KEY_ELAPSED_TIME은 ExerciseDoingFragment에서 관리하는 총 시간을 나타낸다고 가정.
            Log.d(
                TAG,
                "Navigating to continue exercise: ${schedule.exercise_name}. Current KEY_ELAPSED_TIME in prefs for DoingFragment: $elapsedTimeForContinuing."
            )
        }
        editor.apply()
        findNavController().navigate(R.id.action_exercise_to_exerciseDoing, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        if (::serverAdapter.isInitialized) {
            binding.exerciseListRecyclerView.adapter = null
        }
        _binding = null
        serverJob?.cancel()
    }
}