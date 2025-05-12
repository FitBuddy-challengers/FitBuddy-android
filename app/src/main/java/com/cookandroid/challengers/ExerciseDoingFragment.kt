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
import com.cookandroid.challengers.data.ExerciseInPlan
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseDoingBinding
import com.cookandroid.challengers.databinding.ItemExerciseSetBinding
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
    private lateinit var exerciseSetDao: ExerciseSetDao
    private lateinit var planDao: ExercisePlanDao
    private lateinit var planDetailDao: PlanDetailDao

    private var currentExerciseIndex = 0
    private var planExerciseList: List<ExerciseInPlan> = emptyList()
    private var currentExerciseId: Long = -1L
    private var currentExerciseSets: MutableList<ExerciseSet> = mutableListOf()

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    companion object {
        private const val PREFS_PROGRESS = "exercise_progress"
        private const val KEY_PLAN_ID = "current_plan_id"
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

        // planId는 계속 받기
        exercisePlanId = arguments?.getLong("planId") ?: -1L

        // 네비게이션으로 넘어온 인덱스가 있는지 확인
        val navIndex = arguments?.getInt("initialExerciseIndex")
        if (navIndex != null) {
            // 리스트 클릭이나 첫 진입 시 전달된 값
            currentExerciseIndex = navIndex
            currentSetIndex = 0
        } else if (savedInstanceState != null) {
            // 화면 회전 등 복구
            currentExerciseIndex = savedInstanceState.getInt(KEY_EXERCISE_INDEX, 0)
            currentSetIndex = savedInstanceState.getInt(KEY_SET_INDEX, 0)
        } else {
            // 이전 진행 상황(앱 재시작 등)
            currentExerciseIndex = prefs.getInt(KEY_EXERCISE_INDEX, 0)
            currentSetIndex = prefs.getInt(KEY_SET_INDEX, 0)
        }

        // 진행 중 여부는 항상 복원
        isInProgress = prefs.getBoolean(KEY_IN_PROGRESS, false)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDoingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        exerciseSetDao = db.exerciseSetDao()
        planDao = db.exercisePlanDao()
        planDetailDao = db.planDetailDao()

        setAdapter = ExerciseSetAdapter()
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setAdapter
        }

        loadPlanExercises {
            updateExerciseInfo()
            fetchSetsForCurrentExercise()
        }

        setupStopwatch()
        setupListeners()
        updateExerciseProgressUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_EXERCISE_INDEX, currentExerciseIndex)
        outState.putInt(KEY_SET_INDEX, currentSetIndex)
        outState.putBoolean(KEY_IN_PROGRESS, isInProgress)
    }

    private fun setupStopwatch() {
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
            if (time > 0L && !isInProgress) isInProgress = true
        }
        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.pauseButton.setImageResource(
                if (running) R.drawable.ic_pause_black else R.drawable.ic_play_black
            )
        }
        if (stopwatchViewModel.elapsedTime.value == 0L && !stopwatchViewModel.isRunning.value!!) {
            stopwatchViewModel.startStopwatch()
        }
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
        binding.restTimerButton.setOnClickListener { showRestTimer() }
        binding.addSetButton.setOnClickListener { addNewSet() }
        binding.editSetButton.setOnClickListener { showEditSetBottomSheet() }
    }

    private fun loadPlanExercises(onComplete: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            planExerciseList = planDao.getExercisesInPlan(exercisePlanId)
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    private fun fetchSetsForCurrentExercise() {
        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
            currentExerciseId = ex.id
            lifecycleScope.launch(Dispatchers.IO) {
                val sets = exerciseSetDao.getSetsByExerciseId(ex.id)
                    .sortedBy { it.setNumber }
                currentExerciseSets = sets.toMutableList()

                // 첫 번째 미완료 세트 인덱스 계산
                val firstUncompleted = sets.firstOrNull { !it.isCompleted }
                val startIdx = firstUncompleted?.let { sets.indexOf(it) } ?: 0

                val highlighted = sets.mapIndexed { idx, s ->
                    s.copy(isHighlighted = (idx == startIdx))
                }

                withContext(Dispatchers.Main) {
                    setAdapter.submitList(highlighted) {
                        binding.setsRecyclerView.scrollToPosition(startIdx)
                    }
                }
            }
        }
    }

    private fun updateExerciseInfo() {
        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
            binding.exerciseNameTextView.text = ex.name
            val resId = resources.getIdentifier(ex.imagePath ?: "", "drawable", requireContext().packageName)
            Glide.with(requireContext())
                .load(if (resId != 0) resId else R.drawable.ic_launcher_background)
                .transition(withCrossFade())
                .into(binding.exerciseImageView)
            setAdapter.updateEquip(ex.equip)
            binding.titleTextView.text = "오늘의 운동 중"
        }
    }

    private fun completeCurrentSet() {
        if (currentSetIndex < currentExerciseSets.size) {
            val completed = currentExerciseSets[currentSetIndex].copy(isCompleted = true, isHighlighted = false)
            currentExerciseSets[currentSetIndex] = completed

            val nextIdx = currentSetIndex + 1
            val highlightNext = nextIdx < currentExerciseSets.size
            val updated = currentExerciseSets.mapIndexed { idx, s ->
                s.copy(isHighlighted = (highlightNext && idx == nextIdx))
            }

            setAdapter.submitList(updated) {
                // DB 업데이트
                lifecycleScope.launch(Dispatchers.IO) {
                    exerciseSetDao.update(completed)
                }
                currentSetIndex = nextIdx
                prefs.edit().putInt(KEY_SET_INDEX, nextIdx).apply()
                updateProgressText()
                showRestTimer(autoStart = true)
            }
        }
    }

    private fun addNewSet() {
        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let { ex ->
            lifecycleScope.launch(Dispatchers.IO) {
                val nextNo = (currentExerciseSets.maxByOrNull { it.setNumber }?.setNumber ?: 0) + 1
                val newSet = ExerciseSet(
                    id = 0,
                    exerciseId = ex.id,
                    setNumber = nextNo,
                    weight = currentExerciseSets.lastOrNull()?.weight ?: 0,
                    reps = currentExerciseSets.lastOrNull()?.reps ?: 0
                )
                val inserted = exerciseSetDao.insert(newSet)
                if (inserted > 0) {
                    withContext(Dispatchers.Main) {
                        currentExerciseSets.add(newSet.copy(id = inserted))
                        val list = currentExerciseSets.mapIndexed { idx, s ->
                            s.copy(isHighlighted = (idx == currentSetIndex))
                        }.sortedBy { it.setNumber }
                        setAdapter.submitList(list) {
                            updateProgressText()
                            binding.setsRecyclerView.scrollToPosition(list.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun showEditSetBottomSheet() {
        val sheet = ExerciseEditSetFragment.newInstance(
            currentExerciseId, currentExerciseSets.toList(), currentSetIndex
        )
        sheet.setOnSetsUpdatedListener { updated ->
            currentExerciseSets = updated.toMutableList()
            (binding.setsRecyclerView.adapter as? ExerciseSetAdapter)
                ?.updateSetNumbers(currentExerciseSets)
            val list = currentExerciseSets.mapIndexed { idx, s ->
                s.copy(isHighlighted = (idx == currentSetIndex))
            }
            setAdapter.submitList(list) { updateProgressText() }
        }
        sheet.show(childFragmentManager, ExerciseEditSetFragment.TAG)
    }

    private fun updateProgressText() {
        val cur = currentExerciseIndex + 1
        val total = planExerciseList.size
        binding.exerciseProgressTextView.text = "$cur/$total"
    }

    private fun showRestTimer(autoStart: Boolean = false) {
        val allDone = (currentSetIndex == setAdapter.currentList.size)
        val sheet = RestTimerFragment.newInstance(autoStart)
        sheet.setOnTimerFinishedListener {
            if (!allDone) {
                // 다음 세트 강조
                val nextIdx = currentSetIndex
                val list = setAdapter.currentList.mapIndexed { idx, s ->
                    s.copy(isHighlighted = (idx == nextIdx))
                }.toMutableList()
                if (currentSetIndex > 0 && currentSetIndex < list.size) {
                    list[currentSetIndex - 1] =
                        list[currentSetIndex - 1].copy(isHighlighted = false)
                }
                setAdapter.submitList(list) {
                    prefs.edit().putInt(KEY_SET_INDEX, nextIdx).apply()
                    updateProgressText()
                }
            } else {
                completeCurrentExercise()
            }
        }
        sheet.show(childFragmentManager, RestTimerFragment.TAG)
    }

    private fun completeCurrentExercise() {
        val nextIdx = currentExerciseIndex + 1
        lifecycleScope.launch(Dispatchers.IO) {
            planDetailDao.updateCompletion(exercisePlanId, currentExerciseId, true)
            if (nextIdx < planExerciseList.size) {
                prefs.edit()
                    .putInt(KEY_EXERCISE_INDEX, nextIdx)
                    .putInt(KEY_SET_INDEX, 0)
                    .putLong(KEY_START_TIME, System.currentTimeMillis())
                    .apply()
                withContext(Dispatchers.Main) {
                    currentExerciseIndex = nextIdx
                    currentSetIndex = 0
                    updateExerciseInfo()
                    fetchSetsForCurrentExercise()

                }
            } else {
                prefs.edit().putBoolean(KEY_IN_PROGRESS, false).apply()
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_exerciseDoing_to_coolDownStretch)
                }
            }
        }
    }

    private fun updateExerciseProgressUI() {
        planExerciseList.getOrNull(currentExerciseIndex)?.exercise?.let {
            binding.exerciseNameTextView.text = it.name
        }
        binding.stopwatchTextView.text =
            stopwatchViewModel.formatElapsedTime(stopwatchViewModel.elapsedTime.value ?: 0L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseSetAdapter(
        private val onSetClick: ((ExerciseSet) -> Unit)? = null
    ) : ListAdapter<ExerciseSet, ExerciseSetAdapter.ViewHolder>(object : DiffUtil.ItemCallback<ExerciseSet>() {
        override fun areItemsTheSame(old: ExerciseSet, new: ExerciseSet) = old.id == new.id
        override fun areContentsTheSame(old: ExerciseSet, new: ExerciseSet) = old == new
    }) {
        private var currentEquip: String? = null
        fun updateEquip(e: String?) {
            currentEquip = e
            notifyDataSetChanged()
        }
        fun updateSetNumbers(list: MutableList<ExerciseSet>) {
            list.forEachIndexed { idx, s -> s.setNumber = idx + 1 }
        }
        inner class ViewHolder(val b: ItemExerciseSetBinding) : RecyclerView.ViewHolder(b.root) {
            init {
                b.root.setOnClickListener {
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onSetClick?.invoke(getItem(pos))
                    }
                }
            }
            fun bind(s: ExerciseSet) {
                b.setNumberTextView.text = "${s.setNumber}세트"
                b.repsTextView.text     = "${s.reps}회"

                val lp = b.repsTextView.layoutParams as ViewGroup.MarginLayoutParams
                if (currentEquip in listOf("맨몸", "스텝박스", "세라밴드")) {
                    b.weightTextView.visibility  = View.GONE
                    b.dividerImageView.visibility = View.GONE
                    // repsTextView 오른쪽 마진 24dp 로 설정
                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
                        R.dimen.item_reps_margin_end_no_weight)
                } else {
                    b.weightTextView.visibility  = View.VISIBLE
                    b.dividerImageView.visibility = View.VISIBLE
                    b.weightTextView.text = s.weight?.let { "${it}kg" } ?: "-"
                    lp.marginEnd = itemView.context.resources.getDimensionPixelSize(
                        R.dimen.item_reps_margin_end_default)
                }
                b.completionCheckImageView.alpha = if (s.isCompleted) 1f else 0f
                b.root.background = ContextCompat.getDrawable(
                    b.root.context,
                    when {
                        s.isCompleted    -> R.drawable.set_item_background_completed
                        s.isHighlighted  -> R.drawable.set_item_background_emphasized
                        else              -> R.drawable.set_item_background
                    }
                )
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemExerciseSetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(getItem(position))
    }
}
