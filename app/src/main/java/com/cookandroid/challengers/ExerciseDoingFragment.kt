package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.databinding.FragmentExerciseDoingBinding
import com.cookandroid.challengers.databinding.ItemExerciseSetBinding
import com.cookandroid.challengers.network.dto.ServerExerciseSet
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseDoingFragment : Fragment() {

    private var _binding: FragmentExerciseDoingBinding? = null
    private val binding get() = _binding!!

    private lateinit var setAdapter: ExerciseSetAdapter
    private var currentSetIndex = 0
    private var exercisePlanId: Long = -1L
    private var currentExerciseIndex = 0
    private var currentExerciseId: Long = -1L
    private var scheduleId: Long = -1L
    private var setStartTime: Long = 0L

    private var currentExerciseSets: MutableList<ServerExerciseSet> = mutableListOf()

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    companion object {
        private const val PREFS_PROGRESS = "exercise_progress"
        private const val KEY_EXERCISE_INDEX = "current_exercise_index"
        private const val KEY_SET_INDEX = "current_set_index"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_IN_PROGRESS = "is_in_progress"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
    }

    private var isInProgress: Boolean
        get() = prefs.getBoolean(KEY_IN_PROGRESS, false)
        set(v) = prefs.edit().putBoolean(KEY_IN_PROGRESS, v).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exercisePlanId = arguments?.getLong("planId") ?: -1L
        currentExerciseIndex = arguments?.getInt("initialExerciseIndex") ?: 0
        currentSetIndex = 0
        setStartTime = System.currentTimeMillis()
        isInProgress = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDoingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter = ExerciseSetAdapter()
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setAdapter
        }

        setupListeners()
        setupStopwatch()

        loadTodayPlanAndStart()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.pauseButton.setOnClickListener {
            if (stopwatchViewModel.isRunning.value == true) stopwatchViewModel.pauseStopwatch()
            else stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
        }
        binding.completeSetButton.setOnClickListener {
            if (stopwatchViewModel.isRunning.value == false) {
                stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
            }
            completeCurrentSet()
        }
    }

    private fun setupStopwatch() {
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
        }
        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.pauseButton.setImageResource(
                if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black
            )
        }
    }

    private fun loadTodayPlanAndStart() {
        lifecycleScope.launch(Dispatchers.IO) {
            val plan = RetrofitClient.scheduleApi.getTodayPlan(userId = 21).body()
            val currentSchedule = plan?.schedules?.getOrNull(currentExerciseIndex)
            if (currentSchedule != null) {
                currentExerciseId = currentSchedule.exercise_id.toLong()
                currentExerciseId = currentSchedule.exercise_id.toLong()

                val exerciseInfo = RetrofitClient.scheduleApi.getExerciseInfo(scheduleId).body()

                val sets = RetrofitClient.scheduleApi.getRepsSets(scheduleId).execute().body() ?: emptyList()

                currentExerciseSets = sets.map {
                    ServerExerciseSet(
                        setNumber = it.setNumber,
                        weight = it.weight.toInt(),
                        reps = it.reps,
                        isCompleted = it.isCompleted, // ✅ 이제 정상 작동
                        isHighlighted = false
                    )
                }.toMutableList()
                val firstIncomplete = currentExerciseSets.indexOfFirst { !it.isCompleted }.coerceAtLeast(0)
                val highlighted = currentExerciseSets.mapIndexed { idx, set ->
                    set.copy(isHighlighted = idx == firstIncomplete)
                }

                withContext(Dispatchers.Main) {
                    binding.exerciseNameTextView.text = exerciseInfo?.name ?: "운동 이름"
                    setAdapter.submitList(highlighted)
                    binding.setsRecyclerView.scrollToPosition(firstIncomplete)
                }
            }
        }
    }

    private fun completeCurrentSet() {
        if (currentSetIndex >= currentExerciseSets.size) return

        val setNumber = currentExerciseSets[currentSetIndex].setNumber

        lifecycleScope.launch(Dispatchers.IO) {
            val res = RetrofitClient.scheduleApi.completeSet(
                scheduleId,
                RetrofitClient.CompleteSetRequest(setNumber)
            ).execute()

            if (res.isSuccessful && res.body()?.isWorkoutCompleted == true) {
                withContext(Dispatchers.Main) {
                    Log.d("ExerciseDoing", "🎉 운동 완료! 다음 운동으로 이동")
                    moveToNextExercise()
                }
            } else {
                withContext(Dispatchers.Main) {
                    markSetAsCompleted()
                }
            }
        }
    }

    private fun markSetAsCompleted() {
        val now = System.currentTimeMillis()
        val updated = currentExerciseSets.mapIndexed { idx, set ->
            when (idx) {
                currentSetIndex -> set.copy(isCompleted = true, isHighlighted = false)
                currentSetIndex + 1 -> set.copy(isHighlighted = true)
                else -> set.copy(isHighlighted = false)
            }
        }
        currentSetIndex++
        setStartTime = now
        prefs.edit().putInt(KEY_SET_INDEX, currentSetIndex).putLong(KEY_START_TIME, now).apply()
        setAdapter.submitList(updated)
    }

    private fun moveToNextExercise() {
        val nextIdx = currentExerciseIndex + 1
        findNavController().navigate(
            R.id.action_exerciseDoing_to_exerciseDoing, // 정확한 액션 ID로 대체
            Bundle().apply {
                putLong("planId", exercisePlanId)
                putInt("initialExerciseIndex", nextIdx)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseSetAdapter :
        ListAdapter<ServerExerciseSet, ExerciseSetAdapter.ViewHolder>(object :
            DiffUtil.ItemCallback<ServerExerciseSet>() {
            override fun areItemsTheSame(old: ServerExerciseSet, new: ServerExerciseSet) =
                old.setNumber == new.setNumber

            override fun areContentsTheSame(old: ServerExerciseSet, new: ServerExerciseSet) =
                old == new
        }) {

        inner class ViewHolder(private val b: ItemExerciseSetBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(s: ServerExerciseSet) { // ✅ 여기 꼭 ServerExerciseSet!
                b.setNumberTextView.text = "${s.setNumber}세트"
                b.repsTextView.text = "${s.reps}회"
                b.weightTextView.text = "${s.weight}kg"
                b.completionCheckImageView.alpha = if (s.isCompleted) 1f else 0f
                b.root.background = ContextCompat.getDrawable(
                    b.root.context,
                    when {
                        s.isCompleted -> R.drawable.set_item_background_completed
                        s.isHighlighted -> R.drawable.set_item_background_emphasized
                        else -> R.drawable.set_item_background
                    }
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemExerciseSetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
    }
}




//
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
//import com.cookandroid.challengers.data.ExerciseInPlan
//import com.cookandroid.challengers.data.ExercisePlanDao
//import com.cookandroid.challengers.data.ExerciseSet
//import com.cookandroid.challengers.data.ExerciseSetDao
//import com.cookandroid.challengers.data.PlanDetailDao
//import com.cookandroid.challengers.data.db.AppDatabase
//import com.cookandroid.challengers.databinding.FragmentExerciseDoingBinding
//import com.cookandroid.challengers.databinding.ItemExerciseSetBinding
//import com.cookandroid.challengers.viewmodel.StopwatchViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext

//class ExerciseDoingFragment : Fragment() {
//
//    private var _binding: FragmentExerciseDoingBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var setAdapter: ExerciseSetAdapter
//    private var currentSetIndex = 0
//    private var exercisePlanId: Long = -1L
//    private lateinit var exerciseSetDao: ExerciseSetDao
//    private lateinit var planDao: ExercisePlanDao
//    private lateinit var planDetailDao: PlanDetailDao
//
//    private var currentExerciseIndex = 0
//    private var planExerciseList: List<ExerciseInPlan> = emptyList()
//    private var currentExerciseId: Long = -1L
//    private var setStartTime: Long = 0L // 세트 시작 시간
//
//    private var currentExerciseSets: MutableList<ExerciseSet> = mutableListOf()
//
//    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()
//
//    companion object {
//        private const val PREFS_PROGRESS = "exercise_progress"
//        private const val KEY_PLAN_ID = "current_plan_id"
//        private const val KEY_EXERCISE_INDEX = "current_exercise_index"
//        private const val KEY_SET_INDEX = "current_set_index"
//        private const val KEY_START_TIME = "start_time" // setStartTime 저장을 위한 키
//        private const val KEY_IN_PROGRESS = "is_in_progress"
//    }
//
//    private val prefs by lazy {
//        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
//    }
//
//    private var isInProgress: Boolean
//        get() = prefs.getBoolean(KEY_IN_PROGRESS, false)
//        set(v) = prefs.edit().putBoolean(KEY_IN_PROGRESS, v).apply()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // planId는 계속 받기
//        exercisePlanId = arguments?.getLong("planId") ?: -1L
//        Log.d("ExerciseDoingFragment", "Received planId: $exercisePlanId")
//
//        // 네비게이션으로 넘어온 인덱스가 있는지 확인
//        val navIndex = arguments?.getInt("initialExerciseIndex")
//        if (navIndex != null) {
//            // 리스트 클릭이나 첫 진입 시 전달된 값: 새로운 운동 시작
//            currentExerciseIndex = navIndex
//            Log.d("ExerciseDoingFragment", "New exercise started. Initial currentExerciseIndex: $currentExerciseIndex. setStartTime: $setStartTime")
//            currentSetIndex = 0
//            setStartTime = System.currentTimeMillis() // 새로운 운동 시작 시점 기록
//            isInProgress = true // 운동 시작
//            prefs.edit()
//                .putLong(KEY_START_TIME, setStartTime)
//                .putInt(KEY_EXERCISE_INDEX, currentExerciseIndex)
//                .putInt(KEY_SET_INDEX, currentSetIndex)
//                .putBoolean(KEY_IN_PROGRESS, isInProgress)
//                .apply()
//            Log.d("ExerciseDoingFragment", "New exercise started. Initial setStartTime: $setStartTime")
//
//        } else if (savedInstanceState != null) {
//            // 화면 회전 등 프래그먼트 상태 복구
//            currentExerciseIndex = savedInstanceState.getInt(KEY_EXERCISE_INDEX, 0)
//            currentSetIndex = savedInstanceState.getInt(KEY_SET_INDEX, 0)
//            setStartTime = savedInstanceState.getLong(KEY_START_TIME, System.currentTimeMillis())
//            isInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS, false)
//            Log.d("ExerciseDoingFragment", "Restoring from savedInstanceState. setStartTime: $setStartTime")
//
//        } else {
//            // 이전 진행 상황 (앱 재시작 등) 복구
//            currentExerciseIndex = prefs.getInt(KEY_EXERCISE_INDEX, 0)
//            currentSetIndex = prefs.getInt(KEY_SET_INDEX, 0)
//            setStartTime = prefs.getLong(KEY_START_TIME, 0L) // prefs에서 setStartTime 복원, 없으면 0L
//            isInProgress = prefs.getBoolean(KEY_IN_PROGRESS, false)
//            Log.d("ExerciseDoingFragment", "Restoring from prefs. setStartTime: $setStartTime")
//
//        }
//
//        // 운동이 진행 중인데 setStartTime이 초기값이라면 현재 시간으로 보정
//        // (예: 앱이 강제 종료되어 setStartTime이 저장되지 않았을 경우)
//        if (isInProgress && setStartTime == 0L) {
//            setStartTime = System.currentTimeMillis()
//            prefs.edit().putLong(KEY_START_TIME, setStartTime).apply()
//            Log.w("ExerciseDoingFragment", "setStartTime was 0L while in progress, corrected to: $setStartTime")
//        }
//    }
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentExerciseDoingBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
//        exerciseSetDao = db.exerciseSetDao()
//        planDao = db.exercisePlanDao()
//        planDetailDao = db.planDetailDao()
//
//        setAdapter = ExerciseSetAdapter()
//        binding.setsRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = setAdapter
//        }
//
//        loadPlanExercises {
//            updateExerciseInfo()
//            fetchSetsForCurrentExercise()
//        }
//
//        setupStopwatch()
//        setupListeners()
//        updateExerciseProgressUI()
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putInt(KEY_EXERCISE_INDEX, currentExerciseIndex)
//        outState.putInt(KEY_SET_INDEX, currentSetIndex)
//        outState.putBoolean(KEY_IN_PROGRESS, isInProgress)
//        outState.putLong(KEY_START_TIME, setStartTime) // setStartTime 저장
//    }
//
//    private fun setupStopwatch() {
//        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
//            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
//            // 스톱워치가 0보다 크고 isInProgress가 false이면, 운동이 시작된 것으로 간주
//            if (time > 0L && !isInProgress) {
//                isInProgress = true
//                prefs.edit().putBoolean(KEY_IN_PROGRESS, true).apply()
//            }
//        }
//        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { running ->
//            binding.pauseButton.setImageResource(
//                if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black
//            )
//        }
//        // 스톱워치 ViewModel의 초기 상태에 따라 스톱워치 시작 또는 복원
//        // 만약 isInProgress가 true인데 스톱워치가 멈춰있다면 재시작 (앱 재시작 시)
//        if (isInProgress && !stopwatchViewModel.isRunning.value!!) {
//            stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
//        } else if (!isInProgress && stopwatchViewModel.elapsedTime.value == 0L) {
//            // 운동이 진행 중이 아니고, 스톱워치 시간도 0이라면 (새로운 운동 시작 시)
//            stopwatchViewModel.startStopwatch() // 새롭게 시작
//        }
//    }
//
//    private fun setupListeners() {
//        binding.backButton.setOnClickListener { findNavController().popBackStack() }
//        binding.pauseButton.setOnClickListener {
//            if (stopwatchViewModel.isRunning.value == true) stopwatchViewModel.pauseStopwatch()
//            else stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
//        }
//        binding.completeSetButton.setOnClickListener {
//            // 세트 완료 버튼 클릭 시 스톱워치가 멈춰있다면 다시 시작
//            if (stopwatchViewModel.isRunning.value == false) {
//                stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
//            }
//            completeCurrentSet()
//        }
//        binding.restTimerButton.setOnClickListener { showRestTimer() }
//        binding.addSetButton.setOnClickListener { addNewSet() }
//        binding.editSetButton.setOnClickListener { showEditSetBottomSheet() }
//    }
//
//    private fun loadPlanExercises(onComplete: () -> Unit) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            planExerciseList = planDao.getExercisesInPlan(exercisePlanId).sortedBy { it.order }
//            Log.d("ExerciseDoingFragment", "Loaded planExerciseList size: ${planExerciseList.size}")
//            Log.d("ExerciseDoingFragment", "Loaded planExerciseList (sorted by order): ${planExerciseList.map { it.exercise.name + " (order: ${it.order}), isCompleted: ${it.isCompleted}" }}") // 디버그용 로그에 isCompleted도 추가
//            withContext(Dispatchers.Main) {
//                // 여기서 currentExerciseIndex에 해당하는 운동 이름을 다시 로그로 찍어 확인
//                val currentExerciseName = planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.name
//                val currentExerciseOrder = planExerciseList.getOrNull(currentExerciseIndex)?.order
//                Log.d("ExerciseDoingFragment", "After loading planExerciseList, currentExerciseIndex ($currentExerciseIndex) points to: $currentExerciseName (order: $currentExerciseOrder)")
//                onComplete()
//            }
//        }
//    }
//
//    private fun fetchSetsForCurrentExercise() {
//        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
//            currentExerciseId = ex.id
//            lifecycleScope.launch(Dispatchers.IO) {
//                //val sets = exerciseSetDao.getSetsByPlanAndExerciseId(exercisePlanId, ex.id) //여기 룸! 수정!
//                    .sortedBy { it.setNumber }
//                currentExerciseSets = sets.toMutableList()
//
//                // 첫 번째 미완료 세트 인덱스 계산
//                val firstUncompleted = sets.firstOrNull { !it.isCompleted }
//                val startIdx = firstUncompleted?.let { sets.indexOf(it) } ?: 0
//
//                val highlighted = sets.mapIndexed { idx, s ->
//                    s.copy(isHighlighted = (idx == startIdx))
//                }
//
//                withContext(Dispatchers.Main) {
//                    // _binding이 null이 아닌지 확인하여 UI 업데이트 (이전 버그 수정 반영)
//                    _binding?.let { currentBinding ->
//                        setAdapter.submitList(highlighted) {
//                            currentBinding.setsRecyclerView.scrollToPosition(startIdx)
//                            // setStartTime 초기화 로직은 onCreate에서 대부분 처리되므로 여기서는 제거
//                            // 단, 필요에 따라 첫 세트가 강조될 때 setStartTime을 여기서 다시 설정할 수도 있으나,
//                            // onCreate에서 복원 또는 초기화된 setStartTime이 다음 세트 완료 시에만 업데이트되도록 하는 것이 더 논리적입니다.
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun updateExerciseInfo() {
//        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
//            binding.exerciseNameTextView.text = ex.name
//            val resId = resources.getIdentifier(
//                ex.imagePath ?: "",
//                "drawable",
//                requireContext().packageName
//            )
//            Glide.with(requireContext())
//                .load(if (resId != 0) resId else R.drawable.ic_launcher_background)
//                .transition(withCrossFade())
//                .into(binding.exerciseImageView)
//            setAdapter.updateEquip(ex.equip)
//            binding.titleTextView.text = "오늘의 운동 중"
//        }
//    }
//
//    private fun completeCurrentSet() {
//        if (currentSetIndex < currentExerciseSets.size) {
//            val now = System.currentTimeMillis()
//            val elapsed = now - setStartTime // 세트 수행 시간 계산
//            Log.d("ExerciseDoingFragment", "Completing set ${currentSetIndex + 1}. Elapsed time: $elapsed ms")
//
//            val completed =
//                currentExerciseSets[currentSetIndex].copy(
//                    isCompleted = true,
//                    isHighlighted = false,
//                    elapsedTimeMillis = elapsed // 수행 시간 기록
//                )
//            currentExerciseSets[currentSetIndex] = completed
//
//            val nextIdx = currentSetIndex + 1
//            val highlightNext = nextIdx < currentExerciseSets.size
//            val updated = currentExerciseSets.mapIndexed { idx, s ->
//                s.copy(isHighlighted = (highlightNext && idx == nextIdx))
//            }
//
//            setAdapter.submitList(updated) {
//                // DB 업데이트
//                lifecycleScope.launch(Dispatchers.IO) {
//                    //exerciseSetDao.update(completed) //여기 룸!
//                }
//                currentSetIndex = nextIdx
//                prefs.edit().putInt(KEY_SET_INDEX, nextIdx).apply()
//                updateProgressText()
//                showRestTimer(autoStart = true)
//            }
//        }
//    }
//
//    private fun addNewSet() {
//        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
//            lifecycleScope.launch(Dispatchers.IO) {
//                val nextNo = (currentExerciseSets.maxByOrNull { it.setNumber }?.setNumber ?: 0) + 1
//                val newSet = ExerciseSet(
//                    id = 0L,
//                    exercisePlanId = exercisePlanId,
//                    exerciseId = ex.id,
//                    setNumber = nextNo,
//                    weight = currentExerciseSets.lastOrNull()?.weight ?: 0,
//                    reps = currentExerciseSets.lastOrNull()?.reps ?: 0,
//                    times = currentExerciseSets.lastOrNull()?.times ?: 0,
//                    isCompleted = false
//                )
//                //val insertedId = exerciseSetDao.insert(newSet) //여기 룸! 수정!
//                if (insertedId > 0) {
//                    withContext(Dispatchers.Main) {
//                        currentExerciseSets.add(newSet.copy(id = insertedId))
//                        val updatedList = currentExerciseSets
//                            .mapIndexed { idx, s -> s.copy(isHighlighted = (idx == currentSetIndex)) }
//                            .sortedBy { it.setNumber }
//                        setAdapter.submitList(updatedList) {
//                            updateProgressText()
//                            binding.setsRecyclerView.scrollToPosition(updatedList.size - 1)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun showEditSetBottomSheet() {
//        val equip = planExerciseList
//            .getOrNull(currentExerciseIndex)
//            ?.exercise?.equip?.trim()
//
//        val sheet = ExerciseEditSetFragment.newInstance(
//            exercisePlanId,                   // planId
//            currentExerciseId,                // exerciseId
//            currentExerciseSets.toList(),     // initialSetList
//            currentSetIndex,                  // highlightIndex
//            equip                             // equip
//        )
//        sheet.show(childFragmentManager, ExerciseEditSetFragment.TAG)
//    }
//
//    private fun updateProgressText() {
//        val cur = currentExerciseIndex + 1
//        val total = planExerciseList.size
//        binding.exerciseProgressTextView.text = "$cur/$total"
//    }
//
//    private fun showRestTimer(autoStart: Boolean = false) {
//        val allDone = (currentSetIndex == setAdapter.currentList.size)
//        val sheet = RestTimerFragment.newInstance(autoStart)
//        sheet.setOnTimerFinishedListener {
//            if (!allDone) {
//                // 다음 세트 강조
//                val nextIdx = currentSetIndex
//                val list = setAdapter.currentList.mapIndexed { idx, s ->
//                    s.copy(isHighlighted = (idx == nextIdx))
//                }.toMutableList()
//                // 이전 세트의 강조 표시 제거
//                if (currentSetIndex > 0 && currentSetIndex < list.size) { // currentSetIndex는 이미 nextIdx로 업데이트된 상태
//                    list[currentSetIndex - 1] = // 이전 세트의 실제 인덱스
//                        list[currentSetIndex - 1].copy(isHighlighted = false)
//                }
//                setAdapter.submitList(list) {
//                    setStartTime = System.currentTimeMillis() // 다음 세트 진입 시간 기록
//                    prefs.edit().putLong(KEY_START_TIME, setStartTime).putInt(KEY_SET_INDEX, nextIdx).apply() // prefs에도 저장
//                    updateProgressText()
//                    Log.d("ExerciseDoingFragment", "Rest timer finished. Next set start time: $setStartTime")
//                }
//            } else {
//                completeCurrentExercise()
//            }
//        }
//        sheet.show(childFragmentManager, RestTimerFragment.TAG)
//    }
//
//    private fun completeCurrentExercise() {
//        val nextIdx = currentExerciseIndex + 1
//        lifecycleScope.launch(Dispatchers.IO) {
//            planDetailDao.updateCompletion(exercisePlanId, currentExerciseId, true)
//            if (nextIdx < planExerciseList.size) {
//                // 다음 운동으로 넘어갈 때, 새로운 운동의 첫 세트 시작 시간으로 setStartTime을 초기화
//                val newSetStartTime = System.currentTimeMillis()
//                prefs.edit()
//                    .putInt(KEY_EXERCISE_INDEX, nextIdx)
//                    .putInt(KEY_SET_INDEX, 0)
//                    .putLong(KEY_START_TIME, newSetStartTime) // 다음 운동의 setStartTime을 미리 저장
//                    .apply()
//                withContext(Dispatchers.Main) {
//                    currentExerciseIndex = nextIdx
//                    currentSetIndex = 0
//                    setStartTime = newSetStartTime // UI 스레드에서도 반영
//                    updateExerciseInfo()
//                    fetchSetsForCurrentExercise()
//                    Log.d("ExerciseDoingFragment", "Moving to next exercise. New setStartTime: $setStartTime")
//                }
//            } else {
//                // 모든 운동 완료
//                prefs.edit().putBoolean(KEY_IN_PROGRESS, false).apply()
//                withContext(Dispatchers.Main) {
//                    findNavController().navigate(R.id.action_exerciseDoing_to_coolDownStretch)
//                }
//            }
//        }
//    }
//
//    private fun updateExerciseProgressUI() {
//        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let {
//            binding.exerciseNameTextView.text = it.name
//        }
//        binding.stopwatchTextView.text =
//            stopwatchViewModel.formatElapsedTime(stopwatchViewModel.elapsedTime.value ?: 0L)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    private class ExerciseSetAdapter(
//        private val onSetClick: ((ExerciseSet) -> Unit)? = null
//    ) : ListAdapter<ExerciseSet, ExerciseSetAdapter.ViewHolder>(object :
//        DiffUtil.ItemCallback<ExerciseSet>() {
//        override fun areItemsTheSame(old: ExerciseSet, new: ExerciseSet) = old.id == new.id
//        override fun areContentsTheSame(old: ExerciseSet, new: ExerciseSet) = old == new
//    }) {
//        private var currentEquip: String? = null
//        fun updateEquip(e: String?) {
//            currentEquip = e
//            notifyDataSetChanged()
//        }
//
//        fun updateSetNumbers(list: MutableList<ExerciseSet>) {
//            list.forEachIndexed { idx, s -> s.setNumber = idx + 1 }
//        }
//
//        inner class ViewHolder(val b: ItemExerciseSetBinding) : RecyclerView.ViewHolder(b.root) {
//            init {
//                b.root.setOnClickListener {
//                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
//                        onSetClick?.invoke(getItem(pos))
//                    }
//                }
//            }
//
//            fun bind(s: ExerciseSet) {
//                b.setNumberTextView.text = "${s.setNumber}세트"
//                b.repsTextView.text = "${s.reps}회"
//
//                val lp = b.repsTextView.layoutParams as ViewGroup.MarginLayoutParams
//                if (currentEquip in listOf("맨몸", "스텝박스", "세라밴드", "짐볼")) {
//                    b.weightTextView.visibility = View.GONE
//                    b.dividerImageView.visibility = View.GONE
//                    // repsTextView 오른쪽 마진 24dp 로 설정
//                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
//                        R.dimen.item_reps_margin_end_no_weight
//                    )
//                } else {
//                    b.weightTextView.visibility = View.VISIBLE
//                    b.dividerImageView.visibility = View.VISIBLE
//                    b.weightTextView.text = s.weight?.let { "${it}kg" } ?: "-"
//                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
//                        R.dimen.item_reps_margin_end_default
//                    )
//                }
//                b.completionCheckImageView.alpha = if (s.isCompleted) 1f else 0f
//                b.root.background = ContextCompat.getDrawable(
//                    b.root.context,
//                    when {
//                        s.isCompleted -> R.drawable.set_item_background_completed
//                        s.isHighlighted -> R.drawable.set_item_background_emphasized
//                        else -> R.drawable.set_item_background
//                    }
//                )
//            }
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
//            ViewHolder(
//                ItemExerciseSetBinding.inflate(
//                    LayoutInflater.from(parent.context),
//                    parent,
//                    false
//                )
//            )
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
//            holder.bind(getItem(position))
//    }
//}