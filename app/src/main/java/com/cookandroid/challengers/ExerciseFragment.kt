package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.PlanDetailWithExercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Collections

// 운동탭 메인화면
class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseAdapter
    private lateinit var planDao: ExercisePlanDao
    private lateinit var planDetailDao: PlanDetailDao
    private lateinit var db: AppDatabase
    private var planId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        planDao = db.exercisePlanDao()
        planDetailDao = db.planDetailDao()

        parentFragmentManager.setFragmentResultListener(
            "sets_updated",
            viewLifecycleOwner
        ) { _, _ ->
            loadTodayPlan()
        }

        adapter = ExerciseAdapter(requireContext(), this, db) {
            findNavController().navigate(
                R.id.action_exercise_to_exerciseAdd,
                Bundle().apply { putLong("planId", planId) }
            )
        }
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseFragment.adapter
        }
        setupDragAndDrop()
        loadTodayPlan()

        binding.startExerciseButton.setOnClickListener {
            if (planId != -1L) {
                val firstIncompleteExerciseIndex =
                    adapter.currentList.indexOfFirst { !it.planDetail.isCompleted }

                if (firstIncompleteExerciseIndex != -1) {
                    val exerciseToStart = adapter.currentList[firstIncompleteExerciseIndex]
                    Log.d("ExerciseFragment", "Starting exercise: ${exerciseToStart.exercise.name} at index: $firstIncompleteExerciseIndex")

                    findNavController().navigate(
                        R.id.action_exercise_to_exerciseDoing,
                        Bundle().apply {
                            putLong("planId", planId)
                            putInt("initialExerciseIndex", firstIncompleteExerciseIndex)
                        }
                    )
                } else {
                    // 모든 운동이 완료되었거나 운동이 없는 경우
                    Toast.makeText(
                        requireContext(),
                        "오늘 계획된 운동을 모두 완료했거나, 시작할 운동이 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "오늘 계획된 운동이 없습니다. 운동을 추가해주세요.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exercise_to_exerciseList)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTodayPlan()
    }

    private fun loadTodayPlan() {
        lifecycleScope.launch(Dispatchers.IO) {
            val zoneId = ZoneId.of("Asia/Seoul") // 서울 시간대 명시

            val todayStart = LocalDate.now(zoneId)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1 // 오늘 끝 (자정 - 1밀리초)

            // 로그 추가: 어떤 날짜 범위를 찾는지 확인
            Log.d("ExerciseFragment", "Searching for plans between: $todayStart and $todayEnd")

            planDao.getExercisePlansByDate(todayStart, todayEnd)
                .collectLatest { plans ->
                    if (!isAdded || _binding == null) return@collectLatest

                    if (plans.isNotEmpty()) {
                        planId = plans.first().id
                        Log.d("ExerciseFragment", "Found plan for today with ID: $planId")

                        planDao.getPlanDetailsWithExerciseFlow(planId)
                            .collectLatest { exercises ->
                                if (!isAdded || _binding == null) return@collectLatest

                                val sorted = exercises.sortedBy { it.planDetail.exOrder }

                                withContext(Dispatchers.Main) {
                                    if (!isAdded || _binding == null) return@withContext

                                    adapter.submitList(sorted.toList())
                                    Log.d(
                                        "ExerciseFragment",
                                        "Loaded ${sorted.size} exercises for plan ID: $planId"
                                    )
                                    binding.startExerciseButton.visibility = View.VISIBLE
                                    binding.menuBtn.visibility = View.VISIBLE
                                }
                            }

                    } else {
                        withContext(Dispatchers.Main) {
                            if (!isAdded || _binding == null) return@withContext

                            adapter.submitList(emptyList())
                            planId = -1L
                            Log.d("ExerciseFragment", "No plan found for today.")
                        }
                    }
                }

        }
    }

    private fun setupDragAndDrop() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                // 'Add Exercise' 버튼과 'Cool Down' 섹션은 이동할 수 없도록 방지
                if (to >= adapter.currentList.size) return false // 'Add' 버튼 또는 'Cool Down' 영역으로 이동 방지

                val newList = adapter.currentList.toMutableList().apply {
                    Collections.swap(this, from, to)
                }
                adapter.submitList(newList)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val planDetails = planDetailDao.getPlanDetailsForPlanId(planId)
                        newList.forEachIndexed { index, item ->
                            val planDetail =
                                planDetails.find { it.exerciseId == item.planDetail.exerciseId }
                            planDetail?.let {
                                val updatedPlanDetail = it.copy(exOrder = index)
                                planDetailDao.update(updatedPlanDetail)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "운동 순서가 변경되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ExerciseFragment", "Error updating exOrder: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "운동 순서 변경에 실패했습니다: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                return true
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, dir: Int) = Unit
        })
        helper.attachToRecyclerView(binding.exerciseListRecyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseAdapter(
        private val context: Context,
        private val fragment: Fragment,
        private val db: AppDatabase,
        private val onAddClick: () -> Unit
    ) : ListAdapter<PlanDetailWithExercise, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<PlanDetailWithExercise>() {
            override fun areItemsTheSame(
                oldItem: PlanDetailWithExercise,
                newItem: PlanDetailWithExercise
            ): Boolean {
                return oldItem.planDetail.exercisePlanId == newItem.planDetail.exercisePlanId &&
                        oldItem.planDetail.exerciseId == newItem.planDetail.exerciseId
            }

            override fun areContentsTheSame(
                oldItem: PlanDetailWithExercise,
                newItem: PlanDetailWithExercise
            ): Boolean {
                // isCompleted 값도 비교하여 UI 업데이트가 필요할 때 DiffUtil이 감지하도록 합니다.
                return oldItem == newItem &&
                        oldItem.planDetail.isCompleted == newItem.planDetail.isCompleted
            }
        }
    ) {
        private val TYPE_EXERCISE = 0
        private val TYPE_COOLDOWN = 1
        private val TYPE_ADD = 2

        // currentList.size는 운동 아이템만 포함하므로, 쿨다운(1)과 추가 버튼(1)을 더해야 합니다.
        override fun getItemCount() = currentList.size + 2
        override fun getItemViewType(position: Int) = when {
            position < currentList.size -> TYPE_EXERCISE
            position == currentList.size -> TYPE_COOLDOWN
            else -> TYPE_ADD
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_EXERCISE -> {
                    val binding = ItemExerciseBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ExerciseVH(binding)
                }

                TYPE_COOLDOWN -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_cool_down, parent, false)
                    object : RecyclerView.ViewHolder(view) {}
                }

                else -> { // TYPE_ADD
                    val binding = ItemAddExerciseButtonBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    object : RecyclerView.ViewHolder(binding.root) {
                        init {
                            binding.addExerciseButton.setOnClickListener { onAddClick() }
                        }
                    }
                }
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ExerciseVH && position < currentList.size) {
                val item = currentList[position]
                holder.binding.apply {
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        val exerciseSets = db.exerciseSetDao().getSetsByPlanAndExerciseId(
                            item.planDetail.exercisePlanId,
                            item.exercise.id
                        )
                        withContext(Dispatchers.Main) {
                            val reps = exerciseSets.firstOrNull()?.reps ?: 0
                            val setCount = exerciseSets.size
                            holder.binding.apply {
                                exerciseNameTextView.text = item.exercise.name
                                exerciseDetailTextView.text = "${reps}회 X ${setCount}세트"
                            }
                        }
                    }

                    val resId = context.resources.getIdentifier(
                        item.exercise.imagePath ?: "",
                        "drawable",
                        context.packageName
                    )
                    if (item.exercise.imagePath != null) {
                        Glide.with(context)
                            .asBitmap()
                            .load(resId)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(exerciseImageView)
                    } else {
                        exerciseImageView.setImageResource(R.drawable.ic_launcher_background)
                    }

                    // 투명도 (visibility) 설정 부분 추가
                    if (item.planDetail.isCompleted) {
                        contentLayout.alpha = 0.5f // 변경: contentLayout에 alpha 적용
                    } else {
                        contentLayout.alpha = 1.0f // 변경: contentLayout에 alpha 적용
                    }

                    exerciseItem.setOnClickListener {
                        fragment.findNavController().navigate(
                            R.id.action_exercise_to_exerciseDoing,
                            Bundle().apply {
                                putLong("planId", item.planDetail.exercisePlanId)
                                putInt("initialExerciseIndex", position)
                            }
                        )
                    }
                    btnMore.setOnClickListener {
                        val dialog = ExerciseEditFragment(
                            item.planDetail,
                            item.exercise
                        ) {
                            (fragment as? ExerciseFragment)?.loadTodayPlan()
                        }
                        dialog.show(fragment.childFragmentManager, ExerciseEditFragment.TAG)
                    }
                }
            } else if (position == currentList.size) { // Cool Down Section
                val coolDownListView =
                    holder.itemView.findViewById<LinearLayout>(R.id.coolDownListLayoutContainer)
                val btnExpand = holder.itemView.findViewById<ImageButton>(R.id.btnExpand)

                btnExpand.setOnClickListener {
                    coolDownListView.visibility =
                        if (coolDownListView.visibility == View.GONE) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }
            // TYPE_ADD (마지막 포지션)은 onBindViewHolder에서 특별히 할 일이 없으므로 비워둡니다.
            // 이미 onCreateViewHolder에서 클릭 리스너가 설정되어 있습니다.
        }

        private class ExerciseVH(val binding: ItemExerciseBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}