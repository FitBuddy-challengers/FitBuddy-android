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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.util.UserPreference
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.network.dto.ScheduleDto
import com.cookandroid.challengers.viewmodel.ServerExerciseAdapter
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var serverAdapter: ServerExerciseAdapter
    private lateinit var db: AppDatabase
    private var planId: Long = -1L
    private var currentScheduleList: List<ScheduleDto> = emptyList()

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    companion object {
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
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        planId = -1L
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serverAdapter = ServerExerciseAdapter(
            exerciseDao = db.exerciseDao(),
            onExerciseItemClicked = { schedule -> // 아이템 전체 클릭 시
                val index = currentScheduleList.indexOf(schedule)
                if (index != -1) {
                    navigateToExerciseDoingFragment(index, schedule, false) // isContinuing = false
                } else {
                    navigateToFirstIncompleteExercise()
                }
            },
            onMoreButtonClicked = { schedule -> // '더보기' 버튼 클릭 시
                if (planId != -1L) {
                    val editFragment = ExerciseEditFragment(
                        planId = planId,
                        exerciseId = schedule.exercise_id.toLong(),
                        exerciseName = schedule.exercise_name
                    ) {
                        loadTodayPlanFromServer()
                    }
                    editFragment.show(parentFragmentManager, "ExerciseEdit")
                } else {
                    Toast.makeText(requireContext(), "운동 계획 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onAddButtonClicked = { // '운동 추가' 버튼 클릭 시
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
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (viewHolder.itemViewType != ServerExerciseAdapter.TYPE_EXERCISE || target.itemViewType != ServerExerciseAdapter.TYPE_EXERCISE) {
                        return false
                    }
                    if (from < serverAdapter.currentList.size && to < serverAdapter.currentList.size) {
                        serverAdapter.moveItem(from, to)
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

        binding.startExerciseButton.setOnClickListener {
            navigateToFirstIncompleteExercise()
        }

        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exercise_to_exerciseList)
        }

        parentFragmentManager.setFragmentResultListener(
            "sets_updated",
            viewLifecycleOwner
        ) { _, _ ->
            loadTodayPlanFromServer()
        }
        parentFragmentManager.setFragmentResultListener(
            "plan_updated_from_add",
            viewLifecycleOwner
        ) { _, _ ->
            loadTodayPlanFromServer()
        }
        parentFragmentManager.setFragmentResultListener(
            "exercise_hidden",
            viewLifecycleOwner
        ) { _, _ ->
            loadTodayPlanFromServer()
        }
        parentFragmentManager.setFragmentResultListener(
            "exercise_unhidden",
            viewLifecycleOwner
        ) { _, _ ->
            loadTodayPlanFromServer()
        }

        setupInProgressHeaderListeners()
    }

    override fun onResume() {
        super.onResume()
        Log.d("ExerciseFragment", "onResume: Loading plan and updating header.")
        loadTodayPlanFromServer() // 화면에 다시 보일 때마다 데이터 및 진행 상태 갱신
    }

    private fun loadTodayPlanFromServer() {
        if (!isAdded || _binding == null) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 로그인된 사용자 ID 가져오기Add commentMore actions
                val userPref = UserPreference(requireContext())
                val userId = userPref.getUserId()
                if (userId == -1) {
                    Log.e("ExerciseFragment", "❗ 유효하지 않은 userId (로그인 필요)")
                    return@launch
                }
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId = userId)
                if (response.isSuccessful) {
                    val data = response.body()
                    planId = data?.plan?.id?.toLong() ?: -1L

                    val validSchedules =
                        data?.schedules?.filter { it.exercise_id != 0 } ?: emptyList()
                    val enriched = validSchedules.map { schedule ->
                        val ex = db.exerciseDao().getExerciseById(schedule.exercise_id.toLong())
                        schedule.copy(
                            exercise_name = schedule.exercise_name.ifBlank {
                                ex?.name ?: "운동 이름 없음"
                            },
                            image_path = if (schedule.image_path.isNullOrBlank()) ex?.imagePath else schedule.image_path,
                            equip = schedule.equip.ifBlank { ex?.equip ?: "정보 없음" },
                            part = schedule.part.ifBlank { ex?.part ?: "부위 없음" },
                            start_position = schedule.start_position ?: ex?.startPosition,
                            exercise_motion = schedule.exercise_motion ?: ex?.exerciseMotion,
                            breathing = schedule.breathing ?: ex?.breathing,
                            caution = schedule.caution ?: ex?.caution,
                            mets = schedule.mets.takeIf { it != 0.0 } ?: ex?.mets ?: 0.0
                        )
                    }.sortedBy { it.exercise_order }
                    currentScheduleList = enriched

                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            serverAdapter.submitList(currentScheduleList)
                            checkAndShowInProgressHeader()
                            updateStartButtonState()
                        }
                    }
                } else {
                    planId = -1L
                    currentScheduleList = emptyList()
                    val errorCode = response.code()
                    var toastMessage = "운동 계획을 불러오지 못했습니다. (코드: $errorCode)"
                    if (errorCode == 404) {
                        try {
                            val errorBody = response.errorBody()?.string()
                            if (!errorBody.isNullOrBlank()) {
                                val gson = Gson()
                                val errorResponse = gson.fromJson(
                                    errorBody,
                                    RetrofitClient.ServerResponse::class.java
                                )
                                toastMessage = errorResponse.message ?: "오늘 진행할 운동 계획이 없습니다."
                            } else {
                                toastMessage = "오늘 진행할 운동 계획이 없습니다."
                            }
                        } catch (e: Exception) {
                            Log.e("ExerciseFragment", "404 오류 본문 파싱 실패", e)
                        }
                    } else {
                        Log.e(
                            "ExerciseFragment",
                            "❌ 서버 오류: ${response.code()} - ${response.message()}"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            serverAdapter.submitList(emptyList())
                            checkAndShowInProgressHeader()
                            updateStartButtonState()
                            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                planId = -1L
                currentScheduleList = emptyList()
                Log.e("ExerciseFragment", "❗ 네트워크 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        serverAdapter.submitList(emptyList())
                        checkAndShowInProgressHeader()
                        updateStartButtonState()
                        Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                }
            }
        }
    }

    private fun updateStartButtonState() {
        if (_binding == null || !isAdded) return
        val isInProgress = prefs.getBoolean(KEY_IN_PROGRESS, false)
        val savedPlanId = prefs.getLong(KEY_SAVED_PLAN_ID, -1L)

        // ★ 수정: 진행 중 헤더가 보일 때 시작 버튼 숨김
        if (binding.inProgressHeader.visibility == View.VISIBLE) {
            binding.startExerciseButton.visibility = View.GONE
        } else {
            binding.startExerciseButton.visibility = View.VISIBLE
            if (currentScheduleList.any { !it.is_completed }) {
                binding.startExerciseButton.text = "운동 시작하기"
                binding.startExerciseButton.isEnabled = true
            } else if (currentScheduleList.isNotEmpty()) {
                binding.startExerciseButton.text = "모든 운동 완료"
                binding.startExerciseButton.isEnabled = false
            } else {
                binding.startExerciseButton.text = "운동 추가하기"
                binding.startExerciseButton.isEnabled = planId != -1L
            }
        }
    }

    private fun checkAndShowInProgressHeader() {
        if (_binding == null || !isAdded) return

        val isInProgress = prefs.getBoolean(KEY_IN_PROGRESS, false)
        val savedPlanId = prefs.getLong(KEY_SAVED_PLAN_ID, -1L)
        val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
        val savedExerciseName = prefs.getString(KEY_SAVED_EXERCISE_NAME, null)
        val savedImagePath = prefs.getString(KEY_SAVED_IMAGE_PATH, null) // ★ 이 키 사용
        val savedElapsedTime =
            stopwatchViewModel.elapsedTime.value ?: prefs.getLong(KEY_ELAPSED_TIME, 0L)

        Log.d(
            "ExerciseFragment",
            "checkAndShowInProgressHeader - isInProgress: $isInProgress, savedPlanId: $savedPlanId, currentPlanId: $planId, savedScheduleId: $savedScheduleId"
        )

        if (isInProgress && savedPlanId == planId && planId != -1L && savedScheduleId != -1L) {
            val inProgressScheduleInfo =
                currentScheduleList.find { it.schedule_id.toLong() == savedScheduleId }
            // SharedPreferences에 저장된 이름/이미지를 우선 사용하거나,
            // currentScheduleList에서 찾은 정보로 업데이트
            val displayName =
                inProgressScheduleInfo?.exercise_name ?: savedExerciseName ?: "진행 중인 운동"
            val displayImagePath = inProgressScheduleInfo?.image_path ?: savedImagePath


            if (inProgressScheduleInfo != null && !inProgressScheduleInfo.is_completed) { // 진행 중인 운동이 현재 목록에 있고 미완료 상태일 때
                binding.inProgressHeader.visibility = View.VISIBLE
                binding.startExerciseButton.visibility = View.GONE

                binding.inProgressExerciseName.text = displayName
                val resId = if (!displayImagePath.isNullOrBlank()) {
                    try {
                        resources.getIdentifier(
                            displayImagePath,
                            "drawable",
                            requireContext().packageName
                        ).takeIf { it != 0 }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                Glide.with(this)
                    .load(resId ?: R.drawable.ic_launcher_background)
                    .into(binding.exerciseImageView)

                binding.inProgressTime.text = stopwatchViewModel.formatElapsedTime(savedElapsedTime)
                binding.mainStopwatchButton.setImageResource(
                    if (stopwatchViewModel.isRunning.value == true) R.drawable.ic_pause_black else R.drawable.ic_play_black
                )
                setRecyclerViewMargin(true)
            } else {
                binding.inProgressHeader.visibility = View.GONE
                if (isInProgress) {
                    clearInProgressState()
                }
                setRecyclerViewMargin(false)
            }
        } else {
            binding.inProgressHeader.visibility = View.GONE
            if (isInProgress && savedPlanId != planId) {
                clearInProgressState()
            }
            setRecyclerViewMargin(false)
        }
        updateStartButtonState()
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
            .remove(KEY_ELAPSED_TIME)
            .apply()
        Log.d(
            "ExerciseFragment",
            "clearInProgressState: Cleared in-progress state from SharedPreferences."
        )
    }

    private fun setRecyclerViewMargin(isHeaderVisible: Boolean) {
        if (_binding == null || !isAdded) return
        val params =
            binding.exerciseListRecyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (isHeaderVisible) {
            params.topToBottom = binding.inProgressHeader.id
        } else {
            params.topToBottom = binding.todayExerciseText.id
        }
        binding.exerciseListRecyclerView.requestLayout()
    }

    private fun setupInProgressHeaderListeners() {
        if (_binding == null || !isAdded) return

        binding.goToDoingButton.setOnClickListener {
            val savedExerciseIndex = prefs.getInt(KEY_SAVED_EXERCISE_INDEX, -1)
            val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
            val savedPlanIdFromPrefs = prefs.getLong(KEY_SAVED_PLAN_ID, -1L)

            if (savedPlanIdFromPrefs == planId && planId != -1L && savedScheduleId != -1L) {
                val targetSchedule =
                    currentScheduleList.find { it.schedule_id.toLong() == savedScheduleId }
                val targetIndex =
                    if (targetSchedule != null) currentScheduleList.indexOf(targetSchedule) else savedExerciseIndex.coerceAtLeast(
                        0
                    )

                if (targetIndex != -1 && targetIndex < currentScheduleList.size) {
                    val scheduleToPass = currentScheduleList[targetIndex]
                    navigateToExerciseDoingFragment(targetIndex, scheduleToPass, true)
                } else {
                    Toast.makeText(requireContext(), "진행 중인 운동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                    clearInProgressState()
                    checkAndShowInProgressHeader()
                }
            } else {
                Toast.makeText(requireContext(), "진행 중인 운동 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                clearInProgressState()
                checkAndShowInProgressHeader()
            }
        }

        binding.mainStopwatchButton.setOnClickListener {
            if (stopwatchViewModel.isRunning.value == true) {
                stopwatchViewModel.pauseStopwatch()
                prefs.edit().putLong(KEY_ELAPSED_TIME, stopwatchViewModel.elapsedTime.value ?: 0L)
                    .apply()
            } else {
                val isInProgress = prefs.getBoolean(KEY_IN_PROGRESS, false)
                if (isInProgress) {
                    val savedElapsedTime = prefs.getLong(KEY_ELAPSED_TIME, 0L)
                    stopwatchViewModel.startStopwatch(savedElapsedTime)
                } else {
                    Toast.makeText(requireContext(), "시작할 운동이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.completeButton.setOnClickListener {
            val savedScheduleId = prefs.getLong(KEY_SAVED_SCHEDULE_ID, -1L)
            if (savedScheduleId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response =
                            RetrofitClient.scheduleApi.markScheduleAsComplete(savedScheduleId)
                        if (response.isSuccessful) {
                            Log.d(
                                "ExerciseFragment",
                                "운동(scheduleId: $savedScheduleId) 완료 처리 성공 (서버)"
                            )
                            clearInProgressState()
                            withContext(Dispatchers.Main) {
                                if (_binding == null) return@withContext
                                loadTodayPlanFromServer()
                                Toast.makeText(requireContext(), "운동을 완료했습니다!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if (_binding == null) return@withContext
                                Log.e(
                                    "ExerciseFragment",
                                    "운동 완료 처리 실패: ${response.code()} ${response.message()}"
                                )
                                Toast.makeText(
                                    requireContext(),
                                    "운동 완료 처리에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (_binding == null) return@withContext
                            Log.e("ExerciseFragment", "운동 완료 처리 중 오류", e)
                            Toast.makeText(
                                requireContext(),
                                "운동 완료 처리 중 오류가 발생했습니다.",
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
        if (planId == -1L) {
            Toast.makeText(requireContext(), "오늘의 운동 계획을 먼저 로드해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val firstIncompleteIndex = currentScheduleList.indexOfFirst { !it.is_completed }
        if (firstIncompleteIndex != -1) {
            navigateToExerciseDoingFragment(
                firstIncompleteIndex,
                currentScheduleList[firstIncompleteIndex],
                false
            )
        } else if (currentScheduleList.isNotEmpty()) {
            Toast.makeText(requireContext(), "오늘의 모든 운동을 완료했습니다!", Toast.LENGTH_SHORT).show()
        } else {
            if (planId != -1L) {
                val args = Bundle().apply { putLong("planId", planId) }
                findNavController().navigate(R.id.action_exercise_to_exerciseAdd, args)
            } else {
                Toast.makeText(requireContext(), "오늘 진행할 운동 계획이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToExerciseDoingFragment(
        index: Int,
        schedule: ScheduleDto,
        isContinuing: Boolean
    ) {
        if (planId != -1L && index < currentScheduleList.size && index >= 0) {
            val targetSchedule = currentScheduleList[index]

            val args = Bundle().apply {
                putLong("planId", planId)
                putInt("initialExerciseIndex", index)
                putLong("scheduleId", targetSchedule.schedule_id.toLong())
                putLong("exerciseId", targetSchedule.exercise_id.toLong())
                putString("exerciseName", targetSchedule.exercise_name)
                putString("imagePath", targetSchedule.image_path)
                putString("equip", targetSchedule.equip)
            }

            val editor = prefs.edit()
            editor.putLong(KEY_SAVED_PLAN_ID, planId)
            editor.putInt(KEY_SAVED_EXERCISE_INDEX, index)
            editor.putLong(KEY_SAVED_SCHEDULE_ID, targetSchedule.schedule_id.toLong())
            editor.putLong(KEY_SAVED_EXERCISE_ID, targetSchedule.exercise_id.toLong())
            editor.putString(KEY_SAVED_EXERCISE_NAME, targetSchedule.exercise_name)
            editor.putString(KEY_SAVED_IMAGE_PATH, targetSchedule.image_path) // ★ 저장
            editor.putString(KEY_SAVED_EQUIP, targetSchedule.equip)
            editor.putBoolean(KEY_IN_PROGRESS, true)

            if (!isContinuing) {
                editor.putLong(KEY_ELAPSED_TIME, 0L)
                Log.d("ExerciseFragment", "Navigating to new exercise, resetting stopwatch.")
            } else {
                editor.putLong(
                    KEY_ELAPSED_TIME,
                    stopwatchViewModel.elapsedTime.value ?: prefs.getLong(KEY_ELAPSED_TIME, 0L)
                )
                Log.d(
                    "ExerciseFragment",
                    "Navigating to continue exercise, elapsed time: ${stopwatchViewModel.elapsedTime.value}"
                )
            }
            editor.apply()

            findNavController().navigate(R.id.action_exercise_to_exerciseDoing, args)
        } else {
            Toast.makeText(requireContext(), "운동 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::serverAdapter.isInitialized) {
            binding.exerciseListRecyclerView.adapter = null
        }
        _binding = null
    }
}